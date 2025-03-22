package ru.gd_alt.youwilldrive.models

class User(val id: Int, var role: Role, var avatarPhoto: String, var phoneNum: String, var email: String, var pwdHash: String, var name: String, var surname: String, var patronymic: String)