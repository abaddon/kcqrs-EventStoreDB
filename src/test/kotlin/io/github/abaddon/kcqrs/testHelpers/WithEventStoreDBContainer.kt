package io.github.abaddon.kcqrs.testHelpers

import org.junit.jupiter.api.BeforeAll
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
abstract class WithEventStoreDBContainer {
    companion object {
        @JvmStatic
        var container: GenericContainer<*> = EventStoreContainer()

        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            container.start()
        }
    }
}