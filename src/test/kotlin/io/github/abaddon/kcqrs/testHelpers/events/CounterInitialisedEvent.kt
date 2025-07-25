package io.github.abaddon.kcqrs.testHelpers.events

import io.github.abaddon.kcqrs.core.domain.messages.events.EventHeader
import io.github.abaddon.kcqrs.core.domain.messages.events.IDomainEvent
import io.github.abaddon.kcqrs.testHelpers.entities.CounterAggregateId
import io.github.abaddon.kcqrs.testHelpers.entities.CounterAggregateRoot
import java.util.UUID

data class CounterInitialisedEvent(
    override val messageId: UUID,
    override val aggregateId: CounterAggregateId,
    val value: Int,
    override val version: Long
) : IDomainEvent {
    override val aggregateType: String = CounterAggregateRoot::class.java.simpleName
    override val header: EventHeader = EventHeader.create(aggregateType)

    companion object {
        fun create(aggregateId: CounterAggregateId, value: Int, version: Long): CounterInitialisedEvent =
            CounterInitialisedEvent(UUID.randomUUID(), aggregateId, value, version)
    }
}