package io.github.abaddon.kcqrs.eventstores.eventstoredb

data class EventStoreDBConfig(
    val maxReadPageSize: Long,
    val maxWritePageSize: Int
)
