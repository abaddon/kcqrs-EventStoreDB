package io.github.abaddon.kcqrs.eventstoredb.config

import com.eventstore.dbclient.EventStoreDBClientSettings
import com.eventstore.dbclient.EventStoreDBConnectionString

data class EventStoreDBConfig(
    private val connectionString: String
) {

    fun eventStoreDBClientSettingsBuilder(): EventStoreDBClientSettings =
        EventStoreDBConnectionString
            .parseOrThrow(connectionString)

}