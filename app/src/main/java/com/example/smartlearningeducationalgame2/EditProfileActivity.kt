package com.example.smartlearningeducationalgame2

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EditProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var layoutLoading: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_edit_profile)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        layoutLoading = findViewById(R.id.layoutUpdateLoading)

        val etName = findViewById<EditText>(R.id.etEditName)
        val etEmail = findViewById<EditText>(R.id.etEditEmail)
        val etPassword = findViewById<EditText>(R.id.etEditPassword)
        val etConfirmPassword = findViewById<EditText>(R.id.etEditConfirmPassword)
        val btnUpdate = findViewById<Button>(R.id.btnUpdateProfile)
        val tvBack = findViewById<TextView>(R.id.tvBackToProfile)

        // Load current data
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                etName.setText(doc.getString("name"))
                etEmail.setText(doc.getString("email"))
            }
        }

        btnUpdate.setOnClickListener {
            val newName = etName.text.toString().trim()
            val newPassword = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            if (newName.isEmpty()) {
                Toast.makeText(this, "Hero Name cannot be empty!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            layoutLoading.visibility = View.VISIBLE

            // Step 1: Update Firestore Name
            db.collection("users").document(userId).update("name", newName)
                .addOnSuccessListener {
                    // Step 2: Check if password change is requested
                    if (newPassword.isNotEmpty()) {
                        if (newPassword.length < 6) {
                            layoutLoading.visibility = View.GONE
                            Toast.makeText(this, "Password must be at least 6 characters.", Toast.LENGTH_SHORT).show()
                        } else if (newPassword != confirmPassword) {
                            layoutLoading.visibility = View.GONE
                            Toast.makeText(this, "Passwords do not match.", Toast.LENGTH_SHORT).show()
                        } else {
                            // Update Firebase Auth Password
                            auth.currentUser?.updatePassword(newPassword)
                                ?.addOnCompleteListener { task ->
                                    layoutLoading.visibility = View.GONE
                                    if (task.isSuccessful) {
                                        Toast.makeText(this, "Profile and Password Updated!", Toast.LENGTH_SHORT).show()
                                        finish()
                                    } else {
                                        Toast.makeText(this, "Name updated, but password failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                        }
                    } else {
                        // Only name was updated
                        layoutLoading.visibility = View.GONE
                        Toast.makeText(this, "Profile Updated!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
                .addOnFailureListener {
                    layoutLoading.visibility = View.GONE
                    Toast.makeText(this, "Failed to update profile.", Toast.LENGTH_SHORT).show()
                }
        }

        tvBack.setOnClickListener { finish() }
    }
}
