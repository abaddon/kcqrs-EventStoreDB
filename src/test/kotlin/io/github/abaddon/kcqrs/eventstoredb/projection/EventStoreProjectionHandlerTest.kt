package io.github.abaddon.kcqrs.eventstoredb.projection

import io.kurrent.dbclient.Position
import io.github.abaddon.kcqrs.core.persistence.InMemoryProjectionRepository
import io.github.abaddon.kcqrs.eventstoredb.config.EventStoreDBConfig
import io.github.abaddon.kcqrs.eventstoredb.config.SubscriptionFilterConfig
import io.github.abaddon.kcqrs.eventstoredb.eventstore.EventStoreDBRepository
import io.github.abaddon.kcqrs.eventstoredb.eventstore.EventStoreDBRepositoryConfig
import io.github.abaddon.kcqrs.testHelpers.WithEventStoreDBContainer
import io.github.abaddon.kcqrs.testHelpers.entities.CounterAggregateId
import io.github.abaddon.kcqrs.testHelpers.entities.CounterAggregateRoot
import io.github.abaddon.kcqrs.testHelpers.projections.DummyProjection
import io.github.abaddon.kcqrs.testHelpers.projections.DummyProjectionKey
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.*


internal class EventStoreProjectionHandlerTest : WithEventStoreDBContainer() {

    companion object {
        lateinit var repositoryConfig: EventStoreDBRepositoryConfig
        val streamName = "EventStoreProjectionHandlerTest"

        @JvmStatic
        @BeforeAll
        fun setUp() {
            val connectionString = "kurrentdb://127.0.0.1:${container.getMappedPort(2113)}?tls=false&tlsVerifyCert=false"
            repositoryConfig = EventStoreDBRepositoryConfig(EventStoreDBConfig(connectionString), streamName, 500, 500)
        }
    }

    @Test
    fun `Given an event, when it's applied to a projection then the updated projection is updated`() = runBlocking {
        val repository = EventStoreDBRepository(repositoryConfig) { CounterAggregateRoot(it as CounterAggregateId) }

        //Setup
        val projectionKey = DummyProjectionKey("test1")
        val subscriptionFilterConfig = SubscriptionFilterConfig(
            SubscriptionFilterConfig.SUBSCRIPTION_FILTER_STREAM_NAME_PREFIX,
            "${repositoryConfig.streamName}."
        )

        val projectionRepository = InMemoryProjectionRepository<DummyProjection>(){
            DummyProjection(it as DummyProjectionKey,0)
        }

        // In EventStoreDB 4.x, we'll create a Position with specific values
        val eventStoreProjectionHandler = EventStoreProjectionHandler(
            projectionRepository,
            projectionKey,
            subscriptionFilterConfig,
            Position(0, 0) // Start position
        )

        //subscribe
        repository.subscribe(eventStoreProjectionHandler)


        //Start
        val counterAggregateId = CounterAggregateId()
        val aggregate = CounterAggregateRoot.initialiseCounter(counterAggregateId, 5)
        println("aggregateId: ${aggregate.id}")
        runBlocking {
            repository.save(aggregate, UUID.randomUUID())
            delay(1000)
            yield()
        }
        delay(2000)
        val expectedProjection = DummyProjection(projectionKey, 1)

        val actualProjection = projectionRepository.getByKey(projectionKey)

        kotlin.test.assertEquals(expectedProjection, actualProjection)

    }
}