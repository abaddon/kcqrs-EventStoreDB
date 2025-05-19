package io.github.abaddon.kcqrs.eventstoredb.eventstore

import io.kurrent.dbclient.EventData
import io.kurrent.dbclient.ResolvedEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.github.abaddon.kcqrs.core.domain.messages.events.IDomainEvent

val mapper = ObjectMapper().registerModule(
    KotlinModule.Builder()
        .withReflectionCacheSize(512)
        .configure(KotlinFeature.NullToEmptyCollection, false)
        .configure(KotlinFeature.NullToEmptyMap, false)
        .configure(KotlinFeature.NullIsSameAsDefault, false)
        .configure(KotlinFeature.SingletonSupport, false)
        .configure(KotlinFeature.StrictNullChecks, false)
        .build()
)

fun Iterable<ResolvedEvent>.toDomainEvents(): Iterable<IDomainEvent> {
    return this.mapNotNull { resolvedEvent -> resolvedEvent.event?.toDomainEvent() }
}

fun io.kurrent.dbclient.RecordedEvent.toDomainEvent(): IDomainEvent {
    val eventTypeName = this.eventType
    val eventClass = Class.forName(eventTypeName)
    val eventDataJson: String = this.eventData.decodeToString()
    val eventMetaJson: String = this.userMetadata.decodeToString() //TODO headers not managed

    return mapper.readValue(eventDataJson, eventClass) as IDomainEvent
}

inline fun <reified T : IDomainEvent> T.toEventData(header: Map<String, String>): EventData {
    val eventId = this.messageId
    val eventType = this::class.qualifiedName!!
    val eventJson = mapper.writeValueAsString(this)
    val headerJson = mapper.writeValueAsString(header)
    
    // EventData construction hasn't changed in 4.x
    return EventData.builderAsJson(
        eventId,
        eventType,
        eventJson.encodeToByteArray()
    ).metadataAsBytes(headerJson.encodeToByteArray()).build()
}