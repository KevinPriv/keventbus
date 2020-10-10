import me.kbrewster.eventbus.Subscribe
import me.kbrewster.eventbus.eventbus
import me.kbrewster.eventbus.invokers.LMFInvoker
import org.junit.jupiter.api.*


import kotlin.random.Random

open class MessageReceivedEvent(val message: String)

class OtherEvent(message2: String): MessageReceivedEvent(message2)

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class EventBusTest {
    private val eventBus = eventbus {
        invoker { LMFInvoker() }
        exceptionHandler { exception -> println("Error occurred in method: ${exception.message}")  }
    }

    @Test
    @Order(0)
    fun `subscribing class`() {
        eventBus.register(this)
    }

    @Subscribe
    fun `subscribed method`(event: MessageReceivedEvent) {
        // do something
        Random.nextInt()
    }

    @Test
    @Order(1)
    fun `posting event`() {
        repeat(10_000_000) {
            eventBus.post(MessageReceivedEvent("Hello world"))
        }
    }

    @Test
    @Order(2)
    fun `removing class`() {
        eventBus.unregister(this)
    }

}