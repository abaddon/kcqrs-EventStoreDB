package io.github.abaddon.kcqrs.eventstores.eventstoredb.helpers.entities

import io.github.abaddon.kcqrs.core.IIdentity
import java.util.*

data class CounterAggregateId(val value: UUID) : IIdentity {

    constructor (): this(UUID.randomUUID())

    override fun valueAsString(): String {
        return value.toString()
    }
}