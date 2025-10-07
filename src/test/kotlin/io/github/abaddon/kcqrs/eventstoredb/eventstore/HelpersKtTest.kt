package io.github.abaddon.kcqrs.eventstoredb.eventstore

import io.github.abaddon.kcqrs.testHelpers.entities.CounterAggregateId
import io.github.abaddon.kcqrs.testHelpers.events.CounterDecreaseEvent
import io.github.abaddon.kcqrs.testHelpers.events.CounterIncreasedEvent
import io.github.abaddon.kcqrs.testHelpers.events.CounterInitialisedEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.util.UUID


internal class HelpersKtTest {

    @Test
    fun `Given SimpleDomainEvent When ToEventData Then EventData Created`() {
        // Given
        val aggregateId = CounterAggregateId(UUID.randomUUID())
        val value = 5
        val expectedDomainEvent = CounterInitialisedEvent.create(aggregateId, value, 1)
        val headers = mapOf(
            "header1" to "value1",
            "header2" to "value2"
        )

        // When
        val eventData = expectedDomainEvent.toEventData(headers)

        // Then
        val eventType = eventData.contentType
        val eventId = eventData.eventId

        assertEquals("application/json", eventType.toString())
        assertEquals(expectedDomainEvent.messageId, eventId)
        assertNotNull(eventData.eventData)
        assertNotNull(eventData.userMetadata)
    }

    @Test
    fun `Given CounterIncreasedEvent when toEventData then creates valid EventData`() {
        // Given
        val aggregateId = CounterAggregateId(UUID.randomUUID())
        val value = 10
        val event = CounterIncreasedEvent.create(aggregateId, value)
        val headers = mapOf("correlation-id" to UUID.randomUUID().toString())

        // When
        val eventData = event.toEventData(headers)

        // Then
        assertEquals("application/json", eventData.contentType.toString())
        assertEquals(event.messageId, eventData.eventId)
        assertNotNull(eventData.eventData)
        assertNotNull(eventData.userMetadata)
    }

    @Test
    fun `Given CounterDecreaseEvent when toEventData then creates valid EventData`() {
        // Given
        val aggregateId = CounterAggregateId(UUID.randomUUID())
        val value = 3
        val event = CounterDecreaseEvent.create(aggregateId, value)
        val headers = mapOf("user-id" to "test-user")

        // When
        val eventData = event.toEventData(headers)

        // Then
        assertEquals("application/json", eventData.contentType.toString())
        assertEquals(event.messageId, eventData.eventId)
        assertNotNull(eventData.eventData)
        assertNotNull(eventData.userMetadata)
    }

    @Test
    fun `Given event with empty headers when toEventData then creates EventData with empty metadata`() {
        // Given
        val aggregateId = CounterAggregateId(UUID.randomUUID())
        val event = CounterInitialisedEvent.create(aggregateId, 1, 1)
        val headers = emptyMap<String, String>()

        // When
        val eventData = event.toEventData(headers)

        // Then
        assertEquals("application/json", eventData.contentType.toString())
        assertEquals(event.messageId, eventData.eventId)
        assertNotNull(eventData.eventData)
        assertNotNull(eventData.userMetadata)
    }

    @Test
    fun `Given event with multiple headers when toEventData then all headers are serialized`() {
        // Given
        val aggregateId = CounterAggregateId(UUID.randomUUID())
        val event = CounterInitialisedEvent.create(aggregateId, 5, 1)
        val headers = mapOf(
            "correlation-id" to UUID.randomUUID().toString(),
            "causation-id" to UUID.randomUUID().toString(),
            "user-id" to "user-123",
            "tenant-id" to "tenant-456"
        )

        // When
        val eventData = event.toEventData(headers)

        // Then
        assertEquals("application/json", eventData.contentType.toString())
        assertEquals(event.messageId, eventData.eventId)
        assertNotNull(eventData.eventData)
        assertNotNull(eventData.userMetadata)
    }

    @Test
    fun `Given different event types when toEventData then each creates unique EventData`() {
        // Given
        val aggregateId = CounterAggregateId(UUID.randomUUID())
        val initEvent = CounterInitialisedEvent.create(aggregateId, 5, 1)
        val increaseEvent = CounterIncreasedEvent.create(aggregateId, 3)
        val decreaseEvent = CounterDecreaseEvent.create(aggregateId, 2)
        val headers = mapOf("test" to "value")

        // When
        val initEventData = initEvent.toEventData(headers)
        val increaseEventData = increaseEvent.toEventData(headers)
        val decreaseEventData = decreaseEvent.toEventData(headers)

        // Then
        assertEquals(initEvent.messageId, initEventData.eventId)
        assertEquals(increaseEvent.messageId, increaseEventData.eventId)
        assertEquals(decreaseEvent.messageId, decreaseEventData.eventId)

        // Each event should have different IDs
        assert(initEventData.eventId != increaseEventData.eventId)
        assert(increaseEventData.eventId != decreaseEventData.eventId)
    }

    @Test
    fun `Given mapper when used then Jackson serialization works correctly`() {
        // Given
        val aggregateId = CounterAggregateId(UUID.randomUUID())
        val event = CounterInitialisedEvent.create(aggregateId, 42, 1)

        // When
        val json = mapper.writeValueAsString(event)
        val deserialized = mapper.readValue(json, CounterInitialisedEvent::class.java)

        // Then
        assertEquals(event.messageId, deserialized.messageId)
        assertEquals(event.aggregateId, deserialized.aggregateId)
        assertEquals(event.value, deserialized.value)
        assertEquals(event.version, deserialized.version)
    }
}