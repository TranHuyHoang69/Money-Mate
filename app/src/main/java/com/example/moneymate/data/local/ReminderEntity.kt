package com.example.moneymate.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: String = "",
    val title: String,
    val note: String,
    val reminderDateTime: Long,
    val repeatInterval: String,
    val isActive: Boolean = true
) {
    fun toFirestoreMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "userId" to userId,
            "title" to title,
            "note" to note,
            "reminderDateTime" to reminderDateTime,
            "repeatInterval" to repeatInterval,
            "isActive" to isActive
        )
    }
}