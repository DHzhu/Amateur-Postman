package com.github.dhzhu.amateurpostman.services

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import java.util.concurrent.TimeUnit
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Phase 0 infrastructure smoke test. Verifies that gRPC ManagedChannel can be instantiated and torn
 * down cleanly, confirming no dependency conflict with the IntelliJ platform classpath.
 */
class GrpcInfrastructureTest {

    @Test
    fun `managed channel can be created and shut down`() {
        val channel: ManagedChannel =
                ManagedChannelBuilder.forAddress("localhost", 50051).usePlaintext().build()

        assertFalse(
                channel.isShutdown,
                "Channel should not be shut down immediately after creation"
        )

        channel.shutdownNow()
        val terminated = channel.awaitTermination(5, TimeUnit.SECONDS)
        assertTrue(terminated, "Channel should terminate within 5 seconds")
    }

    @Test
    fun `in-process server and channel can be created`() {
        val serverName = InProcessServerBuilder.generateName()

        // Start a bare in-process server (no services registered – just verifies infra)
        val server = InProcessServerBuilder.forName(serverName).directExecutor().build().start()

        val channel: ManagedChannel =
                InProcessChannelBuilder.forName(serverName).directExecutor().build()

        assertFalse(channel.isShutdown, "InProcess channel should be alive")

        channel.shutdownNow()
        channel.awaitTermination(5, TimeUnit.SECONDS)
        server.shutdownNow()
        server.awaitTermination(5, TimeUnit.SECONDS)

        assertTrue(channel.isTerminated, "InProcess channel should be terminated")
        assertTrue(server.isTerminated, "InProcess server should be terminated")
    }
}
