package io.github.abaddon.kcqrs.eventstoredb.config

import com.eventstore.dbclient.Position
import com.eventstore.dbclient.SubscribeToAllOptions
import com.eventstore.dbclient.SubscriptionFilter

data class SubscriptionFilterConfig(private val type: String, private val value: String) {

    companion object {
        const val SUBSCRIPTION_FILTER_EVENT_TYPE_PREFIX = "eventTypePrefix"
        const val SUBSCRIPTION_FILTER_EVENT_TYPE_REGEX = "eventTypeRegEx"
        const val SUBSCRIPTION_FILTER_STREAM_NAME_PREFIX = "streamNamePrefix"
        const val SUBSCRIPTION_FILTER_STREAM_NAME_REGEX = "streamNameRegEx"
    }

    fun subscribeToAllOptions(position: Position): SubscribeToAllOptions =
        SubscribeToAllOptions.get()
            .let { subscribeToAllOptions ->
                when (val filter = subscriptionFilterBuilder()) {
                    is SubscriptionFilter -> subscribeToAllOptions.filter(filter)
                    else -> subscribeToAllOptions
                }
            }
            .fromPosition(position)

    private fun subscriptionFilterBuilder(): SubscriptionFilter {
        return SubscriptionFilter.newBuilder()
            .let { builder ->
                when (type) {
                    SUBSCRIPTION_FILTER_EVENT_TYPE_PREFIX -> builder.withEventTypePrefix(value)
                    SUBSCRIPTION_FILTER_EVENT_TYPE_REGEX -> builder.withEventTypeRegularExpression(value)
                    SUBSCRIPTION_FILTER_STREAM_NAME_PREFIX -> builder.withStreamNamePrefix(value)
                    SUBSCRIPTION_FILTER_STREAM_NAME_REGEX -> builder.withStreamNameRegularExpression(value)
                    else -> builder
                }
            }.build()
    }

}