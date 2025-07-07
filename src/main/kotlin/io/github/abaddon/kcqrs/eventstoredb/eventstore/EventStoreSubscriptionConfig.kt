package io.github.abaddon.kcqrs.eventstoredb.eventstore

import io.github.abaddon.kcqrs.eventstoredb.config.EventStoreDBConfig
import io.kurrent.dbclient.KurrentDBClientSettings

data class EventStoreSubscriptionConfig(
    private val eventStoreDB: EventStoreDBConfig,
    val streamNames: List<String>,
    val groupName: String

) {
    fun eventStoreDBClientSettings(): KurrentDBClientSettings = eventStoreDB.kurrentDBClientSettingsBuilder()
}