package io.github.abaddon.kcqrs.eventstores.eventstoredb.helpers.events

import io.github.abaddon.kcqrs.core.domain.messages.events.EventHeader
import io.github.abaddon.kcqrs.core.domain.messages.events.IDomainEvent
import io.github.abaddon.kcqrs.eventstores.eventstoredb.helpers.entities.CounterAggregateId
import io.github.abaddon.kcqrs.eventstores.eventstoredb.helpers.entities.CounterAggregateRoot
import java.util.*

data class DomainErrorEvent(
    override val messageId: UUID,
    override val aggregateId: CounterAggregateId,
    val errorType: String,
    val errorMsg: String
) : IDomainEvent{
    override val aggregateType: String = CounterAggregateRoot.javaClass.simpleName
    override val version: Int = 1
    override val header: EventHeader = EventHeader.create(aggregateType)

    companion object {
        fun create(aggregateId: CounterAggregateId, error: Exception): DomainErrorEvent =
            DomainErrorEvent(UUID.randomUUID(),aggregateId,error::class.qualifiedName!!,error.message.orEmpty())
    }
}