package com.example.smartlearningeducationalgame2

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var layoutLoadingOverlay: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // UI Pieces
        val layoutStudent = findViewById<LinearLayout>(R.id.layoutStudentView)
        val layoutTeacher = findViewById<LinearLayout>(R.id.layoutTeacherView)
        layoutLoadingOverlay = findViewById(R.id.layoutLoadingOverlay)
        
        // Settings Button - Now navigates to SettingsActivity
        findViewById<ImageButton>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Show loading at start
        layoutLoadingOverlay.visibility = View.VISIBLE

        // Fetch user data and setup UI
        fetchUserProfile(layoutStudent, layoutTeacher)

        // Navigation
        findViewById<LinearLayout>(R.id.navQuest).setOnClickListener { checkRoleAndNavigate() }
        findViewById<LinearLayout>(R.id.navClasses).setOnClickListener {
            startActivity(Intent(this, ClassesActivity::class.java))
            overridePendingTransition(0, 0)
        }
        
        // Safety Check for Dungeons Access
        findViewById<LinearLayout>(R.id.navDungeons).setOnClickListener {
            db.collection("users").document(auth.currentUser?.uid ?: "").get().addOnSuccessListener { doc ->
                if ((doc.getString("role") ?: "Student").equals("Teacher", true)) {
                    startActivity(Intent(this, TeacherDungeonsActivity::class.java))
                    overridePendingTransition(0, 0)
                } else {
                    Toast.makeText(this, "Access Denied: Only teachers can access dungeons.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val layoutStudent = findViewById<LinearLayout>(R.id.layoutStudentView)
        val layoutTeacher = findViewById<LinearLayout>(R.id.layoutTeacherView)
        fetchUserProfile(layoutStudent, layoutTeacher)
    }

    private fun fetchUserProfile(studentPiece: LinearLayout, teacherPiece: LinearLayout) {
        val userId = auth.currentUser?.uid ?: return
        
        db.collection("users").document(userId).get().addOnSuccessListener { document ->
            if (isFinishing || isDestroyed) return@addOnSuccessListener
            layoutLoadingOverlay.visibility = View.GONE
            
            if (document.exists()) {
                val role = document.getString("role") ?: "Student"
                val name = document.getString("name") ?: "No Name"
                val email = document.getString("email") ?: ""

                findViewById<TextView>(R.id.tvProfileName).text = name
                findViewById<TextView>(R.id.tvProfileEmail).text = email
                findViewById<TextView>(R.id.tvProfileRole).text = role.uppercase()

                val ivProfile = findViewById<ImageView>(R.id.ivProfilePicture)
                val tvCoinDisplay = findViewById<TextView>(R.id.tvCoinDisplay)
                val navDungeons = findViewById<LinearLayout>(R.id.navDungeons)

                if (role.equals("Teacher", ignoreCase = true)) {
                    studentPiece.visibility = View.GONE
                    teacherPiece.visibility = View.VISIBLE
                    tvCoinDisplay?.visibility = View.GONE 
                    navDungeons?.visibility = View.VISIBLE
                    
                    ivProfile.setImageResource(R.drawable.teacher_photoicon)
                    ivProfile.setBackgroundResource(R.drawable.bg_avatar_box)
                    
                    val questsPublished = document.getLong("questsPublished") ?: 0L
                    val level = (questsPublished / 10) + 1
                    val success = (document.getLong("successRate") ?: 0).toInt()
                    
                    findViewById<TextView>(R.id.tvHeaderLevel).text = "★ TEACHER LVL $level"
                    findViewById<ProgressBar>(R.id.pbTeacherProfileSuccess).progress = success
                    findViewById<ProgressBar>(R.id.pbTeacherProfileAssignments).progress = (questsPublished.toInt() % 10) * 10
                    
                    findViewById<TextView>(R.id.tvClassesManaged).text = (document.getLong("classesCount") ?: 0).toString()
                    findViewById<TextView>(R.id.tvSuccessRate).text = "$success%"
                } else {
                    studentPiece.visibility = View.VISIBLE
                    teacherPiece.visibility = View.GONE
                    tvCoinDisplay?.visibility = View.VISIBLE 
                    navDungeons?.visibility = View.GONE
                    ivProfile.setImageResource(R.drawable.hero_player)

                    val level = document.getLong("level") ?: 1
                    findViewById<TextView>(R.id.tvHeaderLevel).text = "★ LEVEL $level"
                    
                    // Health Update
                    val health = (document.getLong("health") ?: 100).toInt()
                    findViewById<ProgressBar>(R.id.pbProfileHP).progress = health
                    findViewById<TextView>(R.id.tvHPValue).text = "$health / 100"
                    
                    // EXP Update
                    val expVal = (document.getLong("experience") ?: document.getLong("exp") ?: 0L)
                    val expProgress = (expVal % 100).toInt()
                    findViewById<ProgressBar>(R.id.pbProfileEXP).progress = expProgress
                    findViewById<TextView>(R.id.tvEXPValue).text = "$expProgress / 100"
                    
                    val coins = document.get("coins")?.let { 
                        if (it is Double) it.toLong() else it as? Long 
                    } ?: 0L
                    tvCoinDisplay?.text = "🪙 $coins Coins"

                    val quests = document.getLong("totalQuestions") ?: document.getLong("questsCompleted") ?: 0L
                    val mastery = document.getLong("correctAnswers") ?: document.getLong("mastery") ?: 0L
                    
                    findViewById<TextView>(R.id.tvTotalQuestions).text = quests.toString()
                    findViewById<TextView>(R.id.tvCorrectAnswers).text = mastery.toString()
                }
            }
        }.addOnFailureListener {
            layoutLoadingOverlay.visibility = View.GONE
            Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkRoleAndNavigate() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).get().addOnSuccessListener { doc ->
            if (isFinishing || isDestroyed) return@addOnSuccessListener
            val intent = if ((doc.getString("role") ?: "Student").equals("Teacher", true)) 
                         Intent(this, TeacherActivity::class.java) 
                         else Intent(this, MainActivity::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
            finish()
        }
    }
}
