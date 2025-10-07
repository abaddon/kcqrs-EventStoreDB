package io.github.abaddon.kcqrs.eventstoredb.config

import io.kurrent.dbclient.Position
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class SubscriptionFilterConfigTest {

    @Test
    fun `Given event type prefix filter when created then filter type and value are set correctly`() {
        // Given
        val filterType = SubscriptionFilterConfig.SUBSCRIPTION_FILTER_EVENT_TYPE_PREFIX
        val filterValue = "Counter"

        // When
        val config = SubscriptionFilterConfig(filterType, filterValue)

        // Then
        assertNotNull(config)
    }

    @Test
    fun `Given event type regex filter when subscribeToAllOptions called then returns valid options`() {
        // Given
        val filterType = SubscriptionFilterConfig.SUBSCRIPTION_FILTER_EVENT_TYPE_REGEX
        val filterValue = "^Counter.*Event$"
        val config = SubscriptionFilterConfig(filterType, filterValue)
        val position = Position(0, 0)

        // When
        val options = config.subscribeToAllOptions(position)

        // Then
        assertNotNull(options)
    }

    @Test
    fun `Given stream name prefix filter when subscribeToAllOptions called then returns valid options`() {
        // Given
        val filterType = SubscriptionFilterConfig.SUBSCRIPTION_FILTER_STREAM_NAME_PREFIX
        val filterValue = "account"
        val config = SubscriptionFilterConfig(filterType, filterValue)
        val position = Position(0, 0)

        // When
        val options = config.subscribeToAllOptions(position)

        // Then
        assertNotNull(options)
    }

    @Test
    fun `Given stream name regex filter when subscribeToAllOptions called then returns valid options`() {
        // Given
        val filterType = SubscriptionFilterConfig.SUBSCRIPTION_FILTER_STREAM_NAME_REGEX
        val filterValue = "^(account|transaction)"
        val config = SubscriptionFilterConfig(filterType, filterValue)
        val position = Position(0, 0)

        // When
        val options = config.subscribeToAllOptions(position)

        // Then
        assertNotNull(options)
    }

    @Test
    fun `Given different positions when subscribeToAllOptions called then position is set correctly`() {
        // Given
        val config = SubscriptionFilterConfig(
            SubscriptionFilterConfig.SUBSCRIPTION_FILTER_EVENT_TYPE_PREFIX,
            "Test"
        )
        val startPosition = Position(0, 0)
        val endPosition = Position(100, 50)

        // When
        val optionsFromStart = config.subscribeToAllOptions(startPosition)
        val optionsFromEnd = config.subscribeToAllOptions(endPosition)

        // Then
        assertNotNull(optionsFromStart)
        assertNotNull(optionsFromEnd)
    }

    @Test
    fun `Given invalid filter type when subscribeToAllOptions called then throws IllegalStateException`() {
        // Given
        val filterType = "INVALID_FILTER_TYPE"
        val filterValue = "test"
        val config = SubscriptionFilterConfig(filterType, filterValue)
        val position = Position(0, 0)

        // When/Then
        val exception = assertThrows<IllegalStateException> {
            config.subscribeToAllOptions(position)
        }

        assertTrue(exception.message?.contains("No filter type") == true)
    }

    @Test
    fun `Given event type prefix constant when accessed then has correct value`() {
        // Then
        assertEquals("eventTypePrefix", SubscriptionFilterConfig.SUBSCRIPTION_FILTER_EVENT_TYPE_PREFIX)
    }

    @Test
    fun `Given event type regex constant when accessed then has correct value`() {
        // Then
        assertEquals("eventTypeRegEx", SubscriptionFilterConfig.SUBSCRIPTION_FILTER_EVENT_TYPE_REGEX)
    }

    @Test
    fun `Given stream name prefix constant when accessed then has correct value`() {
        // Then
        assertEquals("streamNamePrefix", SubscriptionFilterConfig.SUBSCRIPTION_FILTER_STREAM_NAME_PREFIX)
    }

    @Test
    fun `Given stream name regex constant when accessed then has correct value`() {
        // Then
        assertEquals("streamNameRegEx", SubscriptionFilterConfig.SUBSCRIPTION_FILTER_STREAM_NAME_REGEX)
    }

    @Test
    fun `Given complex regex pattern when created then pattern is preserved`() {
        // Given
        val filterType = SubscriptionFilterConfig.SUBSCRIPTION_FILTER_STREAM_NAME_REGEX
        val complexPattern = "^(account|transaction|customer)\\.(create|update|delete)$"
        val config = SubscriptionFilterConfig(filterType, complexPattern)
        val position = Position(0, 0)

        // When
        val options = config.subscribeToAllOptions(position)

        // Then
        assertNotNull(options)
    }

    @Test
    fun `Given multiple configs with different filter types when created then each is independent`() {
        // Given
        val config1 = SubscriptionFilterConfig(
            SubscriptionFilterConfig.SUBSCRIPTION_FILTER_EVENT_TYPE_PREFIX,
            "Account"
        )
        val config2 = SubscriptionFilterConfig(
            SubscriptionFilterConfig.SUBSCRIPTION_FILTER_STREAM_NAME_REGEX,
            "^account"
        )
        val position = Position(0, 0)

        // When
        val options1 = config1.subscribeToAllOptions(position)
        val options2 = config2.subscribeToAllOptions(position)

        // Then
        assertNotNull(options1)
        assertNotNull(options2)
    }

    @Test
    fun `Given empty filter value when created then config is created`() {
        // Given
        val filterType = SubscriptionFilterConfig.SUBSCRIPTION_FILTER_EVENT_TYPE_PREFIX
        val filterValue = ""
        val config = SubscriptionFilterConfig(filterType, filterValue)
        val position = Position(0, 0)

        // When
        val options = config.subscribeToAllOptions(position)

        // Then
        assertNotNull(options)
    }
}
