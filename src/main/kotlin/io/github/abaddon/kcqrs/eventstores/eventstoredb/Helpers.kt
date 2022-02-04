package io.github.abaddon.kcqrs.eventstores.eventstoredb

import com.eventstore.dbclient.EventData
import com.eventstore.dbclient.ResolvedEvent
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
        .configure(KotlinFeature.SingletonSupport,false)
        .configure(KotlinFeature.StrictNullChecks, false)
        .build()
)

fun Iterable<ResolvedEvent>.toDomainEvents(): Iterable<IDomainEvent> {
    return this.map {  resolvedEvent -> resolvedEvent.originalEvent.toDomainEvent()  }
}

fun com.eventstore.dbclient.RecordedEvent.toDomainEvent(): IDomainEvent{
    val eventTypeName = this.eventType

    val eventClass = Class.forName(eventTypeName)


    val eventDataJson: String=this.eventData.decodeToString()
    val eventMetaJson: String=this.userMetadata.decodeToString() //TODO headers not managed

    return mapper.readValue(eventDataJson,eventClass) as IDomainEvent
}

inline fun <reified T: IDomainEvent>T.toEventData(header: Map<String,String>): EventData {
    val eventId = this.messageId;
    val eventType = this::class.qualifiedName!!
    //val
    val eventJson = mapper.writeValueAsString(this)//Json.encodeToString<T>(this) //
    val headerJson = mapper.writeValueAsString(header)//Json.encodeToString(header) //
    return EventData(eventId,eventType,"application/json",eventJson.encodeToByteArray(), headerJson.encodeToByteArray())
}