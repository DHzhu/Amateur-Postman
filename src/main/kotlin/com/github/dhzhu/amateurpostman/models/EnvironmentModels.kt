package com.github.dhzhu.amateurpostman.models

/**
 * Represents a single variable with key-value pair and metadata.
 *
 * Variables can be stored in environments or globally, and are used
 * for dynamic substitution in HTTP requests.
 *
 * @property key The variable identifier (case-insensitive)
 * @property value The variable value to substitute
 * @property description Optional description of the variable's purpose
 * @property enabled Whether this variable is active for substitution
 */
data class Variable(
    val key: String,
    val value: String,
    val description: String = "",
    val enabled: Boolean = true
) {
    companion object {
        /**
         * Normalizes a variable key for case-insensitive comparison.
         */
        fun normalizeKey(key: String): String = key.lowercase()
    }

    /**
     * Returns the normalized (lowercase) version of this variable's key.
     */
    fun normalizedKey(): String = normalizeKey(key)
}

/**
 * Represents a collection of variables under a named environment.
 *
 * Environments allow switching between different sets of variables,
 * such as Development, Staging, and Production configurations.
 *
 * @property id Unique identifier for this environment
 * @property name Display name for this environment
 * @property variables List of variables in this environment
 * @property isGlobal Whether this is the global variables environment
 */
data class Environment(
    val id: String,
    val name: String,
    val variables: List<Variable> = emptyList(),
    val isGlobal: Boolean = false
) {
    companion object {
        /**
         * Creates a new environment with a generated UUID.
         */
        fun create(name: String, isGlobal: Boolean = false): Environment {
            return Environment(
                id = java.util.UUID.randomUUID().toString(),
                name = name,
                variables = emptyList(),
                isGlobal = isGlobal
            )
        }
    }

    /**
     * Gets the value of a variable by key (case-insensitive lookup).
     * Only returns values for enabled variables.
     *
     * @param key The variable key to look up
     * @return The variable value, or null if not found or disabled
     */
    fun getVariableValue(key: String): String? {
        val normalizedKey = Variable.normalizeKey(key)
        return variables
            .firstOrNull { it.normalizedKey() == normalizedKey && it.enabled }
            ?.value
    }

    /**
     * Checks if a variable exists (case-insensitive).
     *
     * @param key The variable key to check
     * @return true if the variable exists, false otherwise
     */
    fun hasVariable(key: String): Boolean {
        val normalizedKey = Variable.normalizeKey(key)
        return variables.any { it.normalizedKey() == normalizedKey }
    }

    /**
     * Sets or updates a variable in this environment.
     * If the variable key already exists (case-insensitive), it will be replaced.
     *
     * @param variable The variable to set
     * @return A new Environment instance with the updated variable list
     */
    fun setVariable(variable: Variable): Environment {
        val normalizedKey = variable.normalizedKey()
        val existingIndex = variables.indexOfFirst { it.normalizedKey() == normalizedKey }

        return if (existingIndex >= 0) {
            // Replace existing variable
            copy(variables = variables.toMutableList().apply {
                set(existingIndex, variable)
            })
        } else {
            // Add new variable
            copy(variables = variables + variable)
        }
    }

    /**
     * Removes a variable from this environment by key (case-insensitive).
     *
     * @param key The variable key to remove
     * @return A new Environment instance with the variable removed
     */
    fun removeVariable(key: String): Environment {
        val normalizedKey = Variable.normalizeKey(key)
        return copy(
            variables = variables.filterNot { it.normalizedKey() == normalizedKey }
        )
    }

    /**
     * Gets all enabled variables as a map for quick lookup.
     *
     * @return Map of normalized keys to values (only enabled variables)
     */
    fun getVariablesMap(): Map<String, String> {
        return variables
            .filter { it.enabled }
            .associate { it.normalizedKey() to it.value }
    }

    /**
     * Gets all variables (both enabled and disabled) as a map.
     *
     * @return Map of normalized keys to values
     */
    fun getAllVariablesMap(): Map<String, String> {
        return variables.associate { it.normalizedKey() to it.value }
    }
}

/**
 * Persistent state for the environment system.
 *
 * This class is serialized to XML and stored in the project.
 *
 * @property version Schema version for migration support
 * @property environments List of all environments
 * @property currentEnvironmentId ID of the currently selected environment
 * @property globalVariables List of global variables (stored as Environment with isGlobal=true)
 */
data class EnvironmentState(
    val version: Int = 1,
    val environments: List<SerializableEnvironment> = emptyList(),
    val currentEnvironmentId: String? = null,
    val globalVariables: SerializableEnvironment? = null
)

/**
 * Serializable version of Environment for XML persistence.
 *
 * @property id Unique identifier
 * @property name Display name
 * @property variables List of serializable variables
 * @property isGlobal Whether this is the global environment
 * @property order Display order in UI
 */
data class SerializableEnvironment(
    val id: String,
    val name: String,
    val variables: List<SerializableVariable> = emptyList(),
    val isGlobal: Boolean = false,
    val order: Int = 0
) {
    /**
     * Converts this serializable environment to a domain Environment.
     */
    fun toEnvironment(): Environment {
        return Environment(
            id = id,
            name = name,
            variables = variables.map { it.toVariable() },
            isGlobal = isGlobal
        )
    }

    companion object {
        /**
         * Creates a SerializableEnvironment from a domain Environment.
         */
        fun from(environment: Environment, order: Int = 0): SerializableEnvironment {
            return SerializableEnvironment(
                id = environment.id,
                name = environment.name,
                variables = environment.variables.map { SerializableVariable.from(it) },
                isGlobal = environment.isGlobal,
                order = order
            )
        }
    }
}

/**
 * Serializable version of Variable for XML persistence.
 *
 * @property key The variable identifier
 * @property value The variable value
 * @property description Optional description
 * @property enabled Whether the variable is active
 */
data class SerializableVariable(
    val key: String,
    val value: String,
    val description: String = "",
    val enabled: Boolean = true
) {
    /**
     * Converts this serializable variable to a domain Variable.
     */
    fun toVariable(): Variable {
        return Variable(
            key = key,
            value = value,
            description = description,
            enabled = enabled
        )
    }

    companion object {
        /**
         * Creates a SerializableVariable from a domain Variable.
         */
        fun from(variable: Variable): SerializableVariable {
            return SerializableVariable(
                key = variable.key,
                value = variable.value,
                description = variable.description,
                enabled = variable.enabled
            )
        }
    }
}
