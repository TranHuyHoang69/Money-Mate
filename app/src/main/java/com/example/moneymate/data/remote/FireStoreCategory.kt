package com.example.moneymate.data.remote

import com.google.firebase.firestore.PropertyName

data class FireStoreCategory(
    var categoryId: Long = 0,
    var userId: String = "",
    var title: String = "",
    var iconResName: String = "",
    var colorHex: String = "",
    var type: String = "",

    @get:PropertyName("default")
    @set:PropertyName("default")
    var isDefault: Boolean = false
)