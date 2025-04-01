package ru.gd_alt.youwilldrive.models

import com.appmattus.crypto.Algorithm
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.Serializable

@Serializable
class User(val id: Int, var role: Role, var avatarPhoto: String, var phoneNum: String, var email: String, var pwdHash: String, var name: String, var surname: String, var patronymic: String) {
    companion object {
        suspend fun login(phoneNum: String, pwd: String): User? {
            return null
        }
    }
}