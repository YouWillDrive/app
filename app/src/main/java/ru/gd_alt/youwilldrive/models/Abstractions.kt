package ru.gd_alt.youwilldrive.models
import ru.gd_alt.youwilldrive.data.client.Connection

interface Identifiable {
    val id: String
}

interface ModelCompanion<T : Identifiable> {
    // Abstract property: Each companion must specify its table name
    val tableName: String

    // Abstract method: Each companion must know how to create an instance from a Map
    fun fromDictionary(dictionary: Map<*, *>): T

    suspend fun all(): List<T> {
        val items: MutableList<T> = mutableListOf()
        val result: List<*>? = Connection.cl.select(tableName)
        result?.forEach { item ->
            if (item is Map<*, *>) {
                try {
                    items.add(fromDictionary(item))
                } catch (e: Exception) {
                    System.err.println("Error parsing item for $tableName: $item. Error: ${e.message}")
                }
            }
        }
        return items
    }

    suspend fun fromId(id: String): T? {
        return all().find { it.id == id }
    }
}
/**
 * Fetches a list of related objects based on a linking table/record structure.
 * Assumes the linking table has "in" (ID of the current object) and "out" (ID of the related object).
 *
 * @param T The type of the related object. Must have a companion object with a `fromId(String): T?` function.
 * @param linkTableName The name of the linking table/record set to query.
 * @param relatedFromId A function reference (e.g., RelatedClass::fromId) to fetch the related object by its ID.
 * @param isChild Indicates if the current object is a child in the relationship (default is false, if true will fetch considering this id is out id).
 * @return A MutableList of related objects found. Returns an empty list if none are found or on error.
 */
suspend fun <T : Identifiable> Identifiable.fetchRelatedList(
    linkTableName: String,
    relatedFromId: suspend (String) -> T?,
    isChild: Boolean = false
): MutableList<T> {
    val relatedObjects: MutableList<T> = mutableListOf()
    // Using runBlocking as requested
    run {
        try {
            // Assuming select returns List<Map<*, *>>? or similar structure for link tables
            val result: List<*>? = Connection.cl.select(linkTableName)
            val linkMaps = result?.filterIsInstance<Map<*, *>>() // Safely filter for Maps

            linkMaps?.forEach { linkMap ->
                // Use safe access ?.toString()
                val inId = linkMap[if (isChild) "out" else "in"]?.toString()
                if (inId == this@fetchRelatedList.id.toString()) { // 'this' refers to the Identifiable object (Car, User, etc.)
                    val outId = linkMap[if (isChild) "in" else "out"]?.toString()
                    if (outId != null) {
                        try {
                            val relatedObject = relatedFromId(outId)
                            relatedObject?.let { relatedObjects.add(it) }
                        } catch (e: Exception) {
                            // Log error fetching/creating related object instance
                            System.err.println("Error fetching related object with ID '$outId' for link table '$linkTableName': ${e.message}")
                        }
                    } else {
                        System.err.println("Missing 'out' ID in link map for table '$linkTableName': $linkMap")
                    }
                }
            }
        } catch (e: Exception) {
            // Log error during the select call or processing
            System.err.println("Error fetching related list from '$linkTableName' for ID '${this@fetchRelatedList.id}': ${e.message}")
        }
    }
    return relatedObjects
}

/**
 * Fetches a single related object based on a linking table/record structure.
 * Assumes the linking table has "in" (ID of the current object) and "out" (ID of the related object).
 * Returns the *first* match found.
 *
 * @param T The type of the related object. Must have a companion object with a `fromId(String): T?` function.
 * @param linkTableName The name of the linking table/record set to query.
 * @param relatedFromId A function reference (e.g., RelatedClass::fromId) to fetch the related object by its ID.
 * @param isChild Indicates if the current object is a child in the relationship (default is false, if true will fetch considering this id is out id).
 * @return The first related object found, or null if none is found or on error.
 *
 * !! WARNING: Uses runBlocking, which blocks the calling thread. Avoid in performance-sensitive code (like UI thread). !!
 * !! WARNING: Error handling is basic. Consider adding more robust checks and logging. !!
 * !! WARNING: Assumes Connection.cl.select returns List<Map<*, *>>? for these link tables. !!
 */
suspend fun <T : Identifiable> Identifiable.fetchRelatedSingle(
    linkTableName: String,
    relatedFromId: suspend (String) -> T?,
    isChild: Boolean = false
): T? {
    // Using runBlocking as requested
    val relatedObject: T? = run {
        try {
            val result: List<*>? = Connection.cl.select(linkTableName)
            val linkMaps = result?.filterIsInstance<Map<*, *>>() // Safely filter for Maps

            linkMaps?.forEach { linkMap ->
                val inId = linkMap[if (isChild) "out" else "in"]?.toString()
                if (inId == this@fetchRelatedSingle.id) { // 'this' refers to the Identifiable object
                    val outId = linkMap[if (isChild) "in" else "out"]?.toString()
                    if (outId != null) {
                        try {
                            val foundObject = relatedFromId(outId)
                            if (foundObject != null) {
                                return@run foundObject // Found the first match, return it
                            }
                        } catch (e: Exception) {
                            System.err.println("Error fetching related object with ID '$outId' for link table '$linkTableName': ${e.message}")
                            // Continue searching in case this specific one failed but others might match
                        }
                    } else {
                        System.err.println("Missing 'out' ID in link map for table '$linkTableName': $linkMap")
                    }
                }
            }
        } catch (e: Exception) {
            System.err.println("Error fetching related single from '$linkTableName' for ID '${this@fetchRelatedSingle.id}': ${e.message}")
        }
        return@run null
    }
    return relatedObject
}

interface Participant: Identifiable {
    suspend fun events() : List<Event> {
        return fetchRelatedList("event_of_cadet", Event::fromId, true)
    }
}
