package com.github.dhzhu.amateurpostman

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MyBundleTest {

    @Test
    fun testMessageResolvesKeyWithParams() {
        assertEquals("Project service: demo", MyBundle.message("projectService", "demo"))
    }

    @Test
    fun testMessagePointerResolvesLazily() {
        assertEquals("Shuffle", MyBundle.messagePointer("shuffle").get())
    }
}
