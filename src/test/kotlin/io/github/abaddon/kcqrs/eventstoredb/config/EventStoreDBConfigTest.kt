package io.github.abaddon.kcqrs.eventstoredb.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class EventStoreDBConfigTest {

    @Test
    fun `Given a valid connection string when EventStoreDBClientSettings is created then it contain the same information available in the string`() {
        val connectionString = "kurrentdb://127.0.0.1:2113?tls=false&tlsVerifyCert=false"
        val eventStoreDBConfig = EventStoreDBConfig(connectionString)
        val kurrentDBClientSettings = eventStoreDBConfig.kurrentDBClientSettingsBuilder()
        assertEquals(false, kurrentDBClientSettings.isTls)
        assertEquals(false, kurrentDBClientSettings.isTlsVerifyCert)
        assertEquals(1, kurrentDBClientSettings.hosts.size)
        assertEquals("127.0.0.1", kurrentDBClientSettings.hosts[0].host)
        assertEquals(2113, kurrentDBClientSettings.hosts[0].port)
    }
}