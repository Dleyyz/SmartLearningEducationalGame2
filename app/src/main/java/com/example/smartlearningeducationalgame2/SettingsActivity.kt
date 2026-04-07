package com.example.smartlearningeducationalgame2

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.cardview.widget.CardView
import com.google.firebase.auth.FirebaseAuth

class SettingsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)

        auth = FirebaseAuth.getInstance()

        val sharedPref = getSharedPreferences("game_settings", Context.MODE_PRIVATE)
        val switchMusic = findViewById<SwitchCompat>(R.id.switchMusic)
        
        // Load saved state (default is true)
        switchMusic.isChecked = sharedPref.getBoolean("music_enabled", true)

        switchMusic.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean("music_enabled", isChecked).apply()
            
            val intent = Intent(this, MusicService::class.java)
            if (isChecked) {
                intent.action = MusicService.ACTION_RESUME
                startService(intent)
            } else {
                intent.action = MusicService.ACTION_PAUSE
                startService(intent)
            }
        }

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        findViewById<CardView>(R.id.cardEditProfile).setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        findViewById<CardView>(R.id.cardHelpSupport).setOnClickListener {
            startActivity(Intent(this, HelpSupportActivity::class.java))
        }

        findViewById<CardView>(R.id.cardLogout).setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Exit Numeria")
            .setMessage("Are you sure you want to log out of your hero journey?")
            .setPositiveButton("Logout") { _, _ ->
                auth.signOut()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
