package com.example.smartlearningeducationalgame2

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.util.Log
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date

class MainActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var rvQuests: RecyclerView
    private lateinit var tvQuestProgress: TextView
    private lateinit var tvMainLevel: TextView
    private lateinit var tvWelcomeMessage: TextView
    private lateinit var pbMainHP: ProgressBar
    private lateinit var pbMainEXP: ProgressBar
    private lateinit var tvMainHPValue: TextView
    private lateinit var tvMainEXPValue: TextView
    private lateinit var tvMainCoins: TextView

    private var questList = mutableListOf<Quest>()
    private var currentHealth: Int = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Initialize Firebase
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Initialize Views
        rvQuests = findViewById(R.id.rvQuests)
        tvQuestProgress = findViewById(R.id.tvQuestProgress)
        tvMainLevel = findViewById(R.id.tvMainLevel)
        tvWelcomeMessage = findViewById(R.id.tvWelcomeMessage)
        pbMainHP = findViewById(R.id.pbMainHP)
        pbMainEXP = findViewById(R.id.pbMainEXP)
        tvMainHPValue = findViewById(R.id.tvMainHPValue)
        tvMainEXPValue = findViewById(R.id.tvMainEXPValue)
        tvMainCoins = findViewById(R.id.tvMainCoins)

        // Setup RecyclerView
        rvQuests.layoutManager = LinearLayoutManager(this)
        rvQuests.adapter = QuestAdapter(questList, {})

        // Navigation Listeners
        findViewById<TextView>(R.id.btnDungeonsToggle).setOnClickListener {
            startActivity(Intent(this, DungeonsActivity::class.java))
            overridePendingTransition(0, 0)
        }
        findViewById<LinearLayout>(R.id.navAvatar).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            overridePendingTransition(0, 0)
        }
        findViewById<LinearLayout>(R.id.navClasses).setOnClickListener {
            startActivity(Intent(this, ClassesActivity::class.java))
            overridePendingTransition(0, 0)
        }

        checkRoleAndHideTutorial()

        // Start Background Music
        val musicIntent = Intent(this, MusicService::class.java)
        startService(musicIntent)
    }

    override fun onResume() {
        super.onResume()
        fetchUserStatsAndProgress()
    }

    private fun checkRoleAndHideTutorial() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).get().addOnSuccessListener { doc ->
            if (isFinishing || isDestroyed) return@addOnSuccessListener
            
            // Check for tutorial
            if (doc.getBoolean("isFirstTime") == true) {
                showTutorialDialog(userId)
            }
        }
    }

    private fun showTutorialDialog(userId: String) {
        AlertDialog.Builder(this)
            .setTitle("🌟 Welcome, Hero!")
            .setMessage("Welcome to Numeria! Before you start your journey, would you like a quick guide on how to play?")
            .setPositiveButton("LEARN MORE") { _, _ ->
                // Mark tutorial as seen immediately so it doesn't pop up again
                db.collection("users").document(userId).update("isFirstTime", false)
                startActivity(Intent(this, HelpSupportActivity::class.java))
            }
            .setNegativeButton("I KNOW THE WAY", { _, _ ->
                db.collection("users").document(userId).update("isFirstTime", false)
            })
            .setCancelable(false)
            .show()
    }

    private fun fetchUserStatsAndProgress() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).get().addOnSuccessListener { document ->
            if (isFinishing || isDestroyed || !document.exists()) return@addOnSuccessListener

            checkHealthReset(document)

            val userName = document.getString("name")?.uppercase() ?: "TRAVELLER"
            tvWelcomeMessage.text = userName

            tvMainLevel.text = "Level ${document.getLong("level") ?: 1}"
            
            // HP Update
            currentHealth = (document.getLong("health") ?: 100).toInt()
            pbMainHP.progress = currentHealth
            tvMainHPValue.text = "$currentHealth / 100"
            
            // EXP Update
            val expVal = document.getLong("experience") ?: 0L
            val expProgress = (expVal % 100).toInt()
            pbMainEXP.progress = expProgress
            tvMainEXPValue.text = "$expProgress / 100"
            
            tvMainCoins.text = String.format("%.2f", document.getDouble("coins") ?: 0.0)

            fetchAssignedQuests(userId)
        }
    }

    private fun checkHealthReset(doc: com.google.firebase.firestore.DocumentSnapshot) {
        val lastReset = doc.getTimestamp("lastHealthReset")?.toDate() ?: return
        val currentTime = Date().time

        if (currentTime - lastReset.time >= 3600000) {
            db.collection("users").document(doc.id).update(
                "health", 100,
                "lastHealthReset", FieldValue.serverTimestamp()
            ).addOnSuccessListener {
                if (!isFinishing) {
                    currentHealth = 100
                    pbMainHP.progress = 100
                    tvMainHPValue.text = "100 / 100"
                    Toast.makeText(this, "You have fully recovered!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun fetchAssignedQuests(userId: String) {
        db.collection("assignments")
            .whereEqualTo("studentId", userId)
            .get()
            .addOnSuccessListener { result ->
                if (isFinishing || isDestroyed) return@addOnSuccessListener
                questList.clear()
                for (doc in result) {
                    if ((doc.getString("status") ?: "assigned") == "assigned") {
                        val q = Quest(
                            title = doc.getString("title") ?: "Unnamed Quest",
                            firestoreId = doc.getString("questId") ?: "",
                            rewardXp = doc.getLong("rewardXp")?.toInt() ?: 50,
                            rewardGold = doc.getDouble("rewardGold") ?: 5.0,
                            difficulty = doc.getString("difficulty") ?: "NORMAL"
                        )
                        questList.add(q)
                    }
                }
                updateUI()
            }
    }

    private fun updateUI() {
        if (isFinishing || isDestroyed) return
        tvQuestProgress.text = "${questList.size} Active Quests"
        rvQuests.adapter = QuestAdapter(questList, { clickedQuest ->
            if (currentHealth <= 0) {
                Toast.makeText(this, "You are still recovering!", Toast.LENGTH_LONG).show()
            } else {
                val intent = Intent(this, BattleActivity::class.java)
                intent.putExtra("ENCOUNTER_NAME", clickedQuest.title)
                intent.putExtra("QUEST_DOC_ID", clickedQuest.firestoreId)
                startActivity(intent)
            }
        })
    }
}
