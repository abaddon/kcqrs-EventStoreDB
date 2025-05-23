package io.github.abaddon.kcqrs.eventstoredb.config

import io.kurrent.dbclient.Position
import io.kurrent.dbclient.SubscribeToAllOptions
import io.kurrent.dbclient.SubscriptionFilter

data class SubscriptionFilterConfig(private val type: String, private val value: String) {

    companion object {
        const val SUBSCRIPTION_FILTER_EVENT_TYPE_PREFIX = "eventTypePrefix"
        const val SUBSCRIPTION_FILTER_EVENT_TYPE_REGEX = "eventTypeRegEx"
        const val SUBSCRIPTION_FILTER_STREAM_NAME_PREFIX = "streamNamePrefix"
        const val SUBSCRIPTION_FILTER_STREAM_NAME_REGEX = "streamNameRegEx"
    }

    fun subscribeToAllOptions(position: Position): SubscribeToAllOptions =
        SubscribeToAllOptions.get().filter(subscriptionFilterBuilder())
            .fromPosition(position)

    private fun subscriptionFilterBuilder(): SubscriptionFilter {
        return SubscriptionFilter.newBuilder()
            .let { builder ->
                when (type) {
                    SUBSCRIPTION_FILTER_EVENT_TYPE_PREFIX -> builder.addEventTypePrefix(value)
                    SUBSCRIPTION_FILTER_EVENT_TYPE_REGEX -> builder.withEventTypeRegularExpression(value)
                    SUBSCRIPTION_FILTER_STREAM_NAME_PREFIX -> builder.addStreamNamePrefix(value)
                    SUBSCRIPTION_FILTER_STREAM_NAME_REGEX -> builder.withStreamNameRegularExpression(value)
                    else -> builder
                }
            }.build()
    }

}