package com.example.smartlearningeducationalgame2

import com.google.firebase.firestore.Exclude

data class Dungeon(
    val id: Int = 0,
    val title: String = "",
    val requiredLevel: Int = 1,
    val entryFee: Double = 0.0, // Added entry fee
    @get:Exclude var firestoreId: String = ""
)
