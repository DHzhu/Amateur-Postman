package com.github.dhzhu.amateurpostman.utils

import com.fasterxml.jackson.databind.JsonNode
import com.github.dhzhu.amateurpostman.services.JsonService
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.Schema

/**
 * Converts a JSON string into an OpenAPI 3.0 Schema object.
 *
 * Performs simple structural inference:
 * - JSON object → schema with type "object" and inferred properties
 * - JSON array → schema with type "array" and inferred items schema
 * - Primitives at the root → schema with inferred primitive type
 * - Invalid/null JSON → null
 */
object JsonToSchemaConverter {

    fun convert(json: String?): Schema<*>? {
        if (json.isNullOrBlank()) return null
        return try {
            val node = JsonService.mapper.readTree(json)
            inferSchema(node)
        } catch (e: Exception) {
            null
        }
    }

    private fun inferSchema(node: JsonNode): Schema<*>? = when {
        node.isObject -> inferObjectSchema(node)
        node.isArray -> inferArraySchema(node)
        node.isValueNode && !node.isNull -> inferPrimitiveSchema(node)
        node.isNull -> Schema<Any>().type("string").nullable(true)
        else -> null
    }

    private fun inferObjectSchema(node: JsonNode): Schema<*> {
        val schema = Schema<Any>().type("object")
        if (node.size() > 0) {
            val properties = linkedMapOf<String, Schema<*>>()
            node.fields().forEach { (key, value) ->
                inferSchema(value)?.let { properties[key] = it }
            }
            if (properties.isNotEmpty()) schema.properties = properties
        }
        return schema
    }

    private fun inferArraySchema(node: JsonNode): Schema<*> {
        val arraySchema = ArraySchema()
        val first = node.firstOrNull()
        if (first != null) {
            inferSchema(first)?.let { arraySchema.items = it }
        }
        return arraySchema
    }

    private fun inferPrimitiveSchema(node: JsonNode): Schema<*> = when {
        node.isBoolean -> Schema<Boolean>().type("boolean")
        node.isNumber -> {
            if (node.isIntegralNumber) {
                Schema<Long>().type("integer").format("int64")
            } else {
                Schema<Double>().type("number").format("double")
            }
        }
        else -> Schema<String>().type("string")
    }
}
