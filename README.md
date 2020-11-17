# KEventBus
JVM Eventbus focused on concurrency and performance.

## Registering
**Kotlin**
```kotlin
// Create eventbus
private val eventBus = eventbus {
    invoker { LMFInvoker() }
    exceptionHandler { exception -> println("Error occurred in method: ${exception.message}")  }
}

// Method you would like to subscribe to an event
// Param #1 is MessagedReceivedEvent therefore this method will be subscribed to that class
@Subscribe
fun `subscribed method`(event: MessageReceivedEvent) {
    // do something
    println(event.message)
}
...
// Register all the @Subscribe 'd methods inside of an instance
eventBus.register(this)

```
**Java**
```java
// Create eventbus
private EventBus eventBus = new EventBus(new LMFInvoker(), e -> {
    System.out.println("Error occurred in method: " + e.getMessage());
});

// Method you would like to subscribe to an event
// Param #1 is MessagedReceivedEvent therefore this method will be subscribed to that class
@Subscribe
public void subscribedMethod(MessageReceivedEvent event) {
    System.out.println(event.getMessage());
}
...
// Register all the @Subscribe 'd methods inside of an instance        
eventBus.register(this)
```
## Posting
**Kotlin**
```kotlin
// Post all methods subscribed to the event `MessageReceivedEvent`
eventBus.post(MessageReceivedEvent("Hello world"))
```
**Java**
```java 
// Post all methods subscribed to the event `MessageReceivedEvent`
eventBus.post(new MessageReceivedEvent("Hello world"));
```
## Unregistering
**Kotlin**
```kotlin
// Remove all @Subscribe 'd methods from an instance
eventBus.unregister(this)
```
**Java**
```java
// Remove all @Subscribe 'd methods from an instance
eventBus.unregister(this)
```

