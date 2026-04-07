package com.example.smartlearningeducationalgame2

data class FirestoreQuestion(
    val questionText: String = "",
    val answers: List<String> = emptyList(),
    val correctAnswer: String = "",
    val isBoss: Boolean = false,
    val expReward: Int = 10,  // Default normal reward
    val coinReward: Double = 1.0 // Default normal reward
)
