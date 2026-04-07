package com.example.smartlearningeducationalgame2

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class IntroActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start Intro Music
        val musicIntent = Intent(this, MusicService::class.java).apply {
            action = MusicService.ACTION_START
            putExtra(MusicService.EXTRA_SONG_RES_ID, R.raw.music_intrologin)
        }
        startService(musicIntent)

        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        if (currentUser != null) {
            checkUserRoleAndNavigate(currentUser.uid)
            return 
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_intro)

        val btnGetStarted = findViewById<Button>(R.id.btnGetStarted)
        btnGetStarted.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    private fun checkUserRoleAndNavigate(uid: String) {
        setContentView(R.layout.activity_intro) 
        findViewById<Button>(R.id.btnGetStarted).visibility = View.INVISIBLE
        
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val role = document.getString("role")
                    val intent = if (role == "Teacher") {
                        Intent(this, TeacherActivity::class.java)
                    } else {
                        Intent(this, MainActivity::class.java)
                    }
                    startActivity(intent)
                    finish()
                } else {
                    authSignOutAndLogin()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Connection error. Please try again.", Toast.LENGTH_LONG).show()
                authSignOutAndLogin()
            }
    }

    private fun authSignOutAndLogin() {
        FirebaseAuth.getInstance().signOut()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
