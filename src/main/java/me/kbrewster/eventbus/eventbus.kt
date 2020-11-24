package me.kbrewster.eventbus

import me.kbrewster.eventbus.collection.ConcurrentSubscriberArrayList
import me.kbrewster.eventbus.collection.SubscriberArrayList
import me.kbrewster.eventbus.exception.ExceptionHandler
import me.kbrewster.eventbus.invokers.InvokerType
import me.kbrewster.eventbus.invokers.ReflectionInvoker
import java.lang.reflect.Modifier
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class Subscribe(val priority: Int = 0)

class EventBus @JvmOverloads constructor(
        private val invokerType: InvokerType = ReflectionInvoker(),
        private val exceptionHandler: ExceptionHandler = object: ExceptionHandler {
                   override fun handle(exception: Exception) {
                       throw exception
                   }
               },
        private val threadSaftey: Boolean = true) {

    class Subscriber(val obj: Any, val priority: Int, private val invoker: InvokerType.SubscriberMethod?) {

        @Throws(Exception::class)
        operator fun invoke(arg: Any?) {
            invoker!!.invoke(arg)
        }

        override fun equals(other: Any?): Boolean {
            return other.hashCode() == this.hashCode()
        }

        override fun hashCode(): Int {
            return obj.hashCode()
        }

    }

    private val subscribers: AbstractMap<Class<*>, MutableList<Subscriber>> =
            if(threadSaftey) ConcurrentHashMap() else HashMap()

    /**
     * Subscribes all of the methods marked with the `@Subscribe` annotation
     * within the `obj` instance provided to th methods first parameter class
     *
     * e.g. registering an instance which includes the method below will invoke
     * that method every time EventBus#post(MessageReceivedEvent()) is called.
     * @Subscribe
     * fun messageReceivedEvent(event: MessageReceivedEvent) {
     * }
     *
     */
    fun register(obj: Any) {
        for (method in obj.javaClass.declaredMethods) {
            val sub: Subscribe = method.getAnnotation(Subscribe::class.java) ?: continue

            // verification
            val parameterClazz = method.parameterTypes[0]
            when {
                method.parameterCount != 1 -> throw IllegalArgumentException("Subscribed method must only have one parameter.")
                method.returnType != Void.TYPE -> throw IllegalArgumentException("Subscribed method must be of type 'Void'. ")
                parameterClazz.isPrimitive -> throw IllegalArgumentException("Cannot subscribe method to a primitive.")
                parameterClazz.modifiers and (Modifier.ABSTRACT or Modifier.INTERFACE) != 0 -> throw IllegalArgumentException("Cannot subscribe method to a polymorphic class.")
            }

            val subscriberMethod = invokerType.setup(obj, obj.javaClass, parameterClazz, method)

            val subscriber = Subscriber(obj, sub.priority, subscriberMethod)
            subscribers.putIfAbsent(parameterClazz, if(threadSaftey) ConcurrentSubscriberArrayList() else SubscriberArrayList())
            subscribers[parameterClazz]!!.add(subscriber)
        }
    }

    /**
     * Unsubscribes all `@Subscribe`'d methods inside of the `obj` instance.
     */
    fun unregister(obj: Any) {
        for (method in obj.javaClass.declaredMethods) {
            if (method.getAnnotation(Subscribe::class.java) == null) {
                continue
            }
            subscribers[method.parameterTypes[0]]?.remove(Subscriber(obj, -1, null))
        }
    }

    /**
     * Posts the event instance given to all the subscribers
     * that are subscribed to the events class.
     */
    fun post(event: Any) {
        val events = subscribers[event.javaClass] ?: return
        // executed in descending order
        for (i in (events.size-1) downTo 0) {
            try {
                events[i].invoke(event)
            } catch (e: Exception) {
                exceptionHandler.handle(e)
            }
        }
    }

    /**
     * Supplier is only used if there are subscribers listening to
     * the event.
     *
     * Example usage: EventBus#post { ComputationallyHeavyEvent() }
     *
     * This allows events to only be constructed if needed.
     */
    inline fun <reified T> post(supplier: () -> T) {
        val events = getSubscribedEvents(T::class.java) ?: return
        val event = supplier()
        // executed in descending order
        for (i in (events.size-1) downTo 0) {
            events[i].invoke(event)
        }
    }

    fun getSubscribedEvents(clazz: Class<*>) = subscribers[clazz]

    private inline fun iterateSubclasses(obj: Any, body: (Class<*>) -> Unit) {
        var postClazz: Class<*>? = obj.javaClass
        do {
            body(postClazz!!)
            postClazz = postClazz.superclass
        } while (postClazz != null)
    }
}
