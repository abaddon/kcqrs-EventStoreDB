package io.github.abaddon.kcqrs.eventstoredb.eventstore

import com.eventstore.dbclient.*
import io.github.abaddon.kcqrs.core.IAggregate
import io.github.abaddon.kcqrs.core.IIdentity
import io.github.abaddon.kcqrs.core.domain.messages.events.IDomainEvent
import io.github.abaddon.kcqrs.core.persistence.EventStoreRepository
import io.github.abaddon.kcqrs.core.projections.IProjection
import io.github.abaddon.kcqrs.core.projections.IProjectionHandler
import io.github.abaddon.kcqrs.eventstoredb.projection.EventStoreProjectionHandler
import org.slf4j.LoggerFactory
import java.security.InvalidParameterException
import java.util.concurrent.CompletionException


class EventStoreDBRepository<TAggregate : IAggregate>(
    eventStoreRepositoryConfig: EventStoreDBRepositoryConfig,
    private val funEmpty: (identity: IIdentity) -> TAggregate
) :
    EventStoreRepository<TAggregate>() {

    override val log = LoggerFactory.getLogger(this::class.simpleName)
    private val client: EventStoreDBClient =
        EventStoreDBClient.create(eventStoreRepositoryConfig.eventStoreDBClientSettings())
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
        do {
            val options = ReadStreamOptions.get().forwards().fromRevision(currentRevision)
            val result: ReadResult = try {
                client.readStream(streamName, MAX_READ_PAGE_SIZE, options).join()
            } catch (ex: CompletionException) {
                when (ex.cause) {
                    is StreamNotFoundException -> ReadResult(mutableListOf())
                    else -> {
                        log.error("Stream not read", ex)
                        ReadResult(mutableListOf())
                    }
                }
            }
            val events = result.events
            eventsFound.addAll(events.toDomainEvents())
            currentRevision += events.size
        } while (result.events.isNotEmpty())

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
        val expectedRevision: ExpectedRevision =
            if (currentVersion <= 0L) ExpectedRevision.NO_STREAM else ExpectedRevision.expectedRevision(currentVersion - 1L)
        val options = AppendToStreamOptions.get()
            .expectedRevision(expectedRevision)
        
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
            ?: SubscribeToAllOptions.get()
        
        client.subscribeToAll(projectionHandler, options)
    }

    override fun publish(events: List<IDomainEvent>) {}

}