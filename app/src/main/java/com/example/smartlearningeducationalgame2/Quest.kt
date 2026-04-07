package com.example.smartlearningeducationalgame2

import com.google.firebase.firestore.Exclude

data class Quest(
    val id: Int = 0,
    val title: String = "",
    val isCompleted: Boolean = false,
    val requiredLevel: Int = 1,
    val classIds: List<String>? = null,
    val teacherId: String? = null,
    val rewardXp: Int = 50,
    val rewardGold: Double = 5.0,
    val difficulty: String = "NORMAL",
    @get:Exclude var firestoreId: String = ""
)
