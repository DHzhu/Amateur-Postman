package com.github.dhzhu.amateurpostman.services

import com.github.dhzhu.amateurpostman.models.Variable
import com.intellij.openapi.project.Project
import kotlinx.coroutines.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock

/**
 * Concurrency tests for EnvironmentService and OAuth2Service.
 * Verifies that synchronized state access prevents lost updates (Issue 5.5.3).
 */
class EnvironmentServiceConcurrencyTest {

    private lateinit var envService: EnvironmentService

    @BeforeEach
    fun setUp() {
        envService = EnvironmentService(mock<Project>())
    }

    // ========== EnvironmentService Concurrency ==========

    @Test
    fun `test concurrent createEnvironment does not lose updates`() = runBlocking {
        val numConcurrent = 20

        val jobs = (1..numConcurrent).map { i ->
            async(Dispatchers.Default) {
                envService.createEnvironment("Env_$i")
            }
        }

        val created = jobs.awaitAll()

        // All environments should be created
        assertEquals(numConcurrent, created.size, "All environments should be created")

        val allEnvs = envService.getEnvironments()
        assertEquals(numConcurrent, allEnvs.size, "No environments should be lost")
    }

    @Test
    fun `test concurrent setGlobalVariable does not lose updates`() = runBlocking {
        val numConcurrent = 20

        // First create an environment so we have a place for globals
        envService.createEnvironment("Default")

        val jobs = (1..numConcurrent).map { i ->
            async(Dispatchers.Default) {
                envService.setGlobalVariable(Variable(key = "var_$i", value = "value_$i"))
            }
        }

        jobs.awaitAll()

        val globals = envService.getGlobalVariables()
        assertEquals(numConcurrent, globals.size, "No global variables should be lost")
    }

    @Test
    fun `test concurrent setCollectionVariable does not lose updates`() = runBlocking {
        val numConcurrent = 20
        val collectionId = "test-collection"

        val jobs = (1..numConcurrent).map { i ->
            async(Dispatchers.Default) {
                envService.setCollectionVariable(
                    collectionId,
                    Variable(key = "coll_var_$i", value = "coll_value_$i")
                )
            }
        }

        jobs.awaitAll()

        val collVars = envService.getCollectionVariables(collectionId)
        assertEquals(numConcurrent, collVars.variables.size, "No collection variables should be lost")
    }

    @Test
    fun `test concurrent mixed read write does not corrupt state`() = runBlocking {
        // Create some initial state
        envService.createEnvironment("Initial")
        envService.setGlobalVariable(Variable(key = "init", value = "0"))

        val numConcurrent = 30

        val jobs = (1..numConcurrent).map { i ->
            async(Dispatchers.Default) {
                when (i % 3) {
                    0 -> envService.createEnvironment("Concurrent_$i")
                    1 -> envService.setGlobalVariable(Variable(key = "g_$i", value = "v_$i"))
                    2 -> envService.getEnvironments() // Read-only
                }
            }
        }

        jobs.awaitAll()

        // State should be consistent — at least the initial environment should exist
        val envs = envService.getEnvironments()
        assertTrue(envs.any { it.name == "Initial" }, "Initial environment should still exist")
    }

    // ========== OAuth2Service Concurrency ==========

    @Test
    fun `test concurrent OAuth2 createConfig does not lose updates`() = runBlocking {
        val oauthService = OAuth2Service(mock<Project>())
        val numConcurrent = 20

        val jobs = (1..numConcurrent).map { i ->
            async(Dispatchers.Default) {
                oauthService.createConfig(
                    "Config_$i",
                    com.github.dhzhu.amateurpostman.models.OAuth2Config(
                        grantType = com.github.dhzhu.amateurpostman.models.OAuth2GrantType.CLIENT_CREDENTIALS,
                        tokenUrl = "https://auth.example.com/token",
                        clientId = "client_$i",
                        clientSecret = "secret_$i"
                    )
                )
            }
        }

        val created = jobs.awaitAll()

        assertEquals(numConcurrent, created.size, "All configs should be created")
        val allConfigs = oauthService.getAllConfigs()
        assertEquals(numConcurrent, allConfigs.size, "No configs should be lost")
    }

    @Test
    fun `test concurrent OAuth2 setRequestAuthConfig does not lose updates`() = runBlocking {
        val oauthService = OAuth2Service(mock<Project>())
        val config = com.github.dhzhu.amateurpostman.models.OAuth2Config(
            grantType = com.github.dhzhu.amateurpostman.models.OAuth2GrantType.CLIENT_CREDENTIALS,
            tokenUrl = "https://auth.example.com/token",
            clientId = "test-client",
            clientSecret = "test-secret"
        )
        val entry = oauthService.createConfig("Test", config)

        val numConcurrent = 20

        val jobs = (1..numConcurrent).map { i ->
            async(Dispatchers.Default) {
                oauthService.setRequestAuthConfig("request_$i", entry.id)
            }
        }

        jobs.awaitAll()

        // Each request should have its own mapping
        (1..numConcurrent).forEach { i ->
            val authConfig = oauthService.getRequestAuthConfig("request_$i")
            assertNotNull(authConfig, "Request $i should have an auth config mapping")
            assertEquals(entry.id, authConfig!!.id)
        }
    }
}
