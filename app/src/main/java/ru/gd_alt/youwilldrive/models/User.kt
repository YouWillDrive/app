package ru.gd_alt.youwilldrive.models

import com.appmattus.crypto.Algorithm
import kotlinx.coroutines.runBlocking
import ru.gd_alt.youwilldrive.data.client.Connection

class User(override val id: String, var avatarPhoto: String, var phoneNum: String, var email: String, var pwdHash: String, var name: String, var surname: String, var patronymic: String) : Identifiable {
    companion object: ModelCompanion<User> {
        override val tableName: String = "users"

        override fun fromDictionary(dictionary: Map<*, *>): User {
            return User(
                dictionary["id"]!!.toString(),
                dictionary["avatar"]!!.toString(),
                dictionary["phone"]!!.toString(),
                dictionary["email"]!!.toString(),
                dictionary["password_hash"]!!.toString(),
                dictionary["name"]!!.toString(),
                dictionary["surname"]!!.toString(),
                dictionary["patronymic"]!!.toString()
            )
        }

        fun encryptPassword(password: String): String {
            val password = runBlocking {
                val result = Connection.cl.query("SELECT * FROM crypto::blake3(\$password)", mapOf("password" to "12345678")) as List<Map<*, List<*>>>
                return@runBlocking result[0]["result"]?.get(0)!!.toString()
            }

            return password
        }

        suspend fun authorize(email: String?, phone: String?, password: String): User? {
            if (password.isEmpty() || (email == null && phone == null)) {
                return null
            }

            val users: List<User> = all()
            val passwordHash = encryptPassword(password)

            for (user in users) {
                if ((email != null && user.email == email) || (phone != null && user.phoneNum == phone) && user.pwdHash == passwordHash) {
                    return user
                }
            }
            return null
        }
    }

    suspend fun role(): Role? {
        return fetchRelatedSingle<Role>("of_role", Role::fromId)
    }

    suspend fun isCadet(): Cadet? {
        return fetchRelatedSingle<Cadet>("is_cadet", Cadet::fromId)
    }

    suspend fun isInstructor(): Instructor? {
        return fetchRelatedSingle<Instructor>("is_instructor", Instructor::fromId)
    }

    suspend fun notifications(): MutableList<Notification> {
        return fetchRelatedList<Notification>("is_for", Notification::fromId, true)
    }

    suspend fun chats(): List<Chat> {
        return fetchRelatedList(
            "participates",
            Chat::fromId,
            true
        )
    }
}