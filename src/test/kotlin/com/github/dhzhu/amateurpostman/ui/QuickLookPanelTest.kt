package com.github.dhzhu.amateurpostman.ui

import com.github.dhzhu.amateurpostman.services.EnvironmentService
import com.github.dhzhu.amateurpostman.models.*
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class QuickLookPanelTest : BasePlatformTestCase() {

    private lateinit var environmentService: EnvironmentService
    private lateinit var quickLookPanel: QuickLookPanel

    override fun setUp() {
        super.setUp()
        environmentService = EnvironmentService(project)
        quickLookPanel = QuickLookPanel(project)
    }

    override fun tearDown() {
        environmentService = null!!
        quickLookPanel = null!!
        super.tearDown()
    }

    @Test
    fun testQuickLookPanelCreation() {
        // Test that the panel can be created without errors
        assertNotNull(quickLookPanel)
    }

    @Test
    fun testVariablesWithSourcesDisplay() {
        // Create a test environment
        val testEnv = environmentService.createEnvironment("Test Environment")

        // Set global variable
        environmentService.setGlobalVariable(Variable(key = "test_var", value = "global_value"))

        // Set collection variable
        environmentService.setCollectionVariable("test_collection", Variable(key = "test_var", value = "collection_value"))

        // Set environment variable
        environmentService.addVariable(testEnv.id, Variable(key = "test_var", value = "environment_value"))

        // Set current environment
        environmentService.setCurrentEnvironment(testEnv.id)

        // Verify variable resolution with sources
        val result = environmentService.getAllVariablesWithSource("test_collection")

        // Check that we have the expected variable in the result
        assertTrue(result.allVariables.containsKey("test_var"))
        val varWithSource = result.allVariables["test_var"]!!

        // Check that the final value is from the highest priority (environment)
        assertTrue(varWithSource.finalValue == "environment_value")

        // Check that the variable is marked as shadowed since it exists in multiple scopes
        assertTrue(varWithSource.isShadowed)

        // Verify all scopes are populated correctly
        assertTrue(result.globalVariables.isNotEmpty())
        assertTrue(result.collectionVariables.isNotEmpty())
        assertTrue(result.environmentVariables.isNotEmpty())
    }

    @Test
    fun testTemporaryVariableHandling() {
        // Create a test environment
        val testEnv = environmentService.createEnvironment("Temp Test Environment")

        // Set a base variable
        environmentService.setGlobalVariable(Variable(key = "temp_test", value = "base_value"))
        environmentService.setCurrentEnvironment(testEnv.id)

        // Add a temporary variable
        environmentService.setTemporaryVariable("temp_test", "temp_value")
        environmentService.setTemporaryVariable("new_temp", "new_value")

        // Check resolution with temporary variables
        val resultWithTemp = environmentService.getAllVariablesWithSource(null, includeTemporary = true)
        val resultWithoutTemp = environmentService.getAllVariablesWithSource(null, includeTemporary = false)

        // With temporary variables, the temp value should take precedence
        val varWithTemp = resultWithTemp.allVariables["temp_test"]
        assertNotNull(varWithTemp)
        assertTrue(varWithTemp!!.finalValue == "temp_value")

        // Without temporary variables, the base value should be used
        val varWithoutTemp = resultWithoutTemp.allVariables["temp_test"]
        assertNotNull(varWithoutTemp)
        assertTrue(varWithoutTemp!!.finalValue == "base_value")

        // Temporary variable should be in temporary list
        assertTrue(resultWithTemp.temporaryVariables.size == 2) // temp_test and new_temp
        val tempVar = resultWithTemp.temporaryVariables.firstOrNull { it.key == "new_temp" }
        assertNotNull(tempVar)
        assertTrue(tempVar!!.value == "new_value")
    }

    @Test
    fun testQuickLookPanelVariableResolution() {
        // Set up various variable scopes
        val testEnv = environmentService.createEnvironment("Resolution Test")
        environmentService.setGlobalVariable(Variable(key = "shared_key", value = "global_val"))
        environmentService.setCollectionVariable("test_collection", Variable(key = "shared_key", value = "collection_val"))
        environmentService.addVariable(testEnv.id, Variable(key = "shared_key", value = "environment_val"))
        environmentService.setTemporaryVariable("shared_key", "temporary_val")
        environmentService.setCurrentEnvironment(testEnv.id)

        // Verify the resolution hierarchy is respected
        val result = environmentService.getAllVariablesWithSource("test_collection", includeTemporary = true)

        // The final value should be from temporary since it has the highest priority
        val resolvedVar = result.allVariables["shared_key"]
        assertNotNull(resolvedVar)
        assertTrue(resolvedVar!!.finalValue == "temporary_val")
        assertTrue(resolvedVar.scope == VariableScope.TEMPORARY)

        // Other scope variables should be marked as shadowed
        val globalVar = result.globalVariables.firstOrNull { it.key == "shared_key" }
        assertNotNull(globalVar)
        assertTrue(globalVar!!.isShadowed)

        val collectionVar = result.collectionVariables.firstOrNull { it.key == "shared_key" }
        assertNotNull(collectionVar)
        assertTrue(collectionVar!!.isShadowed)

        val envVar = result.environmentVariables.firstOrNull { it.key == "shared_key" }
        assertNotNull(envVar)
        assertTrue(envVar!!.isShadowed)
    }
}