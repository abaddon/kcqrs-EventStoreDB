package io.github.abaddon.kcqrs.eventstoredb.eventstore

import com.eventstore.dbclient.Position
import com.eventstore.dbclient.RecordedEvent
import com.eventstore.dbclient.StreamRevision
import io.github.abaddon.kcqrs.testHelpers.entities.CounterAggregateId
import io.github.abaddon.kcqrs.testHelpers.events.CounterInitialisedEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.*


internal class HelpersKtTest {

    @Test
    fun `Given SimpleDomainEvent When ToEventData Then EventData Created`() {

        val aggregateId = CounterAggregateId(UUID.randomUUID())
        val value = 5
        val expectedDomainEvent = CounterInitialisedEvent.create(aggregateId,value)

        val headers = mapOf<String, String>(
            Pair("header1", "value1")
        )

        val eventData = expectedDomainEvent.toEventData(headers)

        val systemMap = mapOf<String, String>(
            Pair("type", CounterInitialisedEvent::class.qualifiedName!!),
            Pair("content-type", "json"),
            Pair("created", Instant.now().toEpochMilli().toString()),
        )
        val eventStoreRecordedEvent = RecordedEvent(
            "1223",
            StreamRevision(1L),
            eventData.eventId,
            Position(3L, 2L),
            systemMap,
            eventData.eventData,
            eventData.userMetadata
        )

        //deserialize
        val actualDummyDomainEvent = eventStoreRecordedEvent.toDomainEvent()
        assertEquals(expectedDomainEvent.value, (actualDummyDomainEvent as CounterInitialisedEvent).value)
        assertEquals(expectedDomainEvent.aggregateId, (actualDummyDomainEvent).aggregateId)

    }
}