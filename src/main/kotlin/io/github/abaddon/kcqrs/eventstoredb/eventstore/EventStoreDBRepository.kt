package io.github.abaddon.kcqrs.eventstoredb.eventstore


import io.github.abaddon.kcqrs.core.IAggregate
import io.github.abaddon.kcqrs.core.IIdentity
import io.github.abaddon.kcqrs.core.domain.messages.events.IDomainEvent
import io.github.abaddon.kcqrs.core.helpers.LoggerFactory.log
import io.github.abaddon.kcqrs.core.persistence.EventStoreRepository
import io.github.abaddon.kcqrs.core.projections.IProjection
import io.github.abaddon.kcqrs.core.projections.IProjectionHandler
import io.github.abaddon.kcqrs.eventstoredb.projection.EventStoreProjectionHandler
import io.kurrent.dbclient.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.security.InvalidParameterException
import java.util.concurrent.CompletionException
import kotlin.coroutines.CoroutineContext


class EventStoreDBRepository<TAggregate : IAggregate>(
    eventStoreRepositoryConfig: EventStoreDBRepositoryConfig,
    private val funEmpty: (identity: IIdentity) -> TAggregate,
    coroutineContext: CoroutineContext
) :
    EventStoreRepository<TAggregate>(coroutineContext) {
    private val client: KurrentDBClient =
        KurrentDBClient.create(eventStoreRepositoryConfig.eventStoreDBClientSettings())
    private val MAX_READ_PAGE_SIZE: Long = eventStoreRepositoryConfig.maxReadPageSize
    private val MAX_WRITE_PAGE_SIZE: Int = eventStoreRepositoryConfig.maxWritePageSize
    private val streamName: String = eventStoreRepositoryConfig.streamName

    override fun emptyAggregate(aggregateId: IIdentity): TAggregate = funEmpty(aggregateId)

    override suspend fun <TProjection : IProjection> subscribe(projectionHandler: IProjectionHandler<TProjection>) =
        withContext(coroutineContext) {
            when (projectionHandler) {
                is EventStoreProjectionHandler -> subscribeEventStoreProjectionHandler(projectionHandler)
                else -> log.warn("EventStoreProjectionHandler required, subscription failed")
            }
        }

    override suspend fun loadEvents(streamName: String, startFrom: Long): Result<Flow<IDomainEvent>> =
        withContext(coroutineContext) {
            var currentRevision: Long = startFrom
            var totalEventsLoaded = 0
            log.debug("loading events from stream {} with startRevision {}", streamName, startFrom)

            val result = runCatching {
                flow<IDomainEvent> {
                    var hasMoreEvents = true
                    while (hasMoreEvents) {
                        val options = ReadStreamOptions.get()
                            .forwards()
                            .fromRevision(currentRevision)
                            .maxCount(MAX_READ_PAGE_SIZE)
                        val result = client.readStream(streamName, options).get()

                        val events = result.events
                        log.debug(
                            "events received: {}, firstStreamPosition: {}, lastStreamPosition {}",
                            events.size,
                            result.firstStreamPosition,
                            result.lastStreamPosition
                        )

                        if (events.isEmpty()) {
                            hasMoreEvents = false
                            log.debug("stream is empty")
                        } else {
                            val domainEvents = events.toDomainEvents()

                            // Emit each domain event individually
                            domainEvents.forEach { domainEvent ->
                                totalEventsLoaded += 1
                                emit(domainEvent)
                            }

                            val maxRevision = events.maxOfOrNull { event ->
                                log.debug("event.originalEvent.revision, {}", event.originalEvent.revision)
                                event.originalEvent.revision
                            }
                            log.debug("maxRevision is {}", maxRevision)

                            currentRevision += events.size
                            if (currentRevision != maxRevision) {
                                log.warn(
                                    "currentRevision and maxRevision are different! {} and {}",
                                    currentRevision,
                                    maxRevision
                                )
                            }
                        }
                    }
                }
            }

            log.debug(
                "end loading events from stream {} with startRevision {} getting {} events",
                streamName,
                startFrom,
                totalEventsLoaded
            )

            when {
                result.isFailure -> {
                    val ex = result.exceptionOrNull()!!
                    log.debug("Error reading stream: {}", streamName, ex)
                    when (ex.cause) {
                        is StreamNotFoundException -> {
                            log.debug("Stream not found: {}", streamName)
                            Result.success(flow<IDomainEvent> {})
                        }

                        else -> {
                            log.error("Error reading stream: {}", streamName, ex)
                            result
                        }
                    }
                }

                else -> result
            }
        }

    override fun aggregateIdStreamName(aggregateId: IIdentity): String {
        check(streamName.isNotEmpty()) { throw InvalidParameterException("Cannot get streamName empty") }
        return "$streamName.${aggregateId.valueAsString()}"
    }

    override suspend fun persist(
        streamName: String,
        uncommittedEvents: List<IDomainEvent>,
        header: Map<String, String>,
        currentVersion: Long
    ): Result<Unit> = withContext(coroutineContext) {
        log.debug(
            "persisting uncommittedEvents {} with currentVersion {} on stream {}",
            uncommittedEvents,
            currentVersion,
            streamName
        )
        val eventsToSave = uncommittedEvents.map { domainEvent -> domainEvent.toEventData(header) }
        val options: AppendToStreamOptions =
            if (currentVersion <= 0L)
                AppendToStreamOptions.get().streamState(StreamState.noStream())
            else
                AppendToStreamOptions.get().streamRevision(currentVersion - 1)

        // The append method has changed in EventStoreDB 4.x
        try {
            client.appendToStream(streamName, options, eventsToSave.iterator()).get()
            log.debug("Events applied on stream $streamName")
            Result.success(Unit)
        } catch (ex: CompletionException) {
            log.error("Events not published on stream $streamName")
            Result.failure(ex)
        }

//        writeResultFuture.whenComplete { writeResult, error ->
//            if (error == null) {
//                log.info("Events published on stream $streamName, nextExpectedRevision: ${writeResult.nextExpectedRevision}")
//                Result.success(Unit)
//            } else {
//                log.error("Events not published on stream $streamName", error)
//                Result.failure(error)
//            }
//        }.get()
    }

    private fun <TProjection : IProjection> subscribeEventStoreProjectionHandler(projectionHandler: EventStoreProjectionHandler<TProjection>) {
        log.debug("subscribing to projection handler {}", projectionHandler)
        val options = projectionHandler.subscriptionFilter?.subscribeToAllOptions(projectionHandler.position)
            ?: SubscribeToAllOptions.get().fromStart()

        val subscription = client.subscribeToAll(projectionHandler, options).get()
        log.debug("subscribed to projection {}", subscription)

    }

    override suspend fun publish(events: List<IDomainEvent>): Result<Unit> =
        Result.success(Unit)

}