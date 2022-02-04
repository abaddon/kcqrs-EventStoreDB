package io.github.abaddon.kcqrs.eventstores.eventstoredb

import com.eventstore.dbclient.*
import io.github.abaddon.kcqrs.core.IAggregate
import io.github.abaddon.kcqrs.core.IIdentity
import io.github.abaddon.kcqrs.core.domain.messages.events.IDomainEvent
import io.github.abaddon.kcqrs.core.exceptions.AggregateVersionException
import io.github.abaddon.kcqrs.core.helpers.foldEvents
import io.github.abaddon.kcqrs.core.persistence.IRepository
import org.slf4j.LoggerFactory
import java.security.InvalidParameterException
import java.time.Instant
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance


const val COMMIT_ID_HEADER = "CommitId"
const val COMMIT_DATE_HEADER = "CommitDate"
const val AGGREGATE_TYPE_HEADER = "AggregateTypeName"

class EventStoreDBRepository<TAggregate: IAggregate>(private val client: EventStoreDBClient, repositoryConfig: EventStoreDBConfig, private val klass: KClass<TAggregate>) :
    IRepository<TAggregate> {

    private val log = LoggerFactory.getLogger(this::class.simpleName)
    private val MAX_READ_PAGE_SIZE: Long = repositoryConfig.maxReadPageSize;
    private val MAX_WRITE_PAGE_SIZE: Int = repositoryConfig.maxWritePageSize;

    override fun aggregateIdStreamName(aggregateId: IIdentity): String {
        val className = klass.simpleName.orEmpty();
        check(className.isNotEmpty()) { throw InvalidParameterException("Cannot get className empty") }
        return "$className.${aggregateId.valueAsString()}"
    }

    override suspend fun getById(
        aggregateId: IIdentity
    ): TAggregate {
        return getById(aggregateId, Long.MAX_VALUE)
    }

    override suspend fun getById(
        aggregateId: IIdentity,
        version: Long
    ): TAggregate {
        check(version > 0) { throw InvalidParameterException("Cannot get version <= 0. Current value: $version") }
        val streamName: String = aggregateIdStreamName(aggregateId)
        val emptyAggregate = klass.createInstance()
        var hydratedAggregate = emptyAggregate;

        var startFrom: Long = 0;
        do {
            val options = ReadStreamOptions.get().forwards().fromRevision(startFrom);
            val result: ReadResult = client.readStream(streamName, MAX_READ_PAGE_SIZE, options).get()

            hydratedAggregate = result.events.toDomainEvents().foldEvents(hydratedAggregate)
            startFrom += result.events.size
        } while (version > hydratedAggregate.version && result.events.isNotEmpty())

        check(hydratedAggregate.version != version && version <= Long.MAX_VALUE) {
            throw AggregateVersionException(
                aggregateId,
                klass,
                hydratedAggregate.version,
                version
            )
        }

        return hydratedAggregate;
    }

    override suspend fun save(aggregate: TAggregate, commitID: UUID, updateHeaders: Map<String, String>) {
        val header = updateHeaders + mapOf(
            Pair(COMMIT_ID_HEADER, commitID.toString()),
            Pair(COMMIT_DATE_HEADER, Instant.now().toString()),
            Pair(AGGREGATE_TYPE_HEADER, aggregate::class.simpleName.orEmpty()),
        )
        val streamName: String = aggregateIdStreamName(aggregate.id)
        val uncommittedEvents: List<IDomainEvent> = aggregate.uncommittedEvents();

        val originalVersion = aggregate.version - uncommittedEvents.size
        val expectedRevision: ExpectedRevision =
            if (originalVersion <= 0L) ExpectedRevision.NO_STREAM else ExpectedRevision.expectedRevision(originalVersion - 1L)
        log.info("aggregate.version: ${aggregate.version}, uncommittedEvents.size: ${uncommittedEvents.size}, originalVersion: $originalVersion, expectedRevision: ${expectedRevision}")
        val eventsToSave = uncommittedEvents.map { domainEvent -> domainEvent.toEventData(header) }

        val options = AppendToStreamOptions.get()
            .expectedRevision(expectedRevision)
        val writeResultFuture = client.appendToStream(streamName, options, eventsToSave.iterator())
        writeResultFuture.whenComplete { writeResult, error ->
            if (error == null) {
                log.info("events published on stream $streamName, nextExpectedRevision: ${writeResult.nextExpectedRevision.valueUnsigned}")
            } else {
                log.error("events not published on stream $streamName", error);
            }
        }
    }

}