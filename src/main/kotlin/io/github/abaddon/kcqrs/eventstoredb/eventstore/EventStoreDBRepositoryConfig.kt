package io.github.abaddon.kcqrs.eventstoredb.eventstore

import com.eventstore.dbclient.EventStoreDBClientSettings
import io.github.abaddon.kcqrs.eventstoredb.config.EventStoreDBConfig

data class EventStoreDBRepositoryConfig(
    private val eventStoreDB: EventStoreDBConfig,
    val streamName: String,
    val maxReadPageSize: Long,
    val maxWritePageSize: Int

) {
    fun eventStoreDBClientSettings(): EventStoreDBClientSettings = eventStoreDB.eventStoreDBClientSettingsBuilder()
}