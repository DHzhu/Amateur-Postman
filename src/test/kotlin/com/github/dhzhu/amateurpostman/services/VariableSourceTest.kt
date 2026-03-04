package com.github.dhzhu.amateurpostman.services

import com.github.dhzhu.amateurpostman.models.*
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class VariableSourceTest : BasePlatformTestCase() {

    private lateinit var environmentService: EnvironmentService

    override fun setUp() {
        super.setUp()
        environmentService = EnvironmentService(project)
    }

    override fun tearDown() {
        environmentService = null!!
        super.tearDown()
    }

    @Test
    fun testVariableSourceResolution() {
        // 创建一个测试环境
        val testEnv = environmentService.createEnvironment("Test Environment")

        // 设置全局变量
        environmentService.setGlobalVariable(Variable(key = "api_url", value = "https://global.example.com"))
        environmentService.setGlobalVariable(Variable(key = "timeout", value = "5000"))
        environmentService.setGlobalVariable(Variable(key = "debug", value = "false"))

        // 设置集合变量 (假设集合ID为 "test_collection")
        environmentService.setCollectionVariable("test_collection", Variable(key = "api_url", value = "https://collection.example.com"))
        environmentService.setCollectionVariable("test_collection", Variable(key = "timeout", value = "10000"))

        // 设置环境变量 (具有最高优先级)
        environmentService.addVariable(testEnv.id, Variable(key = "api_url", value = "https://environment.example.com"))
        environmentService.addVariable(testEnv.id, Variable(key = "debug", value = "true"))

        // 设置当前环境
        environmentService.setCurrentEnvironment(testEnv.id)

        // 获取带来源的变量解析结果
        val result = environmentService.getAllVariablesWithSource("test_collection")

        // 验证 api_url 变量的解析 (应该取环境变量的值)
        val apiUrlVar = result.allVariables["api_url"]
        assertNotNull(apiUrlVar)
        assertEquals("https://environment.example.com", apiUrlVar!!.finalValue)
        assertEquals(VariableScope.ENVIRONMENT, apiUrlVar.scope)
        assertTrue(apiUrlVar.isShadowed) // 全局和集合变量都被它覆盖了

        // 验证 timeout 变量的解析 (应该取集合变量的值，因为它被环境变量覆盖)
        val timeoutVar = result.allVariables["timeout"]
        assertNotNull(timeoutVar)
        assertEquals("10000", timeoutVar!!.finalValue)
        assertEquals(VariableScope.COLLECTION, timeoutVar.scope)
        assertTrue(timeoutVar.isShadowed) // 被环境变量覆盖，但环境变量没有这个键

        // 验证 debug 变量的解析 (应该取环境变量的值)
        val debugVar = result.allVariables["debug"]
        assertNotNull(debugVar)
        assertEquals("true", debugVar!!.finalValue)
        assertEquals(VariableScope.ENVIRONMENT, debugVar.scope)
        assertFalse(debugVar.isShadowed) // 没有其他作用域有这个变量

        // 验证分组统计
        assertEquals(3, result.globalVariables.size) // 全局有3个变量
        assertEquals(2, result.collectionVariables.size) // 集合有2个变量
        assertEquals(2, result.environmentVariables.size) // 环境有2个变量
    }

    @Test
    fun testVariablePriorityOrder() {
        // 创建一个测试环境
        val testEnv = environmentService.createEnvironment("Priority Test")

        // 同一个键在多个作用域设置不同的值
        environmentService.setGlobalVariable(Variable(key = "common_key", value = "global_value"))
        environmentService.setCollectionVariable("priority_collection", Variable(key = "common_key", value = "collection_value"))
        environmentService.addVariable(testEnv.id, Variable(key = "common_key", value = "environment_value"))

        // 设置当前环境
        environmentService.setCurrentEnvironment(testEnv.id)

        // 获取带来源的变量解析结果
        val result = environmentService.getAllVariablesWithSource("priority_collection")

        // 验证最终值应该是环境变量的值（最高优先级）
        val commonKeyVar = result.allVariables["common_key"]
        assertNotNull(commonKeyVar)
        assertEquals("environment_value", commonKeyVar!!.finalValue)
        assertEquals(VariableScope.ENVIRONMENT, commonKeyVar.scope)
        assertTrue(commonKeyVar.isShadowed) // 全局和集合变量都被覆盖

        // 验证全局变量被标记为已遮蔽
        val globalCommonKeyVar = result.globalVariables.firstOrNull { it.key == "common_key" }
        assertNotNull(globalCommonKeyVar)
        assertEquals("global_value", globalCommonKeyVar!!.value)
        assertTrue(globalCommonKeyVar.isShadowed)
    }

    @Test
    fun testNoConflictsWhenDifferentKeys() {
        // 创建一个测试环境
        val testEnv = environmentService.createEnvironment("No Conflict Test")

        // 不同键的变量（无冲突）
        environmentService.setGlobalVariable(Variable(key = "global_var", value = "global_val"))
        environmentService.setCollectionVariable("no_conflict_collection", Variable(key = "collection_var", value = "collection_val"))
        environmentService.addVariable(testEnv.id, Variable(key = "environment_var", value = "environment_val"))

        // 设置当前环境
        environmentService.setCurrentEnvironment(testEnv.id)

        // 获取带来源的变量解析结果
        val result = environmentService.getAllVariablesWithSource("no_conflict_collection")

        // 验证所有变量都没有冲突且未被遮蔽
        val globalVar = result.allVariables["global_var"]
        assertNotNull(globalVar)
        assertFalse(globalVar!!.isShadowed)
        assertEquals(VariableScope.GLOBAL, globalVar.scope)

        val collectionVar = result.allVariables["collection_var"]
        assertNotNull(collectionVar)
        assertFalse(collectionVar!!.isShadowed)
        assertEquals(VariableScope.COLLECTION, collectionVar.scope)

        val environmentVar = result.allVariables["environment_var"]
        assertNotNull(environmentVar)
        assertFalse(environmentVar!!.isShadowed)
        assertEquals(VariableScope.ENVIRONMENT, environmentVar.scope)
    }

    @Test
    fun testTemporaryVariablesFromScripts() {
        // 创建一个测试环境
        val testEnv = environmentService.createEnvironment("Script Test")

        // 设置全局变量
        environmentService.setGlobalVariable(Variable(key = "dynamic_value", value = "global_default"))

        // 设置集合变量
        environmentService.setCollectionVariable("script_collection", Variable(key = "dynamic_value", value = "collection_default"))

        // 设置环境变量
        environmentService.addVariable(testEnv.id, Variable(key = "dynamic_value", value = "environment_default"))

        // 设置当前环境
        environmentService.setCurrentEnvironment(testEnv.id)

        // 模拟从脚本中设置临时变量
        environmentService.setTemporaryVariable("dynamic_value", "script_value")
        environmentService.setTemporaryVariable("new_from_script", "new_value")

        // 获取带来源的变量解析结果，包括临时变量
        val result = environmentService.getAllVariablesWithSource("script_collection", includeTemporary = true)

        // 验证临时变量的优先级最高
        val dynamicValueVar = result.allVariables["dynamic_value"]
        assertNotNull(dynamicValueVar)
        assertEquals("script_value", dynamicValueVar!!.finalValue)
        assertEquals(VariableScope.TEMPORARY, dynamicValueVar.scope)
        assertFalse(dynamicValueVar.isShadowed) // 临时变量不会被遮蔽

        // 验证新添加的临时变量也存在
        val newFromScriptVar = result.allVariables["new_from_script"]
        assertNotNull(newFromScriptVar)
        assertEquals("new_value", newFromScriptVar!!.finalValue)
        assertEquals(VariableScope.TEMPORARY, newFromScriptVar.scope)

        // 验证之前的变量现在被标记为遮蔽
        val globalDynamicVar = result.globalVariables.firstOrNull { it.key == "dynamic_value" }
        assertNotNull(globalDynamicVar)
        assertTrue(globalDynamicVar!!.isShadowed)

        val collectionDynamicVar = result.collectionVariables.firstOrNull { it.key == "dynamic_value" }
        assertNotNull(collectionDynamicVar)
        assertTrue(collectionDynamicVar!!.isShadowed)

        val environmentDynamicVar = result.environmentVariables.firstOrNull { it.key == "dynamic_value" }
        assertNotNull(environmentDynamicVar)
        assertTrue(environmentDynamicVar!!.isShadowed)

        // 验证临时变量数量
        assertEquals(2, result.temporaryVariables.size)

        // 验证获取临时变量的方法
        val tempVars = environmentService.getTemporaryVariables()
        assertEquals(2, tempVars.size)
        assertTrue(tempVars.containsKey("dynamic_value"))
        assertTrue(tempVars.containsKey("new_from_script"))

        // 验证清除临时变量的方法
        environmentService.clearTemporaryVariables()
        val clearedTempVars = environmentService.getTemporaryVariables()
        assertEquals(0, clearedTempVars.size)
    }
}