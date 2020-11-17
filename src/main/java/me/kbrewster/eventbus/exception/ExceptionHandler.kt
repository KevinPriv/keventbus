package me.kbrewster.eventbus.exception

// TODO: replace with actual kotlin sam interface once migrated to kotlin 1.4
@FunctionalInterface
interface ExceptionHandler {
    fun handle(exception: Exception)
}
