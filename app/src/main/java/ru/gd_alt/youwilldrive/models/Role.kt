package ru.gd_alt.youwilldrive.models

import kotlinx.coroutines.runBlocking
import ru.gd_alt.youwilldrive.data.client.Connection

class Role(override val id: String, var name: String) : Identifiable {
    companion object: ModelCompanion<Role> {
        override val tableName: String = "roles"

        override fun fromDictionary(dictionary: Map<*, *>): Role {
            return Role(dictionary["id"]!!.toString(), dictionary["name"]!!.toString())
        }

        suspend fun fromName(name: String): Role {
            val roles: List<Role> = all()
            for (role in roles) {
                if (role.name == name) {
                    return role
                }
            }
            run {
                Connection.cl.insert("roles", mapOf("name" to name))
            }
            val rolesNew: List<Role> = all()
            for (role in rolesNew) {
                if (role.name == name) {
                    return role
                }
            }
            return Role("0", "none")
        }
    }
}