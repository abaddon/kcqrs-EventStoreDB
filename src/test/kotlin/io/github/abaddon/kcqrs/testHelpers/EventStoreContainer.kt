package io.github.abaddon.kcqrs.testHelpers

import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy
import java.time.Duration
import java.time.temporal.ChronoUnit.HOURS


class EventStoreContainer : GenericContainer<EventStoreContainer>(DOCKER_IMAGE) {
    companion object {
        const val DOCKER_IMAGE: String = "eventstore/eventstore:21.6.0-buster-slim"
    }

    init {
        withExposedPorts(1113, 2113)
            .withEnv(
                mapOf(
                    Pair("EVENTSTORE_CLUSTER_SIZE", "1"),
                    Pair("EVENTSTORE_RUN_PROJECTIONS", "All"),
                    Pair("EVENTSTORE_START_STANDARD_PROJECTIONS", "true"),
                    Pair("EVENTSTORE_EXT_TCP_PORT", "1113"),
                    Pair("EVENTSTORE_HTTP_PORT", "2113"),
                    Pair("EVENTSTORE_INSECURE", "true"),
                    Pair("EVENTSTORE_ENABLE_EXTERNAL_TCP", "false"),
                    Pair("EVENTSTORE_ENABLE_ATOM_PUB_OVER_HTTP", "false"),
                )
            )
            .waitingFor(
                LogMessageWaitStrategy()
                    .withRegEx(".*SPARTA.*")
                    .withStartupTimeout(Duration.of(5L, HOURS))
            )
        //Finished Starting Projection Manager Response Reader
    }


}