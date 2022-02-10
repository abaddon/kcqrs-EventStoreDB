package io.github.abaddon.kcqrs.testHelpers.projections

import io.github.abaddon.kcqrs.core.projections.IProjectionKey

data class DummyProjectionKey(val key: String) : IProjectionKey {
    override fun key(): String = key
}