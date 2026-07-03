package dev.kmpai.tools

import dev.kmpai.core.models.FunctionDefinition
import dev.kmpai.core.models.Tool
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * DSL for building type-safe [Tool] definitions.
 *
 * ```kotlin
 * val weatherTool = tool("get_weather") {
 *     description("Get the current weather for a city")
 *     parameters {
 *         string("city") {
 *             description("The city name, e.g. 'London'")
 *             required()
 *         }
 *         string("unit") {
 *             description("Temperature unit")
 *             enum("celsius", "fahrenheit")
 *         }
 *         boolean("includeHumidity") {
 *             description("Include humidity in the response")
 *         }
 *     }
 * }
 * ```
 */
@DslMarker
annotation class ToolDsl

@ToolDsl
class ToolBuilder(private val name: String) {
    private var description: String = ""
    private var paramsBuilder: ParametersBuilder? = null

    fun description(desc: String) { description = desc }

    fun parameters(block: ParametersBuilder.() -> Unit) {
        paramsBuilder = ParametersBuilder().apply(block)
    }

    fun build(): Tool = Tool(
        function = FunctionDefinition(
            name = name,
            description = description,
            parameters = paramsBuilder?.build() ?: buildJsonObject {
                put("type", "object")
                put("properties", buildJsonObject {})
            },
        )
    )
}

@ToolDsl
class ParametersBuilder {
    private val properties = mutableMapOf<String, JsonObject>()
    private val required = mutableListOf<String>()

    fun string(name: String, block: StringParamBuilder.() -> Unit = {}) {
        val builder = StringParamBuilder().apply(block)
        properties[name] = builder.build()
        if (builder.isRequired) required.add(name)
    }

    fun number(name: String, block: NumberParamBuilder.() -> Unit = {}) {
        val builder = NumberParamBuilder().apply(block)
        properties[name] = builder.build()
        if (builder.isRequired) required.add(name)
    }

    fun integer(name: String, block: IntegerParamBuilder.() -> Unit = {}) {
        val builder = IntegerParamBuilder().apply(block)
        properties[name] = builder.build()
        if (builder.isRequired) required.add(name)
    }

    fun boolean(name: String, block: BooleanParamBuilder.() -> Unit = {}) {
        val builder = BooleanParamBuilder().apply(block)
        properties[name] = builder.build()
        if (builder.isRequired) required.add(name)
    }

    fun array(name: String, itemType: String = "string", block: BaseParamBuilder.() -> Unit = {}) {
        val builder = BaseParamBuilder().apply(block)
        properties[name] = buildJsonObject {
            put("type", "array")
            putJsonObject("items") { put("type", itemType) }
            builder.descriptionValue?.let { put("description", it) }
        }
        if (builder.isRequired) required.add(name)
    }

    fun build(): JsonObject = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            properties.forEach { (key, value) -> put(key, value) }
        }
        if (required.isNotEmpty()) {
            putJsonArray("required") {
                required.forEach { add(JsonPrimitive(it)) }
            }
        }
        put("additionalProperties", false)
    }
}

@ToolDsl
open class BaseParamBuilder {
    internal var descriptionValue: String? = null
    internal var isRequired: Boolean = false

    fun description(desc: String) { descriptionValue = desc }
    fun required() { isRequired = true }
}

@ToolDsl
class StringParamBuilder : BaseParamBuilder() {
    private var enumValues: List<String>? = null

    fun enum(vararg values: String) { enumValues = values.toList() }

    fun build(): JsonObject = buildJsonObject {
        put("type", "string")
        descriptionValue?.let { put("description", it) }
        enumValues?.let { enums ->
            put("enum", JsonArray(enums.map { JsonPrimitive(it) }))
        }
    }
}

@ToolDsl
class NumberParamBuilder : BaseParamBuilder() {
    private var minimum: Double? = null
    private var maximum: Double? = null

    fun minimum(value: Double) { minimum = value }
    fun maximum(value: Double) { maximum = value }

    fun build(): JsonObject = buildJsonObject {
        put("type", "number")
        descriptionValue?.let { put("description", it) }
        minimum?.let { put("minimum", it) }
        maximum?.let { put("maximum", it) }
    }
}

@ToolDsl
class IntegerParamBuilder : BaseParamBuilder() {
    private var minimum: Int? = null
    private var maximum: Int? = null

    fun minimum(value: Int) { minimum = value }
    fun maximum(value: Int) { maximum = value }

    fun build(): JsonObject = buildJsonObject {
        put("type", "integer")
        descriptionValue?.let { put("description", it) }
        minimum?.let { put("minimum", it) }
        maximum?.let { put("maximum", it) }
    }
}

@ToolDsl
class BooleanParamBuilder : BaseParamBuilder() {
    fun build(): JsonObject = buildJsonObject {
        put("type", "boolean")
        descriptionValue?.let { put("description", it) }
    }
}

/** Create a [Tool] using the [ToolBuilder] DSL. */
fun tool(name: String, block: ToolBuilder.() -> Unit): Tool =
    ToolBuilder(name).apply(block).build()
