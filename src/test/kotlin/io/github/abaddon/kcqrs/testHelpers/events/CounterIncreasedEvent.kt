package io.github.abaddon.kcqrs.testHelpers.events

import io.github.abaddon.kcqrs.core.domain.messages.events.EventHeader
import io.github.abaddon.kcqrs.core.domain.messages.events.IDomainEvent
import io.github.abaddon.kcqrs.testHelpers.entities.CounterAggregateId
import io.github.abaddon.kcqrs.testHelpers.entities.CounterAggregateRoot
import java.util.*

data class CounterIncreasedEvent(
    override val messageId: UUID,
    override val aggregateId: CounterAggregateId,
    val value: Int
) : IDomainEvent {
    override val aggregateType: String = CounterAggregateRoot::class.java.simpleName
    override val version: Int = 1
    override val header: EventHeader = EventHeader.create(aggregateType)

    companion object {
        fun create(aggregateId: CounterAggregateId, value: Int): CounterIncreasedEvent =
            CounterIncreasedEvent(UUID.randomUUID(), aggregateId, value)
    }


}