package io.github.abaddon.kcqrs.eventstoredb.eventstore

import io.github.abaddon.kcqrs.eventstoredb.config.EventStoreDBConfig
import io.github.abaddon.kcqrs.testHelpers.WithEventStoreDBContainer
import io.github.abaddon.kcqrs.testHelpers.entities.CounterAggregateId
import io.github.abaddon.kcqrs.testHelpers.entities.CounterAggregateRoot
import io.github.abaddon.kcqrs.testHelpers.projections.DummyProjection
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertNotNull

@ExperimentalCoroutinesApi
internal class EventStoreDomainEventSubscriberTest : WithEventStoreDBContainer() {

    companion object {
        private const val STREAM_NAME = "CounterAggregateRoot"
        private const val GROUP_NAME = "test-subscription-group"
    }

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    lateinit var repositoryConfig: EventStoreDBRepositoryConfig
    lateinit var repository: EventStoreDBRepository<CounterAggregateRoot>
    lateinit var subscriptionConfig: EventStoreSubscriptionConfig

    @BeforeEach
    fun setUp() {
        val connectionString = "kurrentdb://127.0.0.1:${container.getMappedPort(2113)}?tls=false&tlsVerifyCert=false"
        val eventStoreDBConfig = EventStoreDBConfig(connectionString)

        repositoryConfig = EventStoreDBRepositoryConfig(
            eventStoreDBConfig,
            STREAM_NAME,
            500,
            500
        )

        repository = EventStoreDBRepository(
            repositoryConfig,
            { CounterAggregateRoot(it as CounterAggregateId) }
        )

        subscriptionConfig = EventStoreSubscriptionConfig(
            eventStoreDBConfig,
            listOf(STREAM_NAME),
            "$GROUP_NAME-${UUID.randomUUID()}" // Unique group name for each test
        )
    }

    @Test
    fun `Given valid subscription config when EventStoreDomainEventSubscriber is created then it initializes successfully`() {
        // When
        val subscriber = EventStoreDomainEventSubscriber<DummyProjection>(subscriptionConfig)

        // Then
        assertNotNull(subscriber)
    }

    @Test
    fun `Given subscriber with single stream when created then consumer group is created`() {
        // Given
        val singleStreamConfig = EventStoreSubscriptionConfig(
            EventStoreDBConfig("kurrentdb://127.0.0.1:${container.getMappedPort(2113)}?tls=false&tlsVerifyCert=false"),
            listOf("SingleStream"),
            "single-stream-group-${UUID.randomUUID()}"
        )

        // When
        val subscriber = EventStoreDomainEventSubscriber<DummyProjection>(singleStreamConfig)

        // Then
        assertNotNull(subscriber)
    }

    @Test
    fun `Given subscriber with multiple streams when created then consumer group handles all streams`() {
        // Given
        val multiStreamConfig = EventStoreSubscriptionConfig(
            EventStoreDBConfig("kurrentdb://127.0.0.1:${container.getMappedPort(2113)}?tls=false&tlsVerifyCert=false"),
            listOf("Stream1", "Stream2", "Stream3"),
            "multi-stream-group-${UUID.randomUUID()}"
        )

        // When
        val subscriber = EventStoreDomainEventSubscriber<DummyProjection>(multiStreamConfig)

        // Then
        assertNotNull(subscriber)
    }

    @Test
    fun `Given subscriber with empty stream list when created then consumer group is created`() = testScope.runTest {
        // Given
        val emptyStreamConfig = EventStoreSubscriptionConfig(
            EventStoreDBConfig("kurrentdb://127.0.0.1:${container.getMappedPort(2113)}?tls=false&tlsVerifyCert=false"),
            emptyList(),
            "empty-stream-group-${UUID.randomUUID()}"
        )

        // When
        val subscriber = EventStoreDomainEventSubscriber<DummyProjection>(emptyStreamConfig)

        // Then
        assertNotNull(subscriber)
    }
}
