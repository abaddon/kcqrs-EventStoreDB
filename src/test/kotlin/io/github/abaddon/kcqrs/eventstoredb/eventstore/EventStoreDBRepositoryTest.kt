package io.github.abaddon.kcqrs.eventstoredb.eventstore

import io.github.abaddon.kcqrs.core.domain.Result
import io.github.abaddon.kcqrs.eventstoredb.config.EventStoreDBConfig
import io.github.abaddon.kcqrs.testHelpers.WithEventStoreDBContainer
import io.github.abaddon.kcqrs.testHelpers.entities.CounterAggregateId
import io.github.abaddon.kcqrs.testHelpers.entities.CounterAggregateRoot
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals


internal class EventStoreDBRepositoryTest : WithEventStoreDBContainer() {

    companion object {

        lateinit var repositoryConfig: EventStoreDBRepositoryConfig
        const val streamName = "CounterAggregateRoot"

        @JvmStatic
        @BeforeAll
        fun setUp() {
            val connectionString = "kurrentdb://127.0.0.1:${container.getMappedPort(2113)}?tls=false&tlsVerifyCert=false"
            repositoryConfig = EventStoreDBRepositoryConfig(EventStoreDBConfig(connectionString), streamName, 500, 500)
        }
    }

    @Test
    fun `Given the initialise event when persist it then event is stored in the eventStore`() {
        val repository = EventStoreDBRepository(repositoryConfig) { CounterAggregateRoot(it as CounterAggregateId) }
        val counterAggregateId = CounterAggregateId()
        println("counterAggregateId: ${counterAggregateId.value}")
        val aggregate = CounterAggregateRoot.initialiseCounter(counterAggregateId, 5)
        println("aggregateId: ${aggregate.id}")
        runBlocking {
            repository.save(aggregate, UUID.randomUUID())
        }
        runBlocking {
            delay(2000L)
            when (val result = repository.getById(counterAggregateId)) {
                is Result.Invalid -> assert(false)
                is Result.Valid -> {
                    val regeneratedAggregate = result.value
                    assertEquals(counterAggregateId, regeneratedAggregate.id)
                    assertEquals(5, regeneratedAggregate.counter)
                }
            }
        }


    }

    @Test
    fun `Given an existing aggregate when persist a new event then the new event is stored in the eventStore`() {
        val repository = EventStoreDBRepository(repositoryConfig) { CounterAggregateRoot(it as CounterAggregateId) }
        val counterAggregateId = CounterAggregateId()
        val aggregate = CounterAggregateRoot.initialiseCounter(counterAggregateId, 5)
        runBlocking {
            repository.save(aggregate, UUID.randomUUID())
            delay(2000L)
            when (val initialAggregateResult = repository.getById(counterAggregateId)) {
                is Result.Invalid -> assert(false)
                is Result.Valid -> {
                    val initialAggregate = initialAggregateResult.value
                    val updatedAggregate = initialAggregate.increaseCounter(7)
                    repository.save(updatedAggregate, UUID.randomUUID())
                    delay(2000L)
                    when (val regeneratedAggregateResult = repository.getById(counterAggregateId)) {
                        is Result.Invalid -> assert(false)
                        is Result.Valid -> {
                            val regeneratedAggregate = regeneratedAggregateResult.value
                            assertEquals(counterAggregateId, regeneratedAggregate.id)
                            assertEquals(12, regeneratedAggregate.counter)
                        }
                    }
                }
            }
        }
    }

}

