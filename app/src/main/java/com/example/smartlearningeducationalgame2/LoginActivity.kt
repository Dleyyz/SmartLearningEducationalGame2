package com.example.smartlearningeducationalgame2

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore
import com.example.smartlearningeducationalgame2.R

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    
    private lateinit var tilEmail: TextInputLayout
    private lateinit var etEmail: TextInputEditText
    
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etPassword: TextInputEditText
    
    private lateinit var btnLogin: Button
    private lateinit var loadingOverlay: View
    private lateinit var tvLoadingMessage: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        // Ensure Intro/Login music is playing
        val musicIntent = Intent(this, MusicService::class.java).apply {
            action = MusicService.ACTION_START
            putExtra(MusicService.EXTRA_SONG_RES_ID, R.raw.music_intrologin)
        }
        startService(musicIntent)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialize Views
        tilEmail = findViewById(R.id.tilEmail)
        etEmail = findViewById(R.id.etEmail)
        
        tilPassword = findViewById(R.id.tilPassword)
        etPassword = findViewById(R.id.etPassword)
        
        btnLogin = findViewById(R.id.btnLogin)
        val tvRegister = findViewById<TextView>(R.id.tvRegister)
        loadingOverlay = findViewById(R.id.layoutLoginLoading)
        tvLoadingMessage = findViewById(R.id.tvLoadingMessage)

        setupValidation()

        tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        btnLogin.setOnClickListener {
            performLogin()
        }
    }

    private fun setupValidation() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Clear errors as user types to provide a clean state
                tilEmail.error = null
                tilPassword.error = null
                
                updateButtonState()
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        etEmail.addTextChangedListener(watcher)
        etPassword.addTextChangedListener(watcher)
        
        // Ensure button is pressable from the start
        updateButtonState()
    }

    private fun updateButtonState() {
        // Button is always enabled so user can press it even if fields are empty
        btnLogin.isEnabled = true
        btnLogin.alpha = 1.0f
    }

    private fun performLogin() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val shake = AnimationUtils.loadAnimation(this, R.anim.shake)
        var hasError = false

        // Check Email
        if (email.isEmpty()) {
            tilEmail.error = "Email is required"
            tilEmail.startAnimation(shake)
            hasError = true
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "Invalid email format"
            tilEmail.startAnimation(shake)
            hasError = true
        }

        // Check Password
        if (password.isEmpty()) {
            tilPassword.error = "Password is required"
            tilPassword.startAnimation(shake)
            hasError = true
        }

        if (hasError) return

        // Disable button during network request to prevent multiple taps
        btnLogin.isEnabled = false
        btnLogin.text = "ENTERING..."
        tvLoadingMessage.text = "PREPARING JOURNEY..."
        loadingOverlay.visibility = View.VISIBLE

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    tvLoadingMessage.text = "JOURNEY FOUND!"
                    checkUserRole()
                } else {
                    loadingOverlay.visibility = View.GONE
                    btnLogin.isEnabled = true
                    btnLogin.text = "CONTINUE QUEST"
                    handleLoginError(task.exception)
                }
            }
    }

    private fun handleLoginError(exception: Exception?) {
        val shake = AnimationUtils.loadAnimation(this, R.anim.shake)
        
        when (exception) {
            is FirebaseAuthInvalidUserException -> {
                tilEmail.error = "Invalid email"
                tilEmail.startAnimation(shake)
            }
            is FirebaseAuthInvalidCredentialsException -> {
                // If the error message from Firebase indicates a bad user, show "Invalid email"
                // Otherwise, show "Wrong password"
                if (exception.message?.contains("user", ignoreCase = true) == true) {
                    tilEmail.error = "Invalid email"
                    tilEmail.startAnimation(shake)
                } else {
                    tilPassword.error = "Wrong password, try again"
                    tilPassword.startAnimation(shake)
                }
            }
            else -> {
                Toast.makeText(this, "Login failed. Check your connection.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkUserRole() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).get().addOnSuccessListener { document ->
            if (document.exists()) {
                val role = document.getString("role") ?: "Student"
                
                // Switch to Main Background Song before navigating
                val musicIntent = Intent(this, MusicService::class.java).apply {
                    action = MusicService.ACTION_START
                    putExtra(MusicService.EXTRA_SONG_RES_ID, R.raw.main_backgroundsong)
                }
                startService(musicIntent)

                val intent = if (role.equals("Teacher", true)) {
                    Intent(this, TeacherActivity::class.java)
                } else {
                    Intent(this, MainActivity::class.java)
                }
                startActivity(intent)
                finish()
            } else {
                loadingOverlay.visibility = View.GONE
                btnLogin.isEnabled = true
                btnLogin.text = "CONTINUE QUEST"
                Toast.makeText(this, "Hero data not found.", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            loadingOverlay.visibility = View.GONE
            btnLogin.isEnabled = true
            btnLogin.text = "CONTINUE QUEST"
            Toast.makeText(this, "Connection lost in the void.", Toast.LENGTH_SHORT).show()
        }
    }
}
