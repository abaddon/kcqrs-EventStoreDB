package io.github.abaddon.kcqrs.eventstoredb.eventstore


import io.github.abaddon.kcqrs.eventstoredb.config.EventStoreDBConfig
import io.github.abaddon.kcqrs.testHelpers.WithEventStoreDBContainer
import io.github.abaddon.kcqrs.testHelpers.entities.CounterAggregateId
import io.github.abaddon.kcqrs.testHelpers.entities.CounterAggregateRoot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import  java.util.UUID
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
internal class EventStoreDBRepositoryTest : WithEventStoreDBContainer() {
    companion object {
        private const val STREAM_NAME_PREFIX = "CounterAggregateRoot"
    }

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    lateinit var repositoryConfig: EventStoreDBRepositoryConfig
    lateinit var repository: EventStoreDBRepository<CounterAggregateRoot>


    @BeforeEach
    fun setUp() {
        // Use a unique stream name for each test to prevent event pollution
        val uniqueStreamName = "$STREAM_NAME_PREFIX-${UUID.randomUUID()}"
        val connectionString = "kurrentdb://127.0.0.1:${container.getMappedPort(2113)}?tls=false&tlsVerifyCert=false"
        repositoryConfig = EventStoreDBRepositoryConfig(EventStoreDBConfig(connectionString), uniqueStreamName, 500, 500)
        repository = EventStoreDBRepository(
            repositoryConfig,
            { CounterAggregateRoot(it as CounterAggregateId) }
        )
    }

    @Test
    fun `Given the initialise event when persist it then event is stored in the eventStore`() = testScope.runTest {
        //Given
        val counterAggregateId = CounterAggregateId()
        val aggregate = CounterAggregateRoot.initialiseCounter(counterAggregateId, 5)
        val result: Result<CounterAggregateRoot> = runCatching {
            //When
            repository.save(aggregate, UUID.randomUUID())

            //Then
            repository.getById(counterAggregateId)

        }.getOrElse { exception ->
            Result.failure(exception)
        }

        result
            .onSuccess { aggregateRoot ->
                assertEquals(counterAggregateId, aggregateRoot.id)
                assertEquals(5, aggregateRoot.counter)
                println("test Completed")
            }
            .onFailure {
                println(it)
                assert(false)
            }

    }

    @Test
    fun `Given an existing aggregate when persist a new event then the new event is stored in the eventStore`() =
        testScope.runTest {

            //Given
            val counterAggregateId = CounterAggregateId()
            val aggregate = CounterAggregateRoot.initialiseCounter(counterAggregateId, 5)

            val result: Result<CounterAggregateRoot> = runCatching {
                repository.save(aggregate, UUID.randomUUID())

                val initialAggregateResult = repository.getById(counterAggregateId)
                if (initialAggregateResult.isFailure) {
                    println(initialAggregateResult.exceptionOrNull())
                    assert(false)
                }

                //When
                val initialAggregate = initialAggregateResult.getOrThrow()
                val updatedAggregate = initialAggregate.increaseCounter(7)
                val result = repository.save(updatedAggregate, UUID.randomUUID())
                if (result.isFailure) {
                    println(result.exceptionOrNull())
                    assert(false)
                }

                //Then
                repository.getById(counterAggregateId)
            }.getOrElse { exception ->
                Result.failure(exception)
            }

            result
                .onSuccess { aggregateRoot ->
                    assertEquals(counterAggregateId, aggregateRoot.id)
                    assertEquals(12, aggregateRoot.counter)
                    println("test Completed")
                }
                .onFailure {
                    println(it)
                    assert(false)
                }

        }

    @Test
    fun `Given non-existing aggregate when getById then returns empty aggregate`() = testScope.runTest {
        // Given
        val nonExistingId = CounterAggregateId()

        // When
        val result = repository.getById(nonExistingId)

        // Then
        result
            .onSuccess { aggregateRoot ->
                assertEquals(nonExistingId, aggregateRoot.id)
                assertEquals(0, aggregateRoot.counter)
                assertEquals(0L, aggregateRoot.version)
            }
            .onFailure {
                assert(false) { "Should not fail for non-existing aggregate: ${it.message}" }
            }
    }

    @Test
    fun `Given aggregate id when aggregateIdStreamName called then returns correct stream name`() =
        testScope.runTest {
            // Given
            val aggregateId = CounterAggregateId()

            // When
            val streamName = repository.aggregateIdStreamName(aggregateId)

            // Then
            // Stream name should not contain the aggregate ID
            assertEquals(repositoryConfig.streamName, streamName)
        }

    @Test
    fun `Given aggregate with multiple events when saved then all events are persisted`() =
        testScope.runTest {
            // Given
            val counterAggregateId = CounterAggregateId()
            val aggregate = CounterAggregateRoot.initialiseCounter(counterAggregateId, 10)
            val aggregateWithMultipleEvents = aggregate
                .increaseCounter(5)
                .increaseCounter(3)
                .decreaseCounter(2)

            // When
            repository.save(aggregateWithMultipleEvents, UUID.randomUUID())

            // Then
            val result = repository.getById(counterAggregateId)
            result
                .onSuccess { aggregateRoot ->
                    assertEquals(counterAggregateId, aggregateRoot.id)
                    assertEquals(16, aggregateRoot.counter) // 10 + 5 + 3 - 2
                    assertEquals(3L, aggregateRoot.version)
                }
                .onFailure {
                    assert(false) { "Failed to retrieve aggregate: ${it.message}" }
                }
        }

    @Test
    fun `Given aggregate when decrease counter below zero then error event is raised`() =
        testScope.runTest {
            // Given
            val counterAggregateId = CounterAggregateId()
            val aggregate = CounterAggregateRoot.initialiseCounter(counterAggregateId, 5)

            // When
            repository.save(aggregate, UUID.randomUUID())
            val savedAggregate = repository.getById(counterAggregateId).getOrThrow()
            val aggregateWithError = savedAggregate.decreaseCounter(10) // This should create error event

            repository.save(aggregateWithError, UUID.randomUUID())

            // Then
            val result = repository.getById(counterAggregateId)
            result
                .onSuccess { aggregateRoot ->
                    // Counter should remain 5 as the decrease operation failed
                    assertEquals(5, aggregateRoot.counter)
                    // Version should increase due to error event
                    assertEquals(1L, aggregateRoot.version)
                }
                .onFailure {
                    assert(false) { "Failed to retrieve aggregate: ${it.message}" }
                }
        }

    @Test
    fun `Given aggregate when emptyAggregate called then returns new aggregate with given id`() =
        testScope.runTest {
            // Given
            val aggregateId = CounterAggregateId()

            // When
            val emptyAggregate = repository.emptyAggregate(aggregateId)

            // Then
            assertEquals(aggregateId, emptyAggregate.id)
            assertEquals(0, emptyAggregate.counter)
            assertEquals(0L, emptyAggregate.version)
            assertEquals(0, emptyAggregate.uncommittedEvents.size)
        }


}

