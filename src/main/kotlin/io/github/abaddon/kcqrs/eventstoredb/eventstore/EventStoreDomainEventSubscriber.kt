package io.github.abaddon.kcqrs.eventstoredb.eventstore


import io.github.abaddon.kcqrs.core.helpers.LoggerFactory.log
import io.github.abaddon.kcqrs.core.persistence.IDomainEventSubscriber
import io.github.abaddon.kcqrs.core.projections.IProjection
import io.github.abaddon.kcqrs.core.projections.IProjectionHandler
import io.github.abaddon.kcqrs.eventstoredb.projection.EventStoreProjectionHandler
import io.kurrent.dbclient.CreatePersistentSubscriptionToAllOptions
import io.kurrent.dbclient.KurrentDBPersistentSubscriptionsClient
import io.kurrent.dbclient.SubscriptionFilter


class EventStoreDomainEventSubscriber<TProjection : IProjection>(
    private val eventStoreSubscriptionConfig: EventStoreSubscriptionConfig
) : IDomainEventSubscriber<TProjection> {
    private val persistentSubscriptionsClient = KurrentDBPersistentSubscriptionsClient
        .create(eventStoreSubscriptionConfig.eventStoreDBClientSettings())

    init {
        //Initialize the persistent subscription group
        val streamNameRegex = eventStoreSubscriptionConfig.streamNames.joinToString("|", prefix = "^", postfix = "$")
        val subscriptionFilter = SubscriptionFilter.newBuilder()
            .withStreamNameRegularExpression(streamNameRegex) //^account|^savings
            .build()
        val options = CreatePersistentSubscriptionToAllOptions.get()
            .fromStart()
            .filter(subscriptionFilter)
        persistentSubscriptionsClient.createToAll(
            eventStoreSubscriptionConfig.groupName,
            options
        ).whenComplete { subscription, exception ->
            if (exception != null) {
                log.error(
                    "Failed to create persistentSubscriptionGroup ${eventStoreSubscriptionConfig.groupName}",
                    exception
                )
                throw exception as Throwable
            }
            log.info("Persistent subscription group created {}", eventStoreSubscriptionConfig.groupName)
        }
    }

    override fun subscribe(projectionHandler: IProjectionHandler<TProjection>) {
        when (projectionHandler) {
            is EventStoreProjectionHandler -> subscribeEventStoreProjectionHandler(projectionHandler)
            else -> log.warn("EventStoreProjectionHandler required, subscription failed")
        }
    }

    private fun subscribeEventStoreProjectionHandler(projectionHandler: EventStoreProjectionHandler<TProjection>) {

        persistentSubscriptionsClient.subscribeToAll(
            eventStoreSubscriptionConfig.groupName,
            projectionHandler.getPersistentSubscriptionListener()
        ).whenComplete { subscription, exception ->
            if (exception != null) {
                log.error("Failed to subscribe to projection ${projectionHandler.javaClass.simpleName}", exception)
                throw exception
            }
            log.info("subscribed to projection {}", subscription.subscriptionId)
        }
    }

}