package ru.gd_alt.youwilldrive.data.models

class RecordID(val tableName: String, val recordId: String) {
    override fun toString(): String {
        return "$tableName:$recordId"
    }
}