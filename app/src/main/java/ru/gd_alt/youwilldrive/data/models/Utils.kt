package ru.gd_alt.youwilldrive.data.models

import kotlin.reflect.full.*

fun <T : Any> T.toDictFromConstructor(): Map<String, Any?> {
    val map = mutableMapOf<String, Any?>()
    val kClass = this::class // Get KClass from instance [2, 11, 12]

    // Get all member properties of the class [9]
    val properties = kClass.memberProperties

    for (prop in properties) {
        // Check if the property is associated with a constructor parameter.
        // Note: This isn't a direct check if it's *only* in the primary constructor
        // via standard memberProperties, but for typical data classes/classes
        // with val/var in the primary constructor, memberProperties works.
        // A more robust check would involve inspecting constructor parameters,
        // but memberProperties is often sufficient for this use case.

        val propertyName = prop.name
        // Get the value of the property from the instance
        val propertyValue = prop.getter.call(this)

        map[propertyName] = propertyValue
    }

    return map
}

data class RpcRequest(
    val id: Long,
    val method: String,
    val params: List<Any?> = emptyList()
)