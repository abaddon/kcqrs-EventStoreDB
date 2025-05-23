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
import java.util.*
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
internal class EventStoreDBRepositoryTest : WithEventStoreDBContainer() {
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
        repositoryConfig = EventStoreDBRepositoryConfig(EventStoreDBConfig(connectionString), STREAM_NAME, 500, 500)
        repository = EventStoreDBRepository(
            repositoryConfig,
            { CounterAggregateRoot(it as CounterAggregateId) },
            testDispatcher
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


}

