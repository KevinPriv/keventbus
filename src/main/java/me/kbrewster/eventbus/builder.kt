package me.kbrewster.eventbus

import me.kbrewster.eventbus.exception.ExceptionHandler
import me.kbrewster.eventbus.invokers.InvokerType
import me.kbrewster.eventbus.invokers.ReflectionInvoker

fun eventbus(lambda: EventBusBuilder.() -> Unit): EventBus {
    return EventBusBuilder().apply(lambda).build()
}

class EventBusBuilder {
    /**
     * Default: reflection invoker
     */
    var invokerType: InvokerType = ReflectionInvoker()

    /**
     * Default: throws exception again
     */
    var exceptionHandler: ExceptionHandler = object: ExceptionHandler {
        override fun handle(exception: Exception) {
            throw exception
        }
    }

    fun invoker(lambda: () -> InvokerType) {
        this.invokerType = lambda()
    }

    inline fun exceptionHandler(crossinline lambda: (Exception) -> Unit) {
        this.exceptionHandler = object: ExceptionHandler {
            override fun handle(exception: Exception) {
                lambda(exception)
            }
        }
    }

    fun build() = EventBus(this.invokerType, this.exceptionHandler)

}