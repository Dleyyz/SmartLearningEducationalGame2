package com.example.smartlearningeducationalgame2

import android.content.Intent
import android.os.Bundle
import android.view.View
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

class DungeonsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    
    private lateinit var tvDungeonsLevel: TextView
    private lateinit var pbDungeonsHP: ProgressBar
    private lateinit var pbDungeonsEXP: ProgressBar
    private lateinit var tvDungeonsHPValue: TextView
    private lateinit var tvDungeonsEXPValue: TextView
    private lateinit var tvDungeonsCoins: TextView
    private lateinit var tvWelcomeMessage: TextView
    
    private var playerLevel: Int = 1
    private var currentHealth: Int = 100
    private var playerCoins: Double = 0.0
    private var purchasedDungeonIds = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dungeons)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        tvDungeonsLevel = findViewById(R.id.tvDungeonsLevel)
        pbDungeonsHP = findViewById(R.id.pbDungeonsHP)
        pbDungeonsEXP = findViewById(R.id.pbDungeonsEXP)
        tvDungeonsHPValue = findViewById(R.id.tvDungeonsHPValue)
        tvDungeonsEXPValue = findViewById(R.id.tvDungeonsEXPValue)
        tvDungeonsCoins = findViewById(R.id.tvDungeonsCoins)
        tvWelcomeMessage = findViewById(R.id.tvDungeonsWelcomeMessage)
        val navDungeons = findViewById<LinearLayout>(R.id.navDungeons)

        findViewById<TextView>(R.id.btnGuildToggle).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(0, 0)
        }

        findViewById<LinearLayout>(R.id.navAvatar).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            overridePendingTransition(0, 0)
        }
        findViewById<LinearLayout>(R.id.navQuest).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(0, 0)
        }
        findViewById<LinearLayout>(R.id.navClasses).setOnClickListener {
            startActivity(Intent(this, ClassesActivity::class.java))
            overridePendingTransition(0, 0)
        }

        checkRoleAndHideDungeons(navDungeons)
    }

    override fun onResume() {
        super.onResume()
        fetchUserStats()
    }

    private fun checkRoleAndHideDungeons(navDungeons: LinearLayout?) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).get().addOnSuccessListener { doc ->
            val role = doc.getString("role")
            if (role == "Teacher") {
                navDungeons?.visibility = View.VISIBLE
                navDungeons?.setOnClickListener {
                    startActivity(Intent(this, TeacherDungeonsActivity::class.java))
                    overridePendingTransition(0, 0)
                }
            } else {
                navDungeons?.visibility = View.GONE
            }
        }
    }

    private fun fetchUserStats() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                checkHealthReset(doc)
                
                val userName = doc.getString("name")?.uppercase() ?: "TRAVELLER"
                tvWelcomeMessage.text = userName

                playerLevel = (doc.getLong("level") ?: 1).toInt()
                currentHealth = (doc.getLong("health") ?: 100).toInt()
                playerCoins = doc.getDouble("coins") ?: 0.0
                
                // Load purchased dungeons
                purchasedDungeonIds.clear()
                val owned = doc.get("purchasedDungeons") as? List<String>
                if (owned != null) purchasedDungeonIds.addAll(owned)
                
                tvDungeonsLevel.text = "Level $playerLevel"
                
                // HP Update
                pbDungeonsHP.progress = currentHealth
                tvDungeonsHPValue.text = "$currentHealth / 100"
                
                // EXP Update
                val expVal = doc.getLong("experience") ?: 0L
                val expProgress = (expVal % 100).toInt()
                pbDungeonsEXP.progress = expProgress
                tvDungeonsEXPValue.text = "$expProgress / 100"
                
                tvDungeonsCoins.text = String.format("%.2f", playerCoins)
                
                fetchDungeonsFromFirestore()
            }
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
                if(!isFinishing) { 
                    currentHealth = 100
                    pbDungeonsHP.progress = 100
                    tvDungeonsHPValue.text = "100 / 100"
                    Toast.makeText(this, "You have fully recovered!", Toast.LENGTH_SHORT).show()
                } 
            }
        }
    }

    private fun fetchDungeonsFromFirestore() {
        db.collection("dungeons").get().addOnSuccessListener { result ->
            val dungeons = mutableListOf<Dungeon>()
            for (document in result) {
                val dungeon = document.toObject(Dungeon::class.java)
                dungeon.firestoreId = document.id
                dungeons.add(dungeon)
            }
            
            val rvDungeons = findViewById<RecyclerView>(R.id.rvDungeons)
            rvDungeons.layoutManager = LinearLayoutManager(this)
            
            rvDungeons.adapter = DungeonAdapter(dungeons, playerLevel, purchasedDungeonIds) { clickedDungeon ->
                handleDungeonEntry(clickedDungeon)
            }
        }
    }

    private fun handleDungeonEntry(dungeon: Dungeon) {
        if (currentHealth <= 0) {
            Toast.makeText(this, "You are still recovering! Please wait.", Toast.LENGTH_SHORT).show()
            return
        }

        if (playerLevel < dungeon.requiredLevel) {
            Toast.makeText(this, "Requires Level ${dungeon.requiredLevel}!", Toast.LENGTH_SHORT).show()
            return
        }

        // Check if already purchased
        if (purchasedDungeonIds.contains(dungeon.firestoreId)) {
            enterDungeon(dungeon)
        } else {
            // Show Confirmation Dialog for first-time purchase
            AlertDialog.Builder(this)
                .setTitle("Unlock ${dungeon.title}")
                .setMessage("Permanent Unlock Fee: $${String.format("%.2f", dungeon.entryFee)}\nDo you want to unlock this dungeon forever?")
                .setPositiveButton("YES") { _, _ ->
                    processPurchase(dungeon)
                }
                .setNegativeButton("NO", null)
                .show()
        }
    }

    private fun enterDungeon(dungeon: Dungeon) {
        val intent = Intent(this, BattleActivity::class.java)
        intent.putExtra("ENCOUNTER_NAME", dungeon.title)
        intent.putExtra("QUEST_DOC_ID", dungeon.firestoreId)
        intent.putExtra("IS_DUNGEON", true)
        startActivity(intent)
    }

    private fun processPurchase(dungeon: Dungeon) {
        if (playerCoins >= dungeon.entryFee) {
            val userId = auth.currentUser?.uid ?: return
            val newBalance = playerCoins - dungeon.entryFee
            
            // Deduct coins and add to purchased list permanently
            db.collection("users").document(userId).update(
                "coins", newBalance,
                "purchasedDungeons", FieldValue.arrayUnion(dungeon.firestoreId)
            ).addOnSuccessListener {
                playerCoins = newBalance
                tvDungeonsCoins.text = String.format("%.2f", playerCoins)
                purchasedDungeonIds.add(dungeon.firestoreId)
                
                Toast.makeText(this, "${dungeon.title} Unlocked Permanently!", Toast.LENGTH_SHORT).show()
                enterDungeon(dungeon)
            }
        } else {
            Toast.makeText(this, "Not enough coins!", Toast.LENGTH_SHORT).show()
        }
    }
}
