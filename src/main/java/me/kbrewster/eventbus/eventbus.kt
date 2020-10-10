package me.kbrewster.eventbus

import me.kbrewster.eventbus.invokers.InvokerType
import me.kbrewster.eventbus.invokers.ReflectionInvoker
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.jvm.internal.Intrinsics

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class Subscribe(val priority: Int = 0)

fun eventbus(lambda: EventBusBuilder.() -> Unit) : EventBus {
    return EventBusBuilder().apply(lambda).build()
}

class EventBusBuilder {
    /**
     * Default: reflection invoker
     */
    private var invokerType: InvokerType = ReflectionInvoker()

    /**
     * Default: throws exception again
     */
    private var exceptionHandler: (Exception) -> Unit = { exception -> throw exception }

    fun invoker(lambda: () -> InvokerType) { this.invokerType = lambda() }
    fun exceptionHandler(lambda: (Exception) -> Unit) { this.exceptionHandler = lambda }
    fun build() = EventBus(this.invokerType, this.exceptionHandler)

}

class EventBus(private val invokerType: InvokerType, private val exceptionHandler: (Exception) -> Unit) {
    private class Subscriber(val `object`: Any, val method: Method?, val priority: Int, invoker: InvokerType.SubscriberMethod?) {
        private val invoker: InvokerType.SubscriberMethod? = invoker

        @Throws(Exception::class)
        operator fun invoke(arg: Any?) {
            invoker!!.invoke(arg)
        }

        override fun equals(other: Any?): Boolean {
            return other.hashCode() == this.hashCode()
        }

        override fun hashCode(): Int {
            return `object`.hashCode()
        }

    }

    private class PriorityCopyAndWriteArrayList : CopyOnWriteArrayList<Subscriber>() {
        override fun add(element: Subscriber): Boolean {
            if (size == 0) {
                super.add(element)
            } else {
                var index = this.binarySearch(element, Comparator.comparingInt { obj: Subscriber -> obj.priority })
                if (index < 0) index = -(index + 1)
                super.add(index, element)
            }
            return true
        }
    }

    private val subscribers: ConcurrentHashMap<Class<*>, MutableList<Subscriber>> = ConcurrentHashMap()

    fun register(`object`: Any) {
        var clazz: Class<*>? = `object`.javaClass
        while (clazz != null) {
            for (method in clazz.declaredMethods) {
                val sub: Subscribe = method.getAnnotation(Subscribe::class.java) ?: continue

                // Verification
                Intrinsics.areEqual(method.returnType, Void.TYPE)
                Intrinsics.areEqual(method.parameterCount, 1)
                Intrinsics.areEqual(method.modifiers and Modifier.STATIC, Modifier.STATIC)

                val parameterClazz = method.parameterTypes[0]
                when {
                    Modifier.isStatic(method.modifiers) -> Intrinsics.throwIllegalArgument("Cannot have static modifier on a @Subscribe method.")
                    parameterClazz.isPrimitive -> Intrinsics.throwIllegalArgument("Cannot subscribe method to a primitive.")
                    (parameterClazz.modifiers and (Modifier.ABSTRACT or Modifier.INTERFACE)) != 0 -> Intrinsics.throwIllegalArgument("Cannot subscribe method to a polymorphic class.")
                }

                var psc = parameterClazz.superclass
                while (psc != null) {
                    if(subscribers.containsKey(psc)) {
                        Intrinsics.throwIllegalArgument("@Subscribe method \"$method\" cannot subscribe to class which inherits from subscribed class \"$psc\".")
                    }
                    psc = psc.superclass
                }
                var subscriberMethod: InvokerType.SubscriberMethod? = null
                try {
                    subscriberMethod = invokerType.setup(`object`, clazz, parameterClazz, method)
                } catch (throwable: Throwable) {
                    Intrinsics.throwAssert("Failed to setup invoker, ${throwable.message}")
                }
                val subscriber = Subscriber(`object`, method, sub.priority, subscriberMethod)
                subscribers.putIfAbsent(parameterClazz, PriorityCopyAndWriteArrayList())
                subscribers[parameterClazz]!!.add(subscriber)
            }
            clazz = clazz.superclass
        }
    }

    fun unregister(`object`: Any) {
        var clazz: Class<*>? = `object`.javaClass
        while (clazz != null) {
            for (method in clazz.declaredMethods) {
                if (method.getAnnotation(Subscribe::class.java) == null) {
                    continue
                }
                val parameterClazz = method.parameterTypes[0]
                subscribers[parameterClazz]?.remove(Subscriber(`object`, null, -1, null))
            }
            clazz = clazz.superclass
        }
    }

    fun post(event: Any) {
        subscribers[event.javaClass]?.let { list ->
            // executed in descending order
            for (i in list.size-1 downTo 0) {
                val subscriber = list[i]
                try {
                    subscriber.invoke(event)
                } catch (e: Exception) {
                    exceptionHandler(e)
                }
            }
        }
    }

}
