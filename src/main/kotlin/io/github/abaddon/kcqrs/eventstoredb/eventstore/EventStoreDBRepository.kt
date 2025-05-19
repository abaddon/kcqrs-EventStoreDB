package io.github.abaddon.kcqrs.eventstoredb.eventstore


import io.github.abaddon.kcqrs.core.IAggregate
import io.github.abaddon.kcqrs.core.IIdentity
import io.github.abaddon.kcqrs.core.domain.messages.events.IDomainEvent
import io.github.abaddon.kcqrs.core.persistence.EventStoreRepository
import io.github.abaddon.kcqrs.core.projections.IProjection
import io.github.abaddon.kcqrs.core.projections.IProjectionHandler
import io.github.abaddon.kcqrs.eventstoredb.projection.EventStoreProjectionHandler
import io.kurrent.dbclient.*
import org.slf4j.LoggerFactory
import java.security.InvalidParameterException
import java.util.concurrent.CompletionException


class EventStoreDBRepository<TAggregate : IAggregate>(
    eventStoreRepositoryConfig: EventStoreDBRepositoryConfig,
    private val funEmpty: (identity: IIdentity) -> TAggregate
) :
    EventStoreRepository<TAggregate>() {

    override val log = LoggerFactory.getLogger(this::class.simpleName)
    private val client: KurrentDBClient =
        KurrentDBClient.create(eventStoreRepositoryConfig.eventStoreDBClientSettings())
    private val MAX_READ_PAGE_SIZE: Long = eventStoreRepositoryConfig.maxReadPageSize
    private val MAX_WRITE_PAGE_SIZE: Int = eventStoreRepositoryConfig.maxWritePageSize
    private val streamName: String = eventStoreRepositoryConfig.streamName

    override fun emptyAggregate(aggregateId: IIdentity): TAggregate = funEmpty(aggregateId)

    override fun <TProjection : IProjection> subscribe(projectionHandler: IProjectionHandler<TProjection>) {
        when (projectionHandler) {
            is EventStoreProjectionHandler -> subscribeEventStoreProjectionHandler(projectionHandler)
            else -> log.warn("EventStoreProjectionHandler required, subscription failed")
        }
    }

    override fun load(streamName: String, startFrom: Long): List<IDomainEvent> {
        val eventsFound = mutableListOf<IDomainEvent>()
        var currentRevision: Long = startFrom

        try {
            var hasMoreEvents = true
            while (hasMoreEvents) {
                val options = ReadStreamOptions.get()
                    .forwards()
                    .fromRevision(currentRevision)
                    .maxCount(MAX_READ_PAGE_SIZE)
                val result = client.readStream(streamName, options).join()

                val events = result.events
                val maxRevision = events.maxOfOrNull { event ->
                    log.info("event.originalEvent.revision, {}", event.originalEvent.revision)
                    event.originalEvent.revision
                };
                if (events.isEmpty()) {
                    hasMoreEvents = false
                } else {
                    eventsFound.addAll(events.toDomainEvents())
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
        } catch (ex: CompletionException) {
            when (ex.cause) {
                is StreamNotFoundException -> log.debug("Stream not found: {}", streamName)
                else -> log.error("Error reading stream: {}", streamName, ex)
            }
        }

        return eventsFound
    }

    override fun aggregateIdStreamName(aggregateId: IIdentity): String {
        check(streamName.isNotEmpty()) { throw InvalidParameterException("Cannot get streamName empty") }
        return "$streamName.${aggregateId.valueAsString()}"
    }

    override fun persist(
        streamName: String,
        uncommittedEvents: List<IDomainEvent>,
        header: Map<String, String>,
        currentVersion: Long
    ) {
        val eventsToSave = uncommittedEvents.map { domainEvent -> domainEvent.toEventData(header) }
        val options: AppendToStreamOptions =
            if (currentVersion <= 0L)
                AppendToStreamOptions.get().streamState(StreamState.noStream())
            else
                AppendToStreamOptions.get().streamRevision(currentVersion - 1)

        // The append method has changed in EventStoreDB 4.x
        val writeResultFuture = client.appendToStream(streamName, options, eventsToSave.iterator())

        writeResultFuture.whenComplete { writeResult, error ->
            if (error == null) {
                log.info("Events published on stream $streamName, nextExpectedRevision: ${writeResult.nextExpectedRevision}")
            } else {
                log.error("Events not published on stream $streamName", error)
            }
        }
    }

    private fun <TProjection : IProjection> subscribeEventStoreProjectionHandler(projectionHandler: EventStoreProjectionHandler<TProjection>) {
        // In EventStoreDB 4.x, the subscription API has been updated
        val options = projectionHandler.subscriptionFilter?.subscribeToAllOptions(projectionHandler.position)
            ?: SubscribeToAllOptions.get().fromStart()

        client.subscribeToAll(projectionHandler, options)
    }

    override fun publish(events: List<IDomainEvent>) {}

}