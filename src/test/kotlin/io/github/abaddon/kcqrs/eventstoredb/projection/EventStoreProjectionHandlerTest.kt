package io.github.abaddon.kcqrs.eventstoredb.projection

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
import io.kurrent.dbclient.Position
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
internal class EventStoreProjectionHandlerTest : WithEventStoreDBContainer() {

    companion object {
        private const val STREAM_NAME = "CounterAggregateRoot"
    }

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    lateinit var repositoryConfig: EventStoreDBRepositoryConfig
    lateinit var repository: EventStoreDBRepository<CounterAggregateRoot>

    @BeforeEach
    fun setUp() {
        val connectionString = "kurrentdb://127.0.0.1:${container.getMappedPort(2113)}?tls=false&tlsVerifyCert=false"
        repositoryConfig = EventStoreDBRepositoryConfig(
            EventStoreDBConfig(connectionString),
            STREAM_NAME, 500, 500
        )
        repository = EventStoreDBRepository(
            repositoryConfig,
            { CounterAggregateRoot(it as CounterAggregateId) },
            testDispatcher
        )
    }

    @AfterEach
    fun close() {
        repository.cleanup()
    }

    @Test
    fun `Given an event, when it's applied to a projection then the updated projection is updated`() =
        testScope.runTest {
            // Given
            val projectionKey = DummyProjectionKey("test1")
            val subscriptionFilterConfig = SubscriptionFilterConfig(
                SubscriptionFilterConfig.SUBSCRIPTION_FILTER_STREAM_NAME_PREFIX,
                "${repositoryConfig.streamName}."
            )

            val projectionRepository = InMemoryProjectionRepository<DummyProjection>() {
                DummyProjection(it as DummyProjectionKey, 0)
            }

            val eventStoreProjectionHandler = EventStoreProjectionHandler(
                projectionRepository,
                projectionKey,
                subscriptionFilterConfig,
                Position(0, 0)
            )
            repository.subscribe(eventStoreProjectionHandler)


            //When
            runCatching {
                val counterAggregateId = CounterAggregateId()
                val aggregate = CounterAggregateRoot.initialiseCounter(counterAggregateId, 5)
                println("aggregateId: ${aggregate.id}")

                repository.save(aggregate, UUID.randomUUID())

                projectionRepository.getByKey(projectionKey)
            }
                //Then
                .onSuccess { actualProjection ->
                    val expectedProjection = DummyProjection(projectionKey, 1)
                    assertEquals(expectedProjection, actualProjection)

                }.onFailure {
                    println(it)
                    assert(false)
                }


        }
}