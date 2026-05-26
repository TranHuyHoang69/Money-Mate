package com.example.moneymate.data.remote

data class FireStoreReminder(
    val id: Int = 0,
    val userId: String = "",
    val title: String = "",
    val note: String = "",
    val reminderDateTime: Long = 0L,
    val repeatInterval: String = "",
    val isActive: Boolean = true
)