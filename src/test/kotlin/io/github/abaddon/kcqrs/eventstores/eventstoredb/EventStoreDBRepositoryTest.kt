package io.github.abaddon.kcqrs.eventstores.eventstoredb

import com.eventstore.dbclient.EventStoreDBClient
import com.eventstore.dbclient.EventStoreDBConnectionString
import io.github.abaddon.kcqrs.eventstores.eventstoredb.helpers.entities.CounterAggregateId
import io.github.abaddon.kcqrs.eventstores.eventstoredb.helpers.entities.CounterAggregateRoot
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.*
import kotlin.test.assertEquals


@Testcontainers
internal class EventStoreDBRepositoryTest {

    companion object{
        @Container
        var container: GenericContainer<*> = EventStoreContainer()

        lateinit var client: EventStoreDBClient;
        val repositoryConfig = EventStoreDBConfig(500, 500);

        @JvmStatic
        @BeforeAll
        fun setUp() {
            // Now we have an address and port for Redis, no matter where it is running
            client = EventStoreDBClient.create(loadSettings())
        }

        private fun loadSettings() = EventStoreDBConnectionString
            .parseOrThrow("esdb://127.0.0.1:${container.getMappedPort(2113)}?tls=false&tlsVerifyCert=false");
    }

    @Test
    fun `Given the initialise event when persist it then event is stored in the eventStore`() {
        val repository= EventStoreDBRepository(client, repositoryConfig, CounterAggregateRoot::class)
        val counterAggregateId = CounterAggregateId()
        println("counterAggregateId: ${counterAggregateId.value}")
        val aggregate = CounterAggregateRoot.initialiseCounter(counterAggregateId, 5)
        println("aggregateId: ${aggregate.id}")
        runBlocking {
            repository.save(aggregate, UUID.randomUUID(), mapOf())
        }

        val regeneratedAggregate = runBlocking {
            delay(2000L)
            return@runBlocking repository.getById(counterAggregateId)
        }

        assertEquals(counterAggregateId, regeneratedAggregate.id)
        assertEquals(5, regeneratedAggregate.counter)
    }

    @Test
    fun `Given an existing aggregate when persist a new event then the new event is stored in the eventStore`() {
        val repository= EventStoreDBRepository(client, repositoryConfig, CounterAggregateRoot::class)
        val counterAggregateId = CounterAggregateId()

        val initialAggregate =runBlocking {
            val aggregate = CounterAggregateRoot.initialiseCounter(counterAggregateId, 5)
            repository.save(aggregate, UUID.randomUUID(), mapOf())
            delay(2000L)
            return@runBlocking repository.getById(counterAggregateId)
        }

        val regeneratedAggregate = runBlocking {
            val updatedAggregate = initialAggregate.increaseCounter(7)
            repository.save(updatedAggregate, UUID.randomUUID(), mapOf())
            delay(2000L)
            return@runBlocking repository.getById(counterAggregateId)
        }

        assertEquals(counterAggregateId, regeneratedAggregate.id)
        assertEquals(12, regeneratedAggregate.counter)
    }



}

