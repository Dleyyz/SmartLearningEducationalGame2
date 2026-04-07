package com.example.smartlearningeducationalgame2

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.VideoView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class TutorialActivity : AppCompatActivity() {

    private lateinit var ivTutorialImage: ImageView
    private lateinit var vvTutorialVideo: VideoView
    private lateinit var pbVideoLoading: ProgressBar
    private lateinit var btnReplayVideo: ImageButton
    private lateinit var tvTutorialStepTitle: TextView
    private lateinit var tvTutorialDescription: TextView
    private lateinit var btnPrev: Button
    private lateinit var btnNext: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var currentStep = 0
    private var tutorialSteps = mutableListOf<TutorialStep>()
    private var lastVideoRes = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_tutorial)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        ivTutorialImage = findViewById(R.id.ivTutorialImage)
        vvTutorialVideo = findViewById(R.id.vvTutorialVideo)
        pbVideoLoading = findViewById(R.id.pbVideoLoading)
        btnReplayVideo = findViewById(R.id.btnReplayVideo)
        tvTutorialStepTitle = findViewById(R.id.tvTutorialStepTitle)
        tvTutorialDescription = findViewById(R.id.tvTutorialDescription)
        btnPrev = findViewById(R.id.btnTutorialPrev)
        btnNext = findViewById(R.id.btnTutorialNext)

        val role = intent.getStringExtra("USER_ROLE") ?: "Student"
        setupTutorialSteps(role)

        btnReplayVideo.setOnClickListener {
            btnReplayVideo.visibility = View.GONE
            vvTutorialVideo.seekTo(0)
            vvTutorialVideo.start()
        }

        updateStep()

        btnNext.setOnClickListener {
            if (currentStep < tutorialSteps.size - 1) {
                currentStep++
                updateStep()
            } else {
                finish()
            }
        }

        btnPrev.setOnClickListener {
            if (currentStep > 0) {
                currentStep--
                updateStep()
            }
        }
    }

    private fun setupTutorialSteps(role: String) {
        tutorialSteps.clear()
        
        if (role == "Teacher") {
            tutorialSteps.addAll(listOf(
                TutorialStep(
                    "Welcome, Master Guide",
                    "Numeria is a world where math is power. As a Teacher, you shape the journey of young heroes.",
                    R.drawable.teacher_photoicon
                ),
                TutorialStep(
                    "The Mission Hub",
                    "From your dashboard, you can see how many students are active and how they are progressing in their studies.",
                    R.drawable.tutorial_quest_item_preview
                ),
                TutorialStep(
                    "Crafting Quests",
                    "The 'Add Quest' section allows you to create challenges. Set a title, define rewards, and pick a difficulty.",
                    R.drawable.coins
                ),
                TutorialStep(
                    "The Knowledge Trial",
                    "Once a quest is created, tap it to add math questions. These are the spells students must cast to win!",
                    R.drawable.monster_villain
                ),
                TutorialStep(
                    "Assigning the Path",
                    "Assign your quests to specific classes. Your students will see these missions appear on their own boards.",
                    R.drawable.battle_bg
                ),
                TutorialStep(
                    "Story: The Wisdom Keepers",
                    "You are the keeper of light. Every quest you create helps push back the fog of ignorance from Numeria.",
                    R.drawable.numeria_world
                )
            ))
        } else {
            tutorialSteps.addAll(listOf(
                TutorialStep(
                    "Welcome to Numeria",
                    "Math is the ultimate magic in this realm. Every Hero must master it to survive!",
                    isVideo = true,
                    videoRes = R.raw.numeria_storyline
                ),
                TutorialStep(
                    "The Mission Board",
                    "All the trials your teachers have set for you appear here. Look for quests with rewards like +XP and Coins!",
                    R.drawable.tutorial_quest_item_preview
                ),
                TutorialStep(
                    "Ancient Dungeons",
                    "Switch to the Dungeons tab for tougher trials! Some floors are locked until you reach a higher level.",
                    R.drawable.dungeon_bg
                ),
                TutorialStep(
                    "The Battle Arena",
                    "This is where you fight! Use the 'Brain Challenge' to cast math spells. Keep an eye on your HP bar on the right!",
                    R.drawable.monster_villain
                ),
                TutorialStep(
                    "Hero's Profile",
                    "Your profile shows your journey's record. Track your level, mastery stars, and heroic stats here.",
                    R.drawable.hero_player
                ),
                TutorialStep(
                    "Hall of Fame",
                    "Check the Leaderboard to see the Top Explorers. Gain more XP to climb the ranks and be #1 in your class!",
                    R.drawable.ic_leaderboard_tutorial
                ),
                TutorialStep(
                    "Health & Energy",
                    "The colorful bars represent your Health and Experience. Level up by filling the Experience bar to 100!",
                    R.drawable.tutorial_health_bar_preview
                ),
                TutorialStep(
                    "Story: Reclaim the Light",
                    "Darkness has stolen the numbers of the world. By winning battles, you restore truth and light to Numeria!",
                    R.drawable.numeria_world
                )
            ))
        }
        
        tutorialSteps.add(
            TutorialStep(
                "Are you ready?",
                "Your journey starts now. May your mind be sharp and your courage never fail!",
                R.drawable.tutorial_charactericon
            )
        )
    }

    private fun updateStep() {
        if (tutorialSteps.isEmpty()) return
        val step = tutorialSteps[currentStep]
        tvTutorialStepTitle.text = step.title
        tvTutorialDescription.text = step.description
        
        // Hide replay button and loader by default when moving steps
        btnReplayVideo.visibility = View.GONE
        pbVideoLoading.visibility = View.GONE

        if (step.isVideo) {
            ivTutorialImage.visibility = View.GONE
            vvTutorialVideo.visibility = View.VISIBLE
            
            if (lastVideoRes != step.videoRes) {
                lastVideoRes = step.videoRes
                pbVideoLoading.visibility = View.VISIBLE
                val videoPath = "android.resource://" + packageName + "/" + step.videoRes
                vvTutorialVideo.setVideoURI(Uri.parse(videoPath))
                
                vvTutorialVideo.setOnPreparedListener { mp ->
                    pbVideoLoading.visibility = View.GONE
                    mp.isLooping = false // Don't loop, show replay button at end
                    vvTutorialVideo.start()
                }

                vvTutorialVideo.setOnCompletionListener {
                    btnReplayVideo.visibility = View.VISIBLE
                }
                
                vvTutorialVideo.setOnErrorListener { _, _, _ ->
                    pbVideoLoading.visibility = View.GONE
                    ivTutorialImage.visibility = View.VISIBLE
                    vvTutorialVideo.visibility = View.GONE
                    true
                }
            } else {
                // If returning to same video, restart it
                vvTutorialVideo.seekTo(0)
                vvTutorialVideo.start()
            }
        } else {
            if (vvTutorialVideo.isPlaying) {
                vvTutorialVideo.stopPlayback()
            }
            vvTutorialVideo.visibility = View.GONE
            ivTutorialImage.visibility = View.VISIBLE
            ivTutorialImage.setImageResource(step.imageRes)
            lastVideoRes = -1
        }

        btnPrev.visibility = if (currentStep == 0) View.INVISIBLE else View.VISIBLE
        btnNext.text = if (currentStep == tutorialSteps.size - 1) "ENTER NUMERIA" else "NEXT"
    }

    override fun onPause() {
        super.onPause()
        if (vvTutorialVideo.isPlaying) {
            vvTutorialVideo.pause()
        }
    }

    override fun onResume() {
        super.onResume()
        if (tutorialSteps.isNotEmpty() && tutorialSteps[currentStep].isVideo && btnReplayVideo.visibility == View.GONE) {
            vvTutorialVideo.start()
        }
    }

    data class TutorialStep(
        val title: String, 
        val description: String, 
        val imageRes: Int = 0, 
        val isVideo: Boolean = false,
        val videoRes: Int = 0
    )
}
