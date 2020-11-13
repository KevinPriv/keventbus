import dev.deamsy.eventbus.impl.asm.ASMEventBus
import org.junit.jupiter.api.*
import kotlin.random.Random

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class DesamsyEventBusEventBusTest {
    private val eventBus = ASMEventBus()

    @Test
    @Order(0)
    fun `subscribing class`() {
        eventBus.registerLambda(MessageReceivedEvent::class.java) { _ -> }
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