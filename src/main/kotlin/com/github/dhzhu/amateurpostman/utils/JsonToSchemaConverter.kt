package com.github.dhzhu.amateurpostman.utils

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
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
            val element = JsonParser.parseString(json)
            inferSchema(element)
        } catch (e: Exception) {
            null
        }
    }

    private fun inferSchema(element: com.google.gson.JsonElement): Schema<*>? = when {
        element.isJsonObject -> inferObjectSchema(element.asJsonObject)
        element.isJsonArray -> inferArraySchema(element.asJsonArray)
        element.isJsonPrimitive -> inferPrimitiveSchema(element.asJsonPrimitive)
        element.isJsonNull -> Schema<Any>().type("string").nullable(true)
        else -> null
    }

    private fun inferObjectSchema(obj: JsonObject): Schema<*> {
        val schema = Schema<Any>().type("object")
        if (obj.size() > 0) {
            val properties = linkedMapOf<String, Schema<*>>()
            obj.entrySet().forEach { (key, value) ->
                inferSchema(value)?.let { properties[key] = it }
            }
            if (properties.isNotEmpty()) schema.properties = properties
        }
        return schema
    }

    private fun inferArraySchema(arr: JsonArray): Schema<*> {
        val arraySchema = ArraySchema()
        val firstElement = arr.firstOrNull()
        if (firstElement != null) {
            inferSchema(firstElement)?.let { arraySchema.items = it }
        }
        return arraySchema
    }

    private fun inferPrimitiveSchema(primitive: JsonPrimitive): Schema<*> = when {
        primitive.isBoolean -> Schema<Boolean>().type("boolean")
        primitive.isNumber -> {
            val number = primitive.asNumber
            if (number.toDouble() == number.toLong().toDouble()) {
                Schema<Long>().type("integer").format("int64")
            } else {
                Schema<Double>().type("number").format("double")
            }
        }
        else -> Schema<String>().type("string")
    }
}
