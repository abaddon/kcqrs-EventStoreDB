package io.github.abaddon.kcqrs.eventstoredb.projection

import io.kurrent.dbclient.Position
import io.kurrent.dbclient.ResolvedEvent
import io.kurrent.dbclient.Subscription
import io.kurrent.dbclient.SubscriptionListener
import io.github.abaddon.kcqrs.core.domain.messages.events.IDomainEvent
import io.github.abaddon.kcqrs.core.persistence.IProjectionRepository
import io.github.abaddon.kcqrs.core.projections.IProjection
import io.github.abaddon.kcqrs.core.projections.IProjectionHandler
import io.github.abaddon.kcqrs.core.projections.IProjectionKey
import io.github.abaddon.kcqrs.eventstoredb.config.SubscriptionFilterConfig
import io.github.abaddon.kcqrs.eventstoredb.eventstore.toDomainEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class EventStoreProjectionHandler<TProjection : IProjection>(
    override val repository: IProjectionRepository<TProjection>,
    override val projectionKey: IProjectionKey,
    val subscriptionFilter: SubscriptionFilterConfig?,
    val position: Position,
) : IProjectionHandler<TProjection>, SubscriptionListener() {

    override val log: Logger = LoggerFactory.getLogger(this.javaClass.simpleName)

    override fun onEvent(subscription: Subscription, event: ResolvedEvent) {
        try {
            log.info("New event received: [eventType: ${event.event?.eventType}, stream: ${event.event?.streamId}].")
            when (val domainEvent = transformToDomainEvent(event)) {
                null -> log.warn("Event received not converted in a DomainEvent")
                else -> onEvent(domainEvent)
            }
        } catch (ex: UnsupportedOperationException) {
            log.error("event ${event.event?.eventType} not applied.", ex)
        }
    }

    // In EventStoreDB 4.x, non-nullability is enforced in the method signature
    private fun transformToDomainEvent(event: ResolvedEvent): IDomainEvent? = event.event?.toDomainEvent()
}