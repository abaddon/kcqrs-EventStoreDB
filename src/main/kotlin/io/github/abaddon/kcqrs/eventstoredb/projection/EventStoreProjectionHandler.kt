package io.github.abaddon.kcqrs.eventstoredb.projection

import io.github.abaddon.kcqrs.core.domain.messages.events.IDomainEvent
import io.github.abaddon.kcqrs.core.helpers.LoggerFactory.log
import io.github.abaddon.kcqrs.core.persistence.IProjectionRepository
import io.github.abaddon.kcqrs.core.projections.IProjection
import io.github.abaddon.kcqrs.core.projections.ProjectionHandler
import io.github.abaddon.kcqrs.eventstoredb.eventstore.toDomainEvent
import io.kurrent.dbclient.NackAction
import io.kurrent.dbclient.PersistentSubscription
import io.kurrent.dbclient.PersistentSubscriptionListener
import io.kurrent.dbclient.ResolvedEvent
import io.kurrent.dbclient.Subscription
import io.kurrent.dbclient.SubscriptionListener
import kotlinx.coroutines.launch


abstract class EventStoreProjectionHandler<TProjection : IProjection>(
    override val repository: IProjectionRepository<TProjection>
) : ProjectionHandler<TProjection>() {


    fun getPersistentSubscriptionListener(): PersistentSubscriptionListener =
        object : PersistentSubscriptionListener() {
            override fun onEvent(subscription: PersistentSubscription, retryCount: Int, event: ResolvedEvent) {
                launch(coroutineContext) {
                    try {
                        log.info("New event received: [eventType: ${event.event?.eventType}, stream: ${event.event?.streamId}].")
                        processEvent(event)
                        subscription.ack(event);
                    } catch (ex: Exception) {
                        subscription.nack(NackAction.Park, ex.message, event);
                    }
                }
            }

            override fun onCancelled(subscription: PersistentSubscription?, exception: Throwable?) {
                if (exception == null) {
                    log.warn("Persistent subscription cancelled without exception.")
                } else {
                    log.error("Persistent subscription cancelled with exception: ${exception.message}", exception)
                }
            }
        }

    fun getSubscriptionListener(): SubscriptionListener = object : SubscriptionListener() {
        override fun onEvent(
            subscription: Subscription?,
            event: ResolvedEvent
        ) {
            launch(coroutineContext) {
                try {
                    log.info("New event received: [eventType: ${event.event?.eventType}, stream: ${event.event?.streamId}].")
                    processEvent(event)
                } catch (ex: Exception) {
                    log.error("Unexpected error processing event ${event.event?.eventType}", ex)
                }
            }
        }
    }

    private suspend fun processEvent(event: ResolvedEvent) {
        try {
            when (val domainEvent =
                transformToDomainEvent(event)) {
                null -> log.warn("Event received not converted in a DomainEvent")
                else -> {
                    val result = onEvent(domainEvent)
                    if (result.isFailure) {
                        log.error(
                            "Failed to process event: ${result.exceptionOrNull()?.message}",
                            result.exceptionOrNull()
                        )
                    }
                }
            }
        } catch (ex: UnsupportedOperationException) {
            log.error("event ${event.event?.eventType} not applied.", ex)
        } catch (ex: Exception) {
            log.error("Unexpected error processing event ${event.event?.eventType}", ex)
        }
    }


//    override suspend fun onEvent(event: IDomainEvent): Result<Unit> = withContext(coroutineContext) {
//        onEvents(flowOf(event))
//    }
//
//    override suspend fun onEvents(events: List<IDomainEvent>): Result<Unit> = withContext(coroutineContext) {
//        onEvents(events.asFlow())
//    }
//
//    override suspend fun onEvents(events: Flow<IDomainEvent>): Result<Unit> =
//        withContext(coroutineContext) {
//            repository.getByKey(projectionKey)
//                .flatMap {
//                    updateProjection(it, events)
//                }.flatMap {
//                    saveProjection(it)
//                }
//        }
//
//    @Suppress("UNCHECKED_CAST")
//    private suspend fun updateProjection(
//        currentProjection: TProjection,
//        events: Flow<IDomainEvent>
//    ): Result<TProjection> = withContext(coroutineContext) {
//        runCatching {
//            events.fold(currentProjection) { currentProjection, event ->
//                currentProjection.applyEvent(event) as TProjection
//            }
//        }
//    }
//
//    private suspend fun saveProjection(
//        projection: TProjection
//    ): Result<Unit> = withContext(coroutineContext) {
//        repository.save(projection)
//    }

    // In EventStoreDB 4.x, non-nullability is enforced in the method signature
    private fun transformToDomainEvent(event: ResolvedEvent): IDomainEvent? = event.event?.toDomainEvent()
}