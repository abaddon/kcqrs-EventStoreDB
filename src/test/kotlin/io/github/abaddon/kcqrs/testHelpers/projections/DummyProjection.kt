package io.github.abaddon.kcqrs.testHelpers.projections

import io.github.abaddon.kcqrs.core.domain.messages.events.IDomainEvent
import io.github.abaddon.kcqrs.core.projections.IProjection


data class DummyProjection(override val key: DummyProjectionKey, val numEvents: Int) : IProjection {

    override fun applyEvent(event: IDomainEvent): DummyProjection {
        return copy(numEvents = +1)
    }
}
