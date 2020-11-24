import me.kbrewster.eventbus.Subscribe
import me.kbrewster.eventbus.eventbus
import me.kbrewster.eventbus.exception.ExceptionHandler
import me.kbrewster.eventbus.invokers.LMFInvoker
import org.junit.jupiter.api.*
import sun.plugin2.message.Message


import kotlin.random.Random

class MessageReceivedEvent(val message: String)

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class EventBusTest {

    private val eventBus = eventbus {
        invoker { LMFInvoker() }
        exceptionHandler { exception -> println("Error occurred in method: ${exception.message}")  }
        threadSaftey { true }
    }

    @Test
    @Order(0)
    fun `subscribing class`() {
       // for(i in 0..1000) {
            eventBus.register(this)
     //   }
    }

    @Subscribe
    fun `subscribed method`(event: MessageReceivedEvent) {
        // do something
    }
// cw 20%
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