package io.github.abaddon.kcqrs.eventstoredb.eventstore

import io.github.abaddon.kcqrs.eventstoredb.config.EventStoreDBConfig
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class EventStoreSubscriptionConfigTest {

    @Test
    fun `Given valid config when created then all properties are set correctly`() {
        // Given
        val connectionString = "kurrentdb://127.0.0.1:2113?tls=false&tlsVerifyCert=false"
        val eventStoreDBConfig = EventStoreDBConfig(connectionString)
        val streamNames = listOf("Stream1", "Stream2", "Stream3")
        val groupName = "test-consumer-group"

        // When
        val config = EventStoreSubscriptionConfig(
            eventStoreDB = eventStoreDBConfig,
            streamNames = streamNames,
            groupName = groupName
        )

        // Then
        assertEquals(streamNames, config.streamNames)
        assertEquals(groupName, config.groupName)
        assertEquals(3, config.streamNames.size)
    }

    @Test
    fun `Given config when eventStoreDBClientSettings called then returns valid KurrentDBClientSettings`() {
        // Given
        val connectionString = "kurrentdb://localhost:2113?tls=true&tlsVerifyCert=true"
        val eventStoreDBConfig = EventStoreDBConfig(connectionString)
        val config = EventStoreSubscriptionConfig(
            eventStoreDB = eventStoreDBConfig,
            streamNames = listOf("TestStream"),
            groupName = "test-group"
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
    fun `Given single stream name when created then streamNames list contains one element`() {
        // Given
        val connectionString = "kurrentdb://127.0.0.1:2113?tls=false"
        val eventStoreDBConfig = EventStoreDBConfig(connectionString)
        val streamNames = listOf("SingleStream")

        // When
        val config = EventStoreSubscriptionConfig(eventStoreDBConfig, streamNames, "single-group")

        // Then
        assertEquals(1, config.streamNames.size)
        assertEquals("SingleStream", config.streamNames[0])
    }

    @Test
    fun `Given multiple stream names when created then all streams are preserved`() {
        // Given
        val connectionString = "kurrentdb://127.0.0.1:2113?tls=false"
        val eventStoreDBConfig = EventStoreDBConfig(connectionString)
        val streamNames = listOf("account", "transaction", "customer", "order")

        // When
        val config = EventStoreSubscriptionConfig(eventStoreDBConfig, streamNames, "multi-group")

        // Then
        assertEquals(4, config.streamNames.size)
        assertTrue(config.streamNames.contains("account"))
        assertTrue(config.streamNames.contains("transaction"))
        assertTrue(config.streamNames.contains("customer"))
        assertTrue(config.streamNames.contains("order"))
    }

    @Test
    fun `Given different group names when created then each config has unique group name`() {
        // Given
        val connectionString = "kurrentdb://127.0.0.1:2113?tls=false"
        val eventStoreDBConfig = EventStoreDBConfig(connectionString)
        val streamNames = listOf("TestStream")

        // When
        val config1 = EventStoreSubscriptionConfig(eventStoreDBConfig, streamNames, "group-1")
        val config2 = EventStoreSubscriptionConfig(eventStoreDBConfig, streamNames, "group-2")

        // Then
        assertNotEquals(config1.groupName, config2.groupName)
        assertEquals("group-1", config1.groupName)
        assertEquals("group-2", config2.groupName)
    }

    @Test
    fun `Given empty stream names list when created then streamNames is empty`() {
        // Given
        val connectionString = "kurrentdb://127.0.0.1:2113?tls=false"
        val eventStoreDBConfig = EventStoreDBConfig(connectionString)
        val streamNames = emptyList<String>()

        // When
        val config = EventStoreSubscriptionConfig(eventStoreDBConfig, streamNames, "empty-group")

        // Then
        assertTrue(config.streamNames.isEmpty())
    }

    @Test
    fun `Given config when copy is created then both configs are independent`() {
        // Given
        val connectionString = "kurrentdb://127.0.0.1:2113?tls=false"
        val eventStoreDBConfig = EventStoreDBConfig(connectionString)
        val originalConfig = EventStoreSubscriptionConfig(
            eventStoreDBConfig,
            listOf("OriginalStream"),
            "original-group"
        )

        // When
        val copiedConfig = originalConfig.copy(groupName = "copied-group")

        // Then
        assertNotEquals(originalConfig.groupName, copiedConfig.groupName)
        assertEquals("original-group", originalConfig.groupName)
        assertEquals("copied-group", copiedConfig.groupName)
        assertEquals(originalConfig.streamNames, copiedConfig.streamNames)
    }

    @Test
    fun `Given stream names with special characters when created then names are preserved`() {
        // Given
        val connectionString = "kurrentdb://127.0.0.1:2113?tls=false"
        val eventStoreDBConfig = EventStoreDBConfig(connectionString)
        val streamNames = listOf("account-stream", "order_stream", "customer.stream")

        // When
        val config = EventStoreSubscriptionConfig(eventStoreDBConfig, streamNames, "special-group")

        // Then
        assertEquals(3, config.streamNames.size)
        assertTrue(config.streamNames.contains("account-stream"))
        assertTrue(config.streamNames.contains("order_stream"))
        assertTrue(config.streamNames.contains("customer.stream"))
    }
}
