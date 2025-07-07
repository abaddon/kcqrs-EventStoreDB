package io.github.abaddon.kcqrs.eventstoredb.projection

import io.github.abaddon.kcqrs.eventstoredb.config.EventStoreDBConfig
import io.github.abaddon.kcqrs.eventstoredb.eventstore.EventStoreDBRepository
import io.github.abaddon.kcqrs.eventstoredb.eventstore.EventStoreDBRepositoryConfig
import io.github.abaddon.kcqrs.testHelpers.WithEventStoreDBContainer
import io.github.abaddon.kcqrs.testHelpers.entities.CounterAggregateId
import io.github.abaddon.kcqrs.testHelpers.entities.CounterAggregateRoot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.jupiter.api.BeforeEach

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
            { CounterAggregateRoot(it as CounterAggregateId) }
        )
    }

//    @Test
//    fun `Given an event, when it's applied to a projection then the updated projection is updated`() =
//        testScope.runTest {
//            // Given
//            val projectionKey = DummyProjectionKey("test1")
//            val subscriptionFilterConfig = SubscriptionFilterConfig(
//                SubscriptionFilterConfig.SUBSCRIPTION_FILTER_STREAM_NAME_PREFIX,
//                "${repositoryConfig.streamName}."
//            )
//
//            val projectionRepository = InMemoryProjectionRepository() {
//                DummyProjection(it as DummyProjectionKey, 0)
//            }
//
//            val eventStoreProjectionHandler = EventStoreProjectionHandler(
//                projectionRepository,
//                projectionKey,
//                subscriptionFilterConfig
//            )
//            repository.subscribe(eventStoreProjectionHandler)
//
//            // When
//            val counterAggregateId = CounterAggregateId()
//            val aggregate = CounterAggregateRoot.initialiseCounter(counterAggregateId, 5)
//
//            repository.save(aggregate, UUID.randomUUID())
//                .onFailure {
//                    log.error("Failed to save aggregate", it)
//                    assert(false) { "Failed to save aggregate: ${it.message}" }
//                }
//
//            // Wait for the projection to be updated
//            // Poll until the projection is updated or timeout
//            var projectionUpdated = false
//            val startTime = System.currentTimeMillis()
//            val timeout = 5000L // 5 seconds timeout
//
//            while (!projectionUpdated && (System.currentTimeMillis() - startTime) < timeout) {
//                delay(50) // Small delay between checks
//                runCurrent()
//
//                val result = projectionRepository.getByKey(projectionKey)
//                projectionUpdated = result.isSuccess && result.getOrNull()?.numEvents == 1
//            }
//
//            // Then - Verify projection was updated
//            assert(projectionUpdated) { "Projection was not updated within timeout period" }
//
//            val result = projectionRepository.getByKey(projectionKey)
//            result
//                .onSuccess { actualProjection ->
//                    var event = CounterInitialisedEvent.create(counterAggregateId, 5, 1)
//                    val lastProcessedEvent: ConcurrentHashMap<String, Long> = ConcurrentHashMap()
//                    lastProcessedEvent[event.aggregateType] = 1
//                    val expectedProjection = DummyProjection(projectionKey, 1, lastProcessedEvent)
//                    assertThat(actualProjection)
//                        .usingRecursiveComparison()
//                        .ignoringFields("lastUpdated")
//                        .isEqualTo(expectedProjection)
//                }
//                .onFailure {
//                    assert(false) { "Failed to get projection: ${it.message}" }
//                }
//        }
}