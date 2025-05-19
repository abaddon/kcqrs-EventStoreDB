package io.github.abaddon.kcqrs.eventstoredb.config

import io.kurrent.dbclient.KurrentDBClientSettings
import io.kurrent.dbclient.KurrentDBConnectionString

data class EventStoreDBConfig(
    private val connectionString: String
) {

    fun kurrentDBClientSettingsBuilder(): KurrentDBClientSettings =
        KurrentDBConnectionString
            .parseOrThrow(connectionString)

}