package com.example.smartlearningeducationalgame2

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.example.smartlearningeducationalgame2.R

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        // Ensure Intro/Login music is playing
        val musicIntent = Intent(this, MusicService::class.java).apply {
            action = MusicService.ACTION_START
            putExtra(MusicService.EXTRA_SONG_RES_ID, R.raw.music_intrologin)
        }
        startService(musicIntent)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val etRegisterName = findViewById<EditText>(R.id.etRegisterName)
        val etRegisterEmail = findViewById<EditText>(R.id.etRegisterEmail)
        val etRegisterPassword = findViewById<EditText>(R.id.etRegisterPassword)
        val etRegisterConfirmPassword = findViewById<EditText>(R.id.etRegisterConfirmPassword)
        val rgRole = findViewById<RadioGroup>(R.id.rgRole)
        val tvBackToLogin = findViewById<TextView>(R.id.tvBackToLogin)
        val btnRegister = findViewById<Button>(R.id.btnRegister)

        tvBackToLogin.setOnClickListener {
            finish()
        }

        btnRegister.setOnClickListener {
            val name = etRegisterName.text.toString().trim()
            val email = etRegisterEmail.text.toString().trim()
            val password = etRegisterPassword.text.toString().trim()
            val confirmPassword = etRegisterConfirmPassword.text.toString().trim()
            
            val selectedRoleId = rgRole.checkedRadioButtonId
            val role = if (selectedRoleId == R.id.rbTeacher) "Teacher" else "Student"

            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill out all fields.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        saveUserToFirestore(name, email, role)
                    } else {
                        val exception = task.exception
                        if (exception is FirebaseAuthUserCollisionException) {
                            etRegisterEmail.error = "This email is already registered as a hero!"
                            Toast.makeText(this, "Email already in use. Please login instead.", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this, "Registration failed: ${exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
        }
    }

    private fun saveUserToFirestore(name: String, email: String, role: String) {
        val userId = auth.currentUser?.uid ?: return
        
        val userProfile = hashMapOf(
            "uid" to userId,
            "name" to name,
            "email" to email,
            "role" to role,
            "level" to 1,
            "health" to 100,
            "experience" to 0,
            "totalExperience" to 0L,
            "coins" to 0.0,
            "completedQuests" to arrayListOf<String>(),
            "classIds" to arrayListOf<String>(),
            "totalQuestions" to 0,
            "correctAnswers" to 0,
            "successRate" to 0,
            "classesCount" to 0,
            "assignmentsCount" to 0,
            "lastHealthReset" to FieldValue.serverTimestamp(),
            "isFirstTime" to true // Flag for tutorial
        )

        db.collection("users").document(userId)
            .set(userProfile)
            .addOnSuccessListener {
                Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()
                
                // Switch to Main Background Song before navigating
                val musicIntent = Intent(this, MusicService::class.java).apply {
                    action = MusicService.ACTION_START
                    putExtra(MusicService.EXTRA_SONG_RES_ID, R.raw.main_backgroundsong)
                }
                startService(musicIntent)

                navigateToMain(role)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to save profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun navigateToMain(role: String) {
        val intent = if (role == "Teacher") {
            Intent(this, TeacherActivity::class.java) 
        } else {
            Intent(this, MainActivity::class.java)
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}
