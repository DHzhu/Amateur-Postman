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
     * Gets the current environment's variables only (excluding globals).
     *
     * @return Map of current environment variables (normalized keys to values)
     */
    fun getCurrentEnvironmentVariables(): Map<String, String> {
        return getCurrentEnvironment()?.getVariablesMap() ?: emptyMap()
    }

    /**
     * Sets a variable in the current environment.
     * If no environment is selected, sets it as a global variable.
     *
     * @param key The variable key
     * @param value The variable value
     */
    fun setVariableInCurrent(key: String, value: String) {
        val currentEnv = getCurrentEnvironment()
        if (currentEnv != null) {
            val variable = Variable(key = key, value = value)
            updateEnvironment(currentEnv.setVariable(variable))
        } else {
            // Set as global variable if no environment is selected
            setGlobalVariable(Variable(key = key, value = value))
        }
    }

    /**
     * Gets a variable value from the current environment or globals.
     *
     * @param key The variable key (case-insensitive)
     * @return The variable value, or null if not found
     */
    fun getVariableFromCurrent(key: String): String? {
        return getCurrentEnvironment()?.getVariableValue(key)
            ?: getGlobalEnvironment().getVariableValue(key)
    }

    // ========== Collection Variables Management ==========

    /**
     * Gets collection variables for a specific collection.
     * Creates empty collection variables if none exist.
     *
     * @param collectionId The collection ID
     * @return CollectionVariables for the collection
     */
    fun getCollectionVariables(collectionId: String): CollectionVariables {
        return state.collectionVariables
            .firstOrNull { it.collectionId == collectionId }
            ?.toCollectionVariables()
            ?: CollectionVariables.create(collectionId)
    }

    /**
     * Gets all collection variables for a collection as a map.
     *
     * @param collectionId The collection ID
     * @return Map of variable keys to values
     */
    fun getCollectionVariablesMap(collectionId: String): Map<String, String> {
        return getCollectionVariables(collectionId).getVariablesMap()
    }

    /**
     * Sets or updates a collection variable.
     *
     * @param collectionId The collection ID
     * @param variable The variable to set
     */
    fun setCollectionVariable(collectionId: String, variable: Variable) {
        val collVars = getCollectionVariables(collectionId)
        val updated = collVars.setVariable(variable)
        val serializable = SerializableCollectionVariables.from(updated)

        // Update or add in state
        val existingIndex = state.collectionVariables.indexOfFirst { it.collectionId == collectionId }
        val updatedList = if (existingIndex >= 0) {
            state.collectionVariables.toMutableList().apply {
                set(existingIndex, serializable)
            }
        } else {
            state.collectionVariables + serializable
        }

        state = state.copy(collectionVariables = updatedList)
        logger.info("Set collection variable ${variable.key} in collection $collectionId")
        notifyListeners()
    }

    /**
     * Removes a collection variable by key.
     *
     * @param collectionId The collection ID
     * @param key The variable key to remove
     */
    fun removeCollectionVariable(collectionId: String, key: String) {
        val collVars = getCollectionVariables(collectionId)
        val updated = collVars.removeVariable(key)
        val serializable = SerializableCollectionVariables.from(updated)

        // Update in state
        val existingIndex = state.collectionVariables.indexOfFirst { it.collectionId == collectionId }
        if (existingIndex >= 0) {
            val updatedList = state.collectionVariables.toMutableList().apply {
                set(existingIndex, serializable)
            }
            state = state.copy(collectionVariables = updatedList)
            logger.info("Removed collection variable $key from collection $collectionId")
            notifyListeners()
        }
    }

    /**
     * Gets all variables that should be used for substitution.
     * Merges variables from multiple scopes with priority:
     * Global -> Collection -> Environment (highest priority)
     *
     * @param collectionId Optional collection ID to include collection variables
     * @return Map of all variables (normalized keys to values)
     */
    fun getAllVariables(collectionId: String? = null): Map<String, String> {
        val allVars = getGlobalVariablesMap().toMutableMap()

        // Add collection variables (override globals)
        collectionId?.let { id ->
            getCollectionVariables(id).getVariablesMap().let { collVars ->
                allVars.putAll(collVars)
            }
        }

        // Add environment variables (override collection and globals)
        val current = getCurrentEnvironment()
        current?.let { env ->
            val envVars = env.getVariablesMap()
            allVars.putAll(envVars)
        }

        logger.debug("Resolved ${allVars.size} variables for collection $collectionId")
        return allVars
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
