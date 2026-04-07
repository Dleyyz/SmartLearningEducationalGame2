package com.example.smartlearningeducationalgame2

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.CycleInterpolator
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class BattleActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var tvEnemyName: TextView
    private lateinit var pbEnemyHealth: ProgressBar
    private lateinit var tvPlayerName: TextView
    private lateinit var tvPlayerLevel: TextView
    private lateinit var tvPlayerHPText: TextView
    private lateinit var pbPlayerHealth: ProgressBar
    private lateinit var tvQuestionContent: TextView
    private lateinit var btnAnswer1: TextView
    private lateinit var btnAnswer2: TextView
    private lateinit var btnAnswer3: TextView
    private lateinit var btnAnswer4: TextView
    private lateinit var btnBattleRun: Button
    private lateinit var ivEnemySprite: ImageView
    private lateinit var ivPlayerSprite: ImageView
    private lateinit var ivBattleWallpaper: ImageView
    private lateinit var layoutLoadingOverlay: View
    private lateinit var viewLowHealthOverlay: View
    private lateinit var tvFeedbackText: TextView
    
    private lateinit var layoutFinishOverlay: RelativeLayout
    private lateinit var tvFinishResultTitle: TextView
    private lateinit var tvFinishRewards: TextView
    private lateinit var btnFinishBattle: Button

    private var enemyHP = 100
    private var playerHP = 100
    private var questionList: List<FirestoreQuestion> = emptyList()
    private var currentQuestionIndex = 0
    private var isBattleOver = false
    
    private var totalExpGained = 0L
    private var totalCoinsGained = 0.0
    private var totalQuestionsInThisBattle = 0
    private var correctAnswersCount = 0
    private var questId: String? = null
    private var isDungeon: Boolean = false
    private var lowHealthAnimator: ObjectAnimator? = null
    private var heartbeatMediaPlayer: MediaPlayer? = null
    private var battleMusic: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_battle)
        
        val musicIntent = Intent(this, MusicService::class.java).apply {
            action = MusicService.ACTION_PAUSE
        }
        startService(musicIntent)
        
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        tvEnemyName = findViewById(R.id.tvEnemyName)
        pbEnemyHealth = findViewById(R.id.pbEnemyHealth)
        tvPlayerName = findViewById(R.id.tvPlayerName)
        tvPlayerLevel = findViewById(R.id.tvPlayerLevel)
        tvPlayerHPText = findViewById(R.id.tvPlayerHPText)
        pbPlayerHealth = findViewById(R.id.pbPlayerHealth)
        
        tvQuestionContent = findViewById(R.id.tvQuestionContent)
        btnAnswer1 = findViewById(R.id.btnAnswer1)
        btnAnswer2 = findViewById(R.id.btnAnswer2)
        btnAnswer3 = findViewById(R.id.btnAnswer3)
        btnAnswer4 = findViewById(R.id.btnAnswer4)
        btnBattleRun = findViewById(R.id.btnBattleRun)
        ivEnemySprite = findViewById(R.id.ivEnemySprite)
        ivPlayerSprite = findViewById(R.id.ivPlayerSprite)
        ivBattleWallpaper = findViewById(R.id.ivBattleWallpaper)
        layoutLoadingOverlay = findViewById(R.id.layoutLoadingOverlay)
        viewLowHealthOverlay = findViewById(R.id.viewLowHealthOverlay)
        tvFeedbackText = findViewById(R.id.tvFeedbackText)
        
        layoutFinishOverlay = findViewById(R.id.layoutFinishOverlay)
        tvFinishResultTitle = findViewById(R.id.tvFinishResultTitle)
        tvFinishRewards = findViewById(R.id.tvFinishRewards)
        btnFinishBattle = findViewById(R.id.btnFinishBattle)

        val encounterName = intent.getStringExtra("ENCOUNTER_NAME") ?: "QUEST"
        questId = intent.getStringExtra("QUEST_DOC_ID")
        isDungeon = intent.getBooleanExtra("IS_DUNGEON", false)
        
        if (isDungeon) {
            ivBattleWallpaper.setImageResource(R.drawable.dungeon_bg)
        } else {
            ivBattleWallpaper.setImageResource(R.drawable.battle_bg)
        }

        val cleanedTopic = encounterName.replace("HOMEWORK", "", ignoreCase = true).replace("CHAPTER", "", ignoreCase = true).replace("LESSON", "", ignoreCase = true).replace(Regex("\\d+"), "").trim().uppercase()
        val epicPrefixes = listOf("ANCIENT", "LEGENDARY", "CORRUPTED", "MYTHIC", "ELDER", "PRIMAL")
        val monsterTypes = listOf("SHADOW", "DRAGON", "GOLEM", "PHANTOM", "TITAN", "WRAITH")
        val bossName = "THE ${epicPrefixes.random()} ${monsterTypes.random()} OF $cleanedTopic"
        tvEnemyName.text = bossName

        btnBattleRun.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Retreat from Battle?")
                .setMessage("If you retreat now, you will lose all rewards earned during this encounter. Do you want to head back?")
                .setPositiveButton("RETREAT") { _, _ ->
                    totalExpGained = 0L
                    totalCoinsGained = 0.0
                    endBattle()
                }
                .setNegativeButton("FIGHT ON", null)
                .show()
        }
        
        btnFinishBattle.setOnClickListener { finish() }

        fetchUserStats()

        if (questId != null) {
            fetchQuestions(questId!!)
        } else {
            layoutLoadingOverlay.visibility = View.GONE
            tvQuestionContent.text = "Error: Quest data missing."
        }
        
        startBattleMusic()
    }

    private fun startBattleMusic() {
        if (battleMusic == null) {
            battleMusic = MediaPlayer.create(this, R.raw.battleground_background)
            battleMusic?.isLooping = true
            battleMusic?.setVolume(1.0f, 1.0f)
        }
        battleMusic?.start()
    }

    private fun stopBattleMusic() {
        battleMusic?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        battleMusic = null
    }

    private fun playHitSound() {
        try {
            val mp = MediaPlayer.create(this, R.raw.stab_damage)
            mp.setOnCompletionListener { it.release() }
            mp.start()
        } catch (e: Exception) {
            Log.e("BattleSound", "Error playing sound", e)
        }
    }

    private fun playCorrectSound() {
        try {
            val mp = MediaPlayer.create(this, R.raw.correct)
            mp.setOnCompletionListener { it.release() }
            mp.start()
        } catch (e: Exception) {
            Log.e("BattleSound", "Error playing correct sound", e)
        }
    }

    private fun playVictorySound() {
        try {
            val mp = MediaPlayer.create(this, R.raw.yayvictory)
            mp.setOnCompletionListener { it.release() }
            mp.start()
        } catch (e: Exception) {
            Log.e("BattleSound", "Error playing victory sound", e)
        }
    }

    private fun playQuestWinSound() {
        try {
            val mp = MediaPlayer.create(this, R.raw.quest_win)
            mp.setOnCompletionListener { it.release() }
            mp.start()
        } catch (e: Exception) {
            Log.e("BattleSound", "Error playing quest win sound", e)
        }
    }

    private fun playQuestLoseSound() {
        try {
            val mp = MediaPlayer.create(this, R.raw.quest_lose)
            mp.setOnCompletionListener { it.release() }
            mp.start()
        } catch (e: Exception) {
            Log.e("BattleSound", "Error playing quest lose sound", e)
        }
    }

    private fun startHeartbeatSound() {
        if (heartbeatMediaPlayer == null) {
            heartbeatMediaPlayer = MediaPlayer.create(this, R.raw.heartbeat_lowhealth)
            heartbeatMediaPlayer?.isLooping = true
            heartbeatMediaPlayer?.setVolume(1.0f, 1.0f)
        }
        if (heartbeatMediaPlayer?.isPlaying == false) {
            heartbeatMediaPlayer?.start()
        }
    }

    private fun stopHeartbeatSound() {
        heartbeatMediaPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        heartbeatMediaPlayer = null
    }

    private fun fetchUserStats() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                val name = doc.getString("name")?.uppercase() ?: "EXPLORER"
                val level = doc.getLong("level") ?: 1
                tvPlayerName.text = name
                tvPlayerLevel.text = "Lv$level"
                
                playerHP = (doc.getLong("health") ?: 100).toInt()
                if (playerHP <= 0) {
                    Toast.makeText(this, "You have no health to battle!", Toast.LENGTH_LONG).show()
                    finish()
                }
                updatePlayerHPUI(false)
            }
        }
    }

    private fun updatePlayerHPUI(animate: Boolean) {
        if (animate) {
            ObjectAnimator.ofInt(pbPlayerHealth, "progress", playerHP).apply {
                duration = 500
                interpolator = AccelerateDecelerateInterpolator()
                start()
            }
        } else {
            pbPlayerHealth.progress = playerHP
        }
        tvPlayerHPText.text = "$playerHP / 100"
        checkLowHealthEffect()
    }

    private fun animateEnemyHP(target: Int) {
        ObjectAnimator.ofInt(pbEnemyHealth, "progress", target).apply {
            duration = 500
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    private fun checkLowHealthEffect() {
        if (playerHP in 1..50) {
            if (viewLowHealthOverlay.visibility != View.VISIBLE) {
                viewLowHealthOverlay.visibility = View.VISIBLE
                startLowHealthPulse()
                startHeartbeatSound()
            }
            battleMusic?.setVolume(0.5f, 0.5f)
            heartbeatMediaPlayer?.setVolume(1.0f, 1.0f)
        } else {
            viewLowHealthOverlay.visibility = View.GONE
            lowHealthAnimator?.cancel()
            stopHeartbeatSound()
            battleMusic?.setVolume(1.0f, 1.0f)
        }
    }

    private fun startLowHealthPulse() {
        if (lowHealthAnimator == null) {
            lowHealthAnimator = ObjectAnimator.ofFloat(viewLowHealthOverlay, "alpha", 0.3f, 0.8f)
            lowHealthAnimator?.duration = 800
            lowHealthAnimator?.repeatMode = ValueAnimator.REVERSE
            lowHealthAnimator?.repeatCount = ValueAnimator.INFINITE
        }
        if (lowHealthAnimator?.isRunning == false) {
            lowHealthAnimator?.start()
        }
    }

    private fun fetchQuestions(id: String) {
        val collectionName = if (isDungeon) "dungeons" else "quests"
        db.collection(collectionName).document(id).collection("questions").get().addOnSuccessListener { documents ->
            val fetchedQuestions = mutableListOf<FirestoreQuestion>()
            for (document in documents) {
                fetchedQuestions.add(document.toObject(FirestoreQuestion::class.java))
            }
            layoutLoadingOverlay.visibility = View.GONE
            if (fetchedQuestions.isEmpty()) {
                tvQuestionContent.text = "No questions found. Teacher needs to add some!"
            } else {
                this.questionList = fetchedQuestions
                this.currentQuestionIndex = 0
                
                // Logic: Monster HP is exactly the number of questions
                enemyHP = fetchedQuestions.size
                pbEnemyHealth.max = enemyHP
                pbEnemyHealth.progress = enemyHP
                
                loadNextQuestion()
            }
        }.addOnFailureListener {
            layoutLoadingOverlay.visibility = View.GONE
            Toast.makeText(this, "Failed to load battle.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun loadNextQuestion() {
        if (isBattleOver) return
        if (currentQuestionIndex >= questionList.size) {
            endBattle()
            return
        }
        val currentQ = questionList[currentQuestionIndex]
        
        if (currentQ.isBoss) {
            tvQuestionContent.text = "🔥 BOSS CHALLENGE! 🔥\n\n${currentQ.questionText}"
        } else {
            tvQuestionContent.text = currentQ.questionText
        }
        
        val shuffledAnswers = currentQ.answers.shuffled()
        val buttons = listOf(btnAnswer1, btnAnswer2, btnAnswer3, btnAnswer4)
        for (i in 0 until buttons.size) {
            if (i < shuffledAnswers.size) {
                buttons[i].text = shuffledAnswers[i]
                buttons[i].setOnClickListener { checkAnswer(shuffledAnswers[i], currentQ) }
            } else {
                buttons[i].text = ""
                buttons[i].setOnClickListener(null)
            }
        }
    }

    private fun checkAnswer(selected: String, question: FirestoreQuestion) {
        if (isBattleOver) return
        totalQuestionsInThisBattle++
        
        if (selected == question.correctAnswer) {
            showFeedback(true)
            playCorrectSound()
            correctAnswersCount++
            totalExpGained += question.expReward
            totalCoinsGained += question.coinReward
            
            // Damage is now 1 hit per question
            val damage = 1
            enemyHP -= damage
            if (enemyHP < 0) enemyHP = 0
            animateEnemyHP(enemyHP)
            playHitSound()
            animateEnemyHit()
        } else {
            showFeedback(false)
            val penalty = if (question.isBoss) 10 else 5
            playerHP -= penalty
            if (playerHP < 0) playerHP = 0
            updatePlayerHPUI(true)
            playHitSound()
            animatePlayerHit()
        }
        
        if (playerHP <= 0 || enemyHP <= 0) {
            tvQuestionContent.postDelayed({ endBattle() }, 1000)
        } else {
            currentQuestionIndex++
            tvQuestionContent.postDelayed({ loadNextQuestion() }, 1000)
        }
    }

    private fun showFeedback(correct: Boolean) {
        tvFeedbackText.visibility = View.VISIBLE
        tvFeedbackText.text = if (correct) "CORRECT!" else "WRONG!"
        tvFeedbackText.setTextColor(if (correct) Color.GREEN else Color.RED)
        tvFeedbackText.alpha = 0f
        tvFeedbackText.scaleX = 0.5f
        tvFeedbackText.scaleY = 0.5f

        tvFeedbackText.animate()
            .alpha(1f)
            .scaleX(1.2f)
            .scaleY(1.2f)
            .setDuration(300)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    tvFeedbackText.animate()
                        .alpha(0f)
                        .setStartDelay(400)
                        .setDuration(300)
                        .setListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                tvFeedbackText.visibility = View.GONE
                            }
                        })
                }
            })
    }

    private fun animateEnemyHit() {
        val shake = ObjectAnimator.ofFloat(ivEnemySprite, "translationX", 0f, 25f)
        shake.duration = 100
        shake.interpolator = CycleInterpolator(3f)
        shake.start()
        
        ivEnemySprite.setColorFilter(Color.RED, android.graphics.PorterDuff.Mode.SRC_ATOP)
        ivEnemySprite.postDelayed({ ivEnemySprite.clearColorFilter() }, 200)
    }

    private fun animatePlayerHit() {
        val shake = ObjectAnimator.ofFloat(ivPlayerSprite, "translationX", 0f, -25f)
        shake.duration = 100
        shake.interpolator = CycleInterpolator(3f)
        shake.start()
        
        ivPlayerSprite.setColorFilter(Color.RED, android.graphics.PorterDuff.Mode.SRC_ATOP)
        ivPlayerSprite.postDelayed({ ivPlayerSprite.clearColorFilter() }, 200)
    }

    private fun endBattle() {
        if (isBattleOver) return
        isBattleOver = true
        lowHealthAnimator?.cancel()
        viewLowHealthOverlay.visibility = View.GONE
        stopHeartbeatSound()
        stopBattleMusic()
        
        val victory = enemyHP <= 0 && playerHP > 0
        saveBattleResults(victory)
        
        btnBattleRun.visibility = View.GONE
        
        if (playerHP <= 0) {
            tvFinishResultTitle.text = "DEFEATED"
            tvFinishResultTitle.setTextColor(Color.RED)
            tvFinishRewards.text = "You have fallen in battle.\nRecuperate for 1 hour to try again."
            playQuestLoseSound()
        } else {
            if (victory) {
                if (correctAnswersCount == questionList.size) {
                    playQuestWinSound()
                } else {
                    playVictorySound()
                }
            } else {
                playQuestLoseSound()
            }
            tvFinishResultTitle.text = if (victory) "VICTORY!" else "QUEST FAILED"
            tvFinishResultTitle.setTextColor(if (victory) Color.parseColor("#4CAF50") else Color.RED)
            tvFinishRewards.text = "REWARDS:\n+$totalExpGained EXP\n+$$totalCoinsGained Coins"
        }
        
        layoutFinishOverlay.visibility = View.VISIBLE
    }
    
    private fun saveBattleResults(victory: Boolean) {
        val userId = auth.currentUser?.uid ?: return
        val userRef = db.collection("users").document(userId)
        val expToSave = if (playerHP > 0) totalExpGained else 0L
        val coinsToSave = if (playerHP > 0) totalCoinsGained else 0.0

        userRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val classId = document.getString("classId")
                val totalExp = document.getLong("totalExperience") ?: 0L
                val currentExp = document.getLong("experience") ?: 0L
                val currentLevel = document.getLong("level") ?: 1L
                val currentCoins = document.getDouble("coins") ?: 0.0
                
                val newTotalExp = totalExp + expToSave
                var newCurrentExp = currentExp + expToSave
                var newLevel = currentLevel
                
                while (newCurrentExp >= 100) {
                    newCurrentExp -= 100
                    newLevel += 1
                }
                
                val updates = hashMapOf<String, Any>(
                    "totalExperience" to newTotalExp,
                    "experience" to newCurrentExp,
                    "level" to newLevel,
                    "coins" to currentCoins + coinsToSave,
                    "totalQuestions" to FieldValue.increment(totalQuestionsInThisBattle.toLong()),
                    "health" to playerHP
                )
                if (playerHP <= 0) updates["lastHealthReset"] = FieldValue.serverTimestamp()
                if (newLevel > currentLevel) updates["health"] = 100
                if (victory && questId != null && !isDungeon) {
                    val assignmentId = "${userId}_${questId}"
                    db.collection("assignments").document(assignmentId).update("status", "completed")
                }
                userRef.update(updates)
                if (classId != null) { updateTeacherStats(classId) }
            }
        }
    }

    private fun updateTeacherStats(classId: String) {
        db.collection("classes").document(classId).get().addOnSuccessListener { classDoc ->
            val teacherId = classDoc.getString("teacherId")
            if (teacherId != null) {
                val teacherRef = db.collection("users").document(teacherId)
                teacherRef.update("totalStudentAnswers", FieldValue.increment(totalQuestionsInThisBattle.toLong()))
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        lowHealthAnimator?.cancel()
        stopHeartbeatSound()
        stopBattleMusic()
        val musicIntent = Intent(this, MusicService::class.java).apply {
            action = MusicService.ACTION_RESUME
        }
        startService(musicIntent)
    }
}
