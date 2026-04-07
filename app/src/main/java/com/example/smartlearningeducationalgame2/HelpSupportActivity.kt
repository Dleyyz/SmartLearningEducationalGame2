package com.example.smartlearningeducationalgame2

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.setMargins
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HelpSupportActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var userRole: String = "Student"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_help_support)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        fetchUserRole()
        setupQuickActions()
        setupFAQ()
        
        findViewById<Button>(R.id.btnContactSupportBottom).setOnClickListener {
            showContactDialog()
        }
    }

    private fun fetchUserRole() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).get().addOnSuccessListener { doc ->
            userRole = doc.getString("role") ?: "Student"
        }
    }

    private fun setupQuickActions() {
        findViewById<CardView>(R.id.cardHowToPlay).setOnClickListener {
            val intent = Intent(this, TutorialActivity::class.java)
            intent.putExtra("USER_ROLE", userRole)
            startActivity(intent)
        }

        findViewById<CardView>(R.id.cardContactSupport).setOnClickListener {
            showContactDialog()
        }
    }

    private fun setupFAQ() {
        val faqContainer = findViewById<LinearLayout>(R.id.layoutFAQContainer)
        val faqs = listOf(
            "How do I play the game?" to "Explore the map, click on quest markers to start battles! Solve math problems to defeat monsters.",
            "How do I gain XP?" to "Winning battles and completing quests grants you experience points (XP).",
            "What happens if I answer wrong?" to "If you answer incorrectly, the monster will attack you and you will lose some health!",
            "How do I level up?" to "Collect enough XP to level up! Higher levels make you a stronger hero."
        )

        for (faq in faqs) {
            val card = createFAQCard(faq.first, faq.second)
            faqContainer.addView(card)
        }
    }

    private fun createFAQCard(question: String, answer: String): View {
        val card = CardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 20) }
            radius = 24f
            cardElevation = 8f
            setCardBackgroundColor(Color.WHITE)
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            padding(20)
        }

        val qLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }

        val qIcon = TextView(this).apply { text = "❓ "; textSize = 22.sp }
        val qText = TextView(this).apply {
            text = question
            textSize = 20.sp
            setTextColor(Color.parseColor("#333333"))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        qLayout.addView(qIcon)
        qLayout.addView(qText)

        val aLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            visibility = View.GONE
            setPadding(0, 16, 0, 0)
        }

        val aIcon = TextView(this).apply { text = "💡 "; textSize = 20.sp }
        val aText = TextView(this).apply {
            text = answer
            textSize = 16.sp
            setTextColor(Color.parseColor("#757575"))
            setLineSpacing(4f, 1.1f)
        }

        aLayout.addView(aIcon)
        aLayout.addView(aText)

        layout.addView(qLayout)
        layout.addView(aLayout)
        card.addView(layout)

        card.setOnClickListener {
            aLayout.visibility = if (aLayout.visibility == View.GONE) View.VISIBLE else View.GONE
        }

        return card
    }

    private fun showContactDialog() {
        val editText = EditText(this).apply {
            hint = "Write your message to the guild masters..."
            minLines = 3
            textSize = 18f
            gravity = android.view.Gravity.TOP
            setPadding(32, 32, 32, 32)
        }

        AlertDialog.Builder(this)
            .setTitle("📩 Contact Support")
            .setMessage("Found a bug or have a suggestion?")
            .setView(editText)
            .setPositiveButton("SEND") { _, _ ->
                val message = editText.text.toString().trim()
                if (message.isNotEmpty()) {
                    sendMessageToDev(message)
                }
            }
            .setNegativeButton("CANCEL", null)
            .show()
    }

    private fun sendMessageToDev(comment: String) {
        val userId = auth.currentUser?.uid ?: "Anonymous"
        val feedback = hashMapOf(
            "userId" to userId,
            "comment" to comment,
            "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            "userName" to (auth.currentUser?.displayName ?: "Explorer")
        )

        db.collection("support_messages")
            .add(feedback)
            .addOnSuccessListener {
                Toast.makeText(this, "Message sent to the Guild! Thank you, hero.", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to send message. Try again later.", Toast.LENGTH_SHORT).show()
            }
    }

    // Helper extensions
    private fun View.padding(dp: Int) {
        val p = (dp * resources.displayMetrics.density).toInt()
        setPadding(p, p, p, p)
    }
    private val Int.sp get() = this.toFloat()
}
