package io.github.abaddon.kcqrs.eventstoredb.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class EventStoreDBConfigTest {

    @Test
    fun `Given a valid connection string when EventStoreDBClientSettings is created then it contain the same information available in the string`() {
        val connectionString = "esdb://127.0.0.1:2113?tls=false&tlsVerifyCert=false"
        val eventStoreDBConfig = EventStoreDBConfig(connectionString)
        val eventStoreDBClientSettings = eventStoreDBConfig.eventStoreDBClientSettingsBuilder()
        assertEquals(false, eventStoreDBClientSettings.isTls)
        assertEquals(false, eventStoreDBClientSettings.isTlsVerifyCert)
        assertEquals(1, eventStoreDBClientSettings.hosts.size)
        assertEquals("127.0.0.1", eventStoreDBClientSettings.hosts[0].hostname)
        assertEquals(2113, eventStoreDBClientSettings.hosts[0].port)
    }
}