package io.github.abaddon.kcqrs.testHelpers.entities

import io.github.abaddon.kcqrs.core.domain.AggregateRoot
import io.github.abaddon.kcqrs.core.domain.messages.events.IDomainEvent
import io.github.abaddon.kcqrs.testHelpers.events.CounterDecreaseEvent
import io.github.abaddon.kcqrs.testHelpers.events.CounterIncreasedEvent
import io.github.abaddon.kcqrs.testHelpers.events.CounterInitialisedEvent
import io.github.abaddon.kcqrs.testHelpers.events.DomainErrorEvent

data class CounterAggregateRoot private constructor(
    override val id: CounterAggregateId,
    override val version: Long,
    val counter: Int,
    override val uncommittedEvents: MutableCollection<IDomainEvent>
) : AggregateRoot() {

    constructor(id: CounterAggregateId) : this(id, 0L, 0, ArrayList<IDomainEvent>())
    //constructor(id: CounterAggregateId) : this(CounterAggregateId(), 0L, 0)

    companion object {

        fun initialiseCounter(id: CounterAggregateId, initialValue: Int): CounterAggregateRoot {
            val emptyAggregate = CounterAggregateRoot(id)
            return try {
                check(initialValue >= 0 && initialValue < Int.MAX_VALUE) { "Value $initialValue not valid, it has to be >= 0 and < ${Int.MAX_VALUE}" }
                emptyAggregate.raiseEvent(CounterInitialisedEvent.create(id, initialValue)) as CounterAggregateRoot
            } catch (e: Exception) {
                emptyAggregate.raiseEvent(DomainErrorEvent.create(id, e)) as CounterAggregateRoot
            }
        }
    }

    fun increaseCounter(incrementValue: Int): CounterAggregateRoot {
        return try {
            check(incrementValue >= 0 && incrementValue < Int.MAX_VALUE) { "Value $incrementValue not valid, it has to be >= 0 and < ${Int.MAX_VALUE}" }
            val updatedCounter = counter + incrementValue
            check(updatedCounter < Int.MAX_VALUE) { "Aggregate value $updatedCounter is not valid, it has to be < ${Int.MAX_VALUE}" }
            raiseEvent(CounterIncreasedEvent.create(id, incrementValue)) as CounterAggregateRoot
        } catch (e: Exception) {
            raiseEvent(DomainErrorEvent.create(id, e)) as CounterAggregateRoot
        }
    }

    fun decreaseCounter(decrementValue: Int): CounterAggregateRoot {
        return try {
            check(decrementValue >= 0 && decrementValue < Int.MAX_VALUE) { "Value $decrementValue not valid, it has to be >= 0 and < ${Int.MAX_VALUE}" }
            val updatedCounter = counter - decrementValue
            check(updatedCounter >= 0) { "Aggregate value $updatedCounter is not valid, it has to be >= 0" }

            raiseEvent(CounterDecreaseEvent.create(id, decrementValue)) as CounterAggregateRoot
        } catch (e: Exception) {
            raiseEvent(DomainErrorEvent.create(id, e)) as CounterAggregateRoot
        }
    }

    private fun apply(event: CounterInitialisedEvent): CounterAggregateRoot {
        return copy(id = event.aggregateId, version = version + 1, counter = event.value)
    }

    private fun apply(event: CounterIncreasedEvent): CounterAggregateRoot {
        val newCounter = counter + event.value
        return copy(counter = newCounter, version = version + 1)
    }

    private fun apply(event: CounterDecreaseEvent): CounterAggregateRoot {
        val newCounter = counter - event.value
        return copy(counter = newCounter, version = version + 1)
    }

    private fun apply(event: DomainErrorEvent): CounterAggregateRoot {
        return copy(version = version + 1)
    }


}
