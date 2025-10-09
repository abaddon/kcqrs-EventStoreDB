package io.github.abaddon.kcqrs.eventstoredb.eventstore


import io.github.abaddon.kcqrs.core.IAggregate
import io.github.abaddon.kcqrs.core.IIdentity
import io.github.abaddon.kcqrs.core.domain.messages.events.IDomainEvent
import io.github.abaddon.kcqrs.core.helpers.KcqrsLoggerFactory.log
import io.github.abaddon.kcqrs.core.persistence.EventStoreRepository
import io.kurrent.dbclient.AppendToStreamOptions
import io.kurrent.dbclient.KurrentDBClient
import io.kurrent.dbclient.ReadStreamOptions
import io.kurrent.dbclient.ResolvedEvent
import io.kurrent.dbclient.StreamNotFoundException
import io.kurrent.dbclient.StreamState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import java.security.InvalidParameterException
import java.util.concurrent.ExecutionException


class EventStoreDBRepository<TAggregate : IAggregate>(
    eventStoreRepositoryConfig: EventStoreDBRepositoryConfig,
    private val funEmpty: (identity: IIdentity) -> TAggregate
) :
    EventStoreRepository<TAggregate>() {
    private val client: KurrentDBClient =
        KurrentDBClient.create(eventStoreRepositoryConfig.eventStoreDBClientSettings())
    private val MAX_READ_PAGE_SIZE: Long = eventStoreRepositoryConfig.maxReadPageSize
    private val MAX_WRITE_PAGE_SIZE: Int = eventStoreRepositoryConfig.maxWritePageSize
    private val streamName: String = eventStoreRepositoryConfig.streamName

    override fun emptyAggregate(aggregateId: IIdentity): TAggregate = funEmpty(aggregateId)

    override suspend fun loadEvents(streamName: String, startFrom: Long): Result<Flow<IDomainEvent>> = runCatching {
        var startFromRevision: Long = startFrom
        var totalEventsLoaded = 0
        log.debug("loading events from stream {} with startRevision {}", streamName, startFrom)
        var hasMoreEvents = true
        val domainEventFounds: MutableList<IDomainEvent> = mutableListOf()
        while (hasMoreEvents) {
            val options = ReadStreamOptions.get()
                .forwards()
                .fromRevision(startFromRevision) // Start reading from the specified revision included
                .maxCount(MAX_READ_PAGE_SIZE)

            val eventsRead = readEventStore(options, streamName);
            val domainEvents = eventsRead.toDomainEvents()

            if (domainEvents.isEmpty()) {
                hasMoreEvents = false
                log.debug("stream is empty")
            } else {
                // Emit each domain event individually
                totalEventsLoaded += domainEvents.size //1

                val lastRevisionReceived = eventsRead.maxOfOrNull { event -> //0
                    log.debug(" event {} with revision, {}", event.event.eventType, event.originalEvent.revision)
                    event.originalEvent.revision
                }
                log.debug("last RevisionReceived is {}", lastRevisionReceived)

                //0+100-1
                val expectedLastRevisionReceived = startFromRevision + domainEvents.size - 1
                if (expectedLastRevisionReceived != lastRevisionReceived) {
                    log.warn(
                        "expectedLastRevisionReceived and lastRevisionReceived are different! {} and {}. Expected Revision calculated as {} + {} +1",
                        expectedLastRevisionReceived,
                        lastRevisionReceived,
                        startFromRevision,
                        domainEvents.size,
                    )
                }
                startFromRevision = (lastRevisionReceived ?: -1) + 1
            }
            domainEventFounds.addAll(domainEvents)
        }
        domainEventFounds.asFlow()
    }


    private fun readEventStore(
        options: ReadStreamOptions,
        streamName: String
    ): List<ResolvedEvent> {
        try {
            val result = client.readStream(streamName, options).get()
            log.debug(
                "events received: {}, firstStreamPosition: {}, lastStreamPosition {}",
                result.events.size,
                result.firstStreamPosition,
                result.lastStreamPosition
            )
            log.debug("stream contains {} events", result.events.size)
            return result.events
//            return result.events.toDomainEvents()

        } catch (ex: ExecutionException) {
            if (ex.cause is StreamNotFoundException) {
                log.debug("Stream not found: {}", streamName)
                return listOf<ResolvedEvent>()
            } else
                throw ex
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
    ): Result<Unit> = runCatching {
        log.debug(
            "persisting uncommittedEvents {} with currentVersion {} on stream {}",
            uncommittedEvents,
            currentVersion,
            streamName
        )
        val eventsToSave = uncommittedEvents.map { domainEvent -> domainEvent.toEventData(header) }
        val options: AppendToStreamOptions =
            if (currentVersion < 0L)
                AppendToStreamOptions.get().streamState(StreamState.noStream())
            else
                AppendToStreamOptions.get().streamRevision(currentVersion)

        // The append method has changed in EventStoreDB 4.x
        try {
            client.appendToStream(streamName, options, eventsToSave.iterator()).get()
            log.debug("Events applied on stream $streamName")
            Result.success(Unit)
        } catch (ex: RuntimeException) {
            log.error("Events not published on stream $streamName")
            Result.failure(ex)
        }
    }

    override suspend fun publish(events: List<IDomainEvent>): Result<Unit> =
        Result.success(Unit)

}