package com.github.dhzhu.amateurpostman.services

import com.github.dhzhu.amateurpostman.models.*
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.openapi.diagnostic.thisLogger

/**
 * Service for managing environments and variables with persistent storage.
 *
 * This service provides CRUD operations for environments, manages global variables,
 * handles environment switching, and notifies listeners of changes.
 *
 * @property project The IntelliJ project this service belongs to
 */
@Service(Service.Level.PROJECT)
@State(name = "EnvironmentService", storages = [Storage("amateur-postman-env.xml")])
class EnvironmentService(private val project: Project) :
    PersistentStateComponent<EnvironmentState> {

    private val logger = thisLogger()

    private var state = EnvironmentState()
    private val listeners = mutableListOf<EnvironmentChangeListener>()

    override fun getState(): EnvironmentState = state

    override fun loadState(state: EnvironmentState) {
        this.state = state
        logger.info("Loaded ${state.environments.size} environments")
    }

    // ========== Environment CRUD Operations ==========

    /**
     * Creates a new environment with the given name.
     *
     * @param name The display name for the environment
     * @return The newly created Environment
     */
    fun createEnvironment(name: String): Environment {
        val environment = Environment.create(name)
        val serializable = SerializableEnvironment.from(environment, state.environments.size)
        state = state.copy(environments = state.environments + serializable)
        logger.info("Created environment: ${environment.name} (${environment.id})")
        notifyListeners()
        return environment
    }

    /**
     * Gets an environment by its ID.
     *
     * @param id The environment ID
     * @return The Environment, or null if not found
     */
    fun getEnvironment(id: String): Environment? {
        return state.environments.find { it.id == id }?.toEnvironment()
    }

    /**
     * Gets all environments (excluding global).
     *
     * @return List of all environments
     */
    fun getEnvironments(): List<Environment> {
        return state.environments
            .filter { !it.isGlobal }
            .sortedBy { it.order }
            .map { it.toEnvironment() }
    }

    /**
     * Updates an existing environment.
     *
     * @param environment The environment to update
     */
    fun updateEnvironment(environment: Environment) {
        val index = state.environments.indexOfFirst { it.id == environment.id }
        if (index >= 0) {
            val serializable = SerializableEnvironment.from(
                environment,
                state.environments[index].order
            )
            val updatedList = state.environments.toMutableList()
            updatedList[index] = serializable
            state = state.copy(environments = updatedList)
            logger.info("Updated environment: ${environment.name}")
            notifyListeners()
        } else {
            logger.warn("Attempted to update non-existent environment: ${environment.id}")
        }
    }

    /**
     * Deletes an environment by ID.
     * If this is the current environment, clears the current selection.
     *
     * @param id The environment ID to delete
     */
    fun deleteEnvironment(id: String) {
        val env = getEnvironment(id)
        if (env != null) {
            val newCurrentId = if (state.currentEnvironmentId == id) null else state.currentEnvironmentId
            state = state.copy(
                environments = state.environments.filter { it.id != id },
                currentEnvironmentId = newCurrentId
            )
            logger.info("Deleted environment: ${env.name}")
            notifyListeners()
        }
    }

    /**
     * Renames an environment.
     *
     * @param id The environment ID
     * @param newName The new name
     * @return true if renamed, false if environment not found
     */
    fun renameEnvironment(id: String, newName: String): Boolean {
        val index = state.environments.indexOfFirst { it.id == id }
        if (index >= 0) {
            val oldName = state.environments[index].name
            val updatedList = state.environments.toMutableList()
            updatedList[index] = updatedList[index].copy(name = newName)
            state = state.copy(environments = updatedList)
            logger.info("Renamed environment '$oldName' to '$newName'")
            notifyListeners()
            return true
        }
        return false
    }

    // ========== Current Environment Management ==========

    /**
     * Gets the currently selected environment.
     *
     * @return The current Environment, or null if none selected
     */
    fun getCurrentEnvironment(): Environment? {
        return state.currentEnvironmentId?.let { getEnvironment(it) }
    }

    /**
     * Sets the current environment by ID.
     *
     * @param id The environment ID to set as current
     * @return true if set successfully, false if environment not found
     */
    fun setCurrentEnvironment(id: String): Boolean {
        if (getEnvironment(id) != null) {
            state = state.copy(currentEnvironmentId = id)
            val envName = getEnvironment(id)?.name ?: "Unknown"
            logger.info("Set current environment to: $envName")
            notifyListeners()
            return true
        }
        return false
    }

    /**
     * Clears the current environment selection.
     */
    fun clearCurrentEnvironment() {
        state = state.copy(currentEnvironmentId = null)
        logger.info("Cleared current environment")
        notifyListeners()
    }

    // ========== Global Variables Management ==========

    /**
     * Gets the global environment containing global variables.
     *
     * @return The global Environment, or empty environment if none exists
     */
    private fun getGlobalEnvironment(): Environment {
        return state.globalVariables?.toEnvironment()
            ?: Environment.create("Globals", isGlobal = true)
    }

    /**
     * Gets all global variables.
     *
     * @return List of global variables
     */
    fun getGlobalVariables(): List<Variable> {
        return state.globalVariables?.variables?.map { it.toVariable() }
            ?: emptyList()
    }

    /**
     * Sets or updates a global variable.
     *
     * @param variable The variable to set
     */
    fun setGlobalVariable(variable: Variable) {
        val globalEnv = getGlobalEnvironment()
        val updatedEnv = globalEnv.setVariable(variable)
        state = state.copy(globalVariables = SerializableEnvironment.from(updatedEnv, 0))
        logger.info("Set global variable: ${variable.key}")
        notifyListeners()
    }

    /**
     * Removes a global variable by key.
     *
     * @param key The variable key to remove
     */
    fun removeGlobalVariable(key: String) {
        val globalEnv = getGlobalEnvironment()
        val updatedEnv = globalEnv.removeVariable(key)
        state = state.copy(globalVariables = SerializableEnvironment.from(updatedEnv, 0))
        logger.info("Removed global variable: $key")
        notifyListeners()
    }

    /**
     * Gets global variables as a map.
     *
     * @return Map of variable keys to values
     */
    fun getGlobalVariablesMap(): Map<String, String> {
        return getGlobalEnvironment().getVariablesMap()
    }

    // ========== Variable Management for Environments ==========

    /**
     * Adds a variable to an environment.
     *
     * @param environmentId The environment ID
     * @param variable The variable to add
     * @return true if added, false if environment not found
     */
    fun addVariable(environmentId: String, variable: Variable): Boolean {
        val env = getEnvironment(environmentId)
        if (env != null) {
            val updatedEnv = env.setVariable(variable)
            updateEnvironment(updatedEnv)
            logger.info("Added variable ${variable.key} to ${env.name}")
            return true
        }
        return false
    }

    /**
     * Updates a variable in an environment.
     *
     * @param environmentId The environment ID
     * @param variable The variable to update
     * @return true if updated, false if environment not found
     */
    fun updateVariable(environmentId: String, variable: Variable): Boolean {
        return addVariable(environmentId, variable)
    }

    /**
     * Removes a variable from an environment.
     *
     * @param environmentId The environment ID
     * @param variableKey The variable key to remove
     * @return true if removed, false if environment not found
     */
    fun removeVariable(environmentId: String, variableKey: String): Boolean {
        val env = getEnvironment(environmentId)
        if (env != null) {
            val updatedEnv = env.removeVariable(variableKey)
            updateEnvironment(updatedEnv)
            logger.info("Removed variable $variableKey from ${env.name}")
            return true
        }
        return false
    }

    // ========== Variable Resolution ==========

    /**
     * Gets all variables that should be used for substitution.
     * Merges global variables with current environment variables.
     * Environment variables take precedence over global variables.
     *
     * @return Map of all variables (normalized keys to values)
     */
    fun getAllVariables(): Map<String, String> {
        val globals = getGlobalVariablesMap().toMutableMap()
        val current = getCurrentEnvironment()

        current?.let { env ->
            // Environment variables override global variables
            val envVars = env.getVariablesMap()
            globals.putAll(envVars)
        }

        logger.debug("Resolved ${globals.size} variables (${globals.size - getGlobalVariablesMap().size} from environment, ${getGlobalVariablesMap().size} from globals)")
        return globals
    }

    // ========== Change Listeners ==========

    /**
     * Adds a listener for environment changes.
     *
     * @param listener The listener to add
     */
    fun addChangeListener(listener: EnvironmentChangeListener) {
        listeners.add(listener)
    }

    /**
     * Removes a change listener.
     *
     * @param listener The listener to remove
     */
    fun removeChangeListener(listener: EnvironmentChangeListener) {
        listeners.remove(listener)
    }

    private fun notifyListeners() {
        listeners.forEach { it.onEnvironmentChanged() }
    }
}

/**
 * Interface for listening to environment changes.
 */
interface EnvironmentChangeListener {
    /**
     * Called when environments or variables change.
     */
    fun onEnvironmentChanged()
}
