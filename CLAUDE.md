## Project Overview
This library provides EventStoreDB (KurrentDB) integration for the kcqrs-core framework, implementing the necessary interfaces to persist and consume domain events using EventStoreDB as the event store backend.

### Key Dependencies
- **kcqrs-core**: Core CQRS/Event Sourcing framework that defines the interfaces this library implements
- **kurrentdb-client**: Official EventStoreDB client library for communication with EventStoreDB
- **Jackson**: JSON serialization/deserialization for event data
- **Kotlin** with Coroutines: Primary language and async support
- **JVM**: Java 21 toolchain

## Core Components

### 1. EventStoreDBRepository (src/main/kotlin/io/github/abaddon/kcqrs/eventstoredb/eventstore/EventStoreDBRepository.kt:21)
Implements `EventStoreRepository<TAggregate>` from kcqrs-core for event persistence and retrieval.

**Responsibilities:**
- Persists uncommitted domain events to EventStoreDB streams
- Loads events from streams with pagination support
- Manages aggregate stream naming convention: `{streamName}.{aggregateId}`
- Handles optimistic concurrency control using stream revisions
- Converts between EventStoreDB `ResolvedEvent` and domain `IDomainEvent`

**Key Configuration:**
- `streamName`: Base stream name prefix for aggregates
- `maxReadPageSize`: Maximum events per read operation (default pagination size)
- `maxWritePageSize`: Maximum events per write operation
- Uses `KurrentDBClient` for EventStoreDB communication

**Important Methods:**
- `loadEvents()`: Loads events from a stream with pagination, returns `Flow<IDomainEvent>`
- `persist()`: Saves uncommitted events with optimistic concurrency check
- `aggregateIdStreamName()`: Generates stream name from aggregate ID

### 2. EventStoreDomainEventSubscriber (src/main/kotlin/io/github/abaddon/kcqrs/eventstoredb/eventstore/EventStoreDomainEventSubscriber.kt:14)
Implements `IDomainEventSubscriber<TProjection>` from kcqrs-core for consuming events via persistent subscriptions.

**Responsibilities:**
- Creates and manages EventStoreDB persistent subscription groups
- Subscribes projection handlers to event streams
- Filters events by stream name using regex patterns
- Handles subscription lifecycle (creation, existence checks)

**Key Features:**
- Automatically creates consumer group if it doesn't exist
- Uses `SubscriptionFilter` with stream name regex for filtering
- Supports multiple stream names via regex pattern: `^stream1|^stream2$`
- Only works with `EventStoreProjectionHandler` implementations

**Configuration:**
- `groupName`: Consumer group name for the persistent subscription
- `streamNames`: List of stream name prefixes to subscribe to

### 3. EventStoreProjectionHandler (src/main/kotlin/io/github/abaddon/kcqrs/eventstoredb/projection/EventStoreProjectionHandler.kt:18)
Abstract base class extending `ProjectionHandler<TProjection>` from kcqrs-core for projection event processing.

**Responsibilities:**
- Provides EventStoreDB-specific subscription listeners
- Processes incoming events and transforms them to domain events
- Handles event acknowledgment (ACK) and negative acknowledgment (NACK)
- Manages coroutine context for async event processing

**Key Methods:**
- `getPersistentSubscriptionListener()`: Returns listener for persistent subscriptions with ACK/NACK support
- `getSubscriptionListener()`: Returns listener for catch-up subscriptions
- `processEvent()`: Transforms `ResolvedEvent` to `IDomainEvent` and invokes `onEvent()`

**Error Handling:**
- ACKs successfully processed events
- NACKs failed events with Park action (moves to parked queue)
- Logs unsupported or unexpected errors

### 4. Helper Functions (src/main/kotlin/io/github/abaddon/kcqrs/eventstoredb/eventstore/Helpers.kt)
Provides serialization utilities for converting between EventStoreDB and domain models.

**Key Functions:**
- `RecordedEvent.toDomainEvent()`: Deserializes EventStoreDB event to `IDomainEvent` using class name reflection
- `IDomainEvent.toEventData()`: Serializes domain event to EventStoreDB `EventData` with metadata
- `Iterable<ResolvedEvent>.toDomainEvents()`: Batch conversion of resolved events

**Serialization:**
- Uses Jackson `ObjectMapper` with Kotlin module
- Event type stored as fully qualified class name
- Event data serialized as JSON bytes
- Headers/metadata stored as JSON in user metadata field

## Configuration Classes

### EventStoreDBConfig (src/main/kotlin/io/github/abaddon/kcqrs/eventstoredb/config/EventStoreDBConfig.kt:6)
Parses EventStoreDB connection string and builds `KurrentDBClientSettings`.

### EventStoreDBRepositoryConfig (src/main/kotlin/io/github/abaddon/kcqrs/eventstoredb/eventstore/EventStoreDBRepositoryConfig.kt:6)
Repository-specific configuration including stream name and pagination settings.

### EventStoreSubscriptionConfig (src/main/kotlin/io/github/abaddon/kcqrs/eventstoredb/eventstore/EventStoreSubscriptionConfig.kt:6)
Subscription configuration including stream names filter and consumer group name.

### SubscriptionFilterConfig (src/main/kotlin/io/github/abaddon/kcqrs/eventstoredb/config/SubscriptionFilterConfig.kt:7)
Configures subscription filters for event type or stream name filtering (prefix or regex).

## Integration with kcqrs-core
This library implements three key interfaces from kcqrs-core:
1. **EventStoreRepository**: For aggregate event persistence
2. **IDomainEventSubscriber**: For consuming events via subscriptions
3. **ProjectionHandler**: For updating read models from events

## Stream Naming Convention
- Aggregate streams: `{streamName}.{aggregateId}` (e.g., `account.123`)
- Stream names configured per repository instance
- Subscription filters use regex to match multiple stream prefixes

## Event Serialization Protocol
- Event type: Fully qualified Kotlin class name
- Event data: JSON-serialized event body
- Event ID: Domain event's `messageId`
- Metadata: JSON-serialized headers map
- Uses reflection to deserialize events back to correct class

## Testing Infrastructure
- Uses Testcontainers for EventStoreDB integration tests
- Test helpers include dummy aggregates (Counter), events, and projections
- Located in `src/test/kotlin/io/github/abaddon/kcqrs/testHelpers/`

## Prompts
- always think hard and produce a plan before execute any change
- Always test any change