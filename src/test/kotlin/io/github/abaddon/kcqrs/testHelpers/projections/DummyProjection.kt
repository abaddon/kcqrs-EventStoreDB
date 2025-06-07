package io.github.abaddon.kcqrs.testHelpers.projections

import io.github.abaddon.kcqrs.core.domain.messages.events.IDomainEvent
import io.github.abaddon.kcqrs.core.projections.IProjection
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap


data class DummyProjection(
    override val key: DummyProjectionKey, val numEvents: Int,
    override val lastProcessedEvent: ConcurrentHashMap<String, Long> = ConcurrentHashMap(),
    override val lastUpdated: Instant? = Instant.now()
) : IProjection {

    override fun applyEvent(event: IDomainEvent): DummyProjection {
        return copy(numEvents = numEvents + 1)
    }

    override fun withPosition(event: IDomainEvent): IProjection {
        this.lastProcessedEvent[event.aggregateType] = event.version
        return this
    }
}
