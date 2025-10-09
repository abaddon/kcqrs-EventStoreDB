package io.github.abaddon.kcqrs.testHelpers

import io.github.abaddon.kcqrs.core.helpers.KcqrsLoggerFactory.log
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy
import java.time.Duration
import java.time.temporal.ChronoUnit


class EventStoreContainer : GenericContainer<EventStoreContainer>(DOCKER_IMAGE) {
    companion object {
        const val DOCKER_IMAGE: String = "docker.kurrent.io/kurrent-latest/kurrentdb:latest"
    }

    init {
        log.info("Starting EventStoreDB container...")
        withExposedPorts(1113, 2113)
            .withEnv(
                mapOf(
                    Pair("KURRENTDB_CLUSTER_SIZE", "1"),
                    Pair("KURRENTDB_RUN_PROJECTIONS", "All"),
                    Pair("KURRENTDB_START_STANDARD_PROJECTIONS", "true"),
                    Pair("KURRENTDB_NODE_PORT", "2113"),
                    Pair("KURRENTDB_INSECURE", "true"),
                    Pair("KURRENTDB_ENABLE_ATOM_PUB_OVER_HTTP", "true")
                )
            )
            .waitingFor(
                LogMessageWaitStrategy()
                    .withRegEx(".*SPARTA.*")
                    .withStartupTimeout(Duration.of(10L, ChronoUnit.MINUTES))
            )
        log.info("EventStoreDB container started")
    }


}