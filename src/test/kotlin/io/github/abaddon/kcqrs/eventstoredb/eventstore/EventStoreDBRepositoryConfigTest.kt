package io.github.abaddon.kcqrs.eventstoredb.eventstore

import io.github.abaddon.kcqrs.eventstoredb.config.EventStoreDBConfig
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class EventStoreDBRepositoryConfigTest {

    @Test
    fun `Given valid config when created then all properties are set correctly`() {
        // Given
        val connectionString = "kurrentdb://127.0.0.1:2113?tls=false&tlsVerifyCert=false"
        val eventStoreDBConfig = EventStoreDBConfig(connectionString)
        val streamName = "TestStream"
        val maxReadPageSize = 1000L
        val maxWritePageSize = 500

        // When
        val config = EventStoreDBRepositoryConfig(
            eventStoreDB = eventStoreDBConfig,
            streamName = streamName,
            maxReadPageSize = maxReadPageSize,
            maxWritePageSize = maxWritePageSize
        )

        // Then
        assertEquals(streamName, config.streamName)
        assertEquals(maxReadPageSize, config.maxReadPageSize)
        assertEquals(maxWritePageSize, config.maxWritePageSize)
    }

    @Test
    fun `Given config when eventStoreDBClientSettings called then returns valid KurrentDBClientSettings`() {
        // Given
        val connectionString = "kurrentdb://localhost:2113?tls=true&tlsVerifyCert=true"
        val eventStoreDBConfig = EventStoreDBConfig(connectionString)
        val config = EventStoreDBRepositoryConfig(
            eventStoreDB = eventStoreDBConfig,
            streamName = "TestStream",
            maxReadPageSize = 500L,
            maxWritePageSize = 100
        )

        // When
        val clientSettings = config.eventStoreDBClientSettings()

        // Then
        assertNotNull(clientSettings)
        assertTrue(clientSettings.isTls)
        assertTrue(clientSettings.isTlsVerifyCert)
        assertEquals(1, clientSettings.hosts.size)
        assertEquals("localhost", clientSettings.hosts[0].host)
        assertEquals(2113, clientSettings.hosts[0].port)
    }

    @Test
    fun `Given different stream names when created then each config has unique stream name`() {
        // Given
        val connectionString = "kurrentdb://127.0.0.1:2113?tls=false"
        val eventStoreDBConfig = EventStoreDBConfig(connectionString)

        // When
        val config1 = EventStoreDBRepositoryConfig(eventStoreDBConfig, "Stream1", 100L, 50)
        val config2 = EventStoreDBRepositoryConfig(eventStoreDBConfig, "Stream2", 100L, 50)

        // Then
        assertNotEquals(config1.streamName, config2.streamName)
        assertEquals("Stream1", config1.streamName)
        assertEquals("Stream2", config2.streamName)
    }

    @Test
    fun `Given config with different page sizes when created then page sizes are set correctly`() {
        // Given
        val connectionString = "kurrentdb://127.0.0.1:2113?tls=false"
        val eventStoreDBConfig = EventStoreDBConfig(connectionString)

        // When
        val config1 = EventStoreDBRepositoryConfig(eventStoreDBConfig, "Stream", 100L, 50)
        val config2 = EventStoreDBRepositoryConfig(eventStoreDBConfig, "Stream", 2000L, 1000)

        // Then
        assertEquals(100L, config1.maxReadPageSize)
        assertEquals(50, config1.maxWritePageSize)
        assertEquals(2000L, config2.maxReadPageSize)
        assertEquals(1000, config2.maxWritePageSize)
    }

    @Test
    fun `Given config when copy is created then both configs are independent`() {
        // Given
        val connectionString = "kurrentdb://127.0.0.1:2113?tls=false"
        val eventStoreDBConfig = EventStoreDBConfig(connectionString)
        val originalConfig = EventStoreDBRepositoryConfig(eventStoreDBConfig, "OriginalStream", 500L, 250)

        // When
        val copiedConfig = originalConfig.copy(streamName = "CopiedStream")

        // Then
        assertNotEquals(originalConfig.streamName, copiedConfig.streamName)
        assertEquals("OriginalStream", originalConfig.streamName)
        assertEquals("CopiedStream", copiedConfig.streamName)
        assertEquals(originalConfig.maxReadPageSize, copiedConfig.maxReadPageSize)
        assertEquals(originalConfig.maxWritePageSize, copiedConfig.maxWritePageSize)
    }
}
