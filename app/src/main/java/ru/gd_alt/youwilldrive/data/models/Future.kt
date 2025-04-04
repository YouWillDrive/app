package ru.gd_alt.youwilldrive.data.models

/**
 * Some bizarre and fancy SurrealDB thing that does not work with Kotlin serialization.
 * It'll throw an error if you try to serialize it.
 */
class Future(args: List<Any>) {
    /**
     * Something here does not wish to be remembered.
     * @throws Exception when the future refused to be made static.
     */
    fun throwException() {
        throw Exception("This is a Future object. It cannot be serialized.")
    }
}