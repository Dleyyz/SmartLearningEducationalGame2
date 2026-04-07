package com.example.smartlearningeducationalgame2

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.example.smartlearningeducationalgame2.R

class TeacherActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    
    private lateinit var rvTeacherQuests: RecyclerView
    private lateinit var etQuestTitle: EditText
    private lateinit var etQuestXp: EditText
    private lateinit var etQuestGold: EditText
    private lateinit var rgDifficulty: RadioGroup
    private lateinit var btnAddQuest: Button
    private lateinit var btnSelectClasses: Button
    
    private lateinit var tvTeacherLevel: TextView
    private lateinit var tvTeacherWelcome: TextView
    private lateinit var pbSuccessRate: ProgressBar
    private lateinit var pbAssignments: ProgressBar
    private lateinit var tvClassesCount: TextView

    private var questList = mutableListOf<Quest>()
    private var teacherClasses = mutableListOf<ClassModel>()
    private var selectedClassIds = mutableListOf<String>()
    private var assignedToClassIds = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_teacher)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Initialize UI Elements
        etQuestTitle = findViewById(R.id.etQuestTitle)
        etQuestXp = findViewById(R.id.etQuestXp)
        etQuestGold = findViewById(R.id.etQuestGold)
        rgDifficulty = findViewById(R.id.rgDifficulty)
        btnAddQuest = findViewById(R.id.btnAddQuest)
        rvTeacherQuests = findViewById(R.id.rvTeacherQuests)
        btnSelectClasses = findViewById(R.id.btnSelectClasses)
        
        tvTeacherLevel = findViewById(R.id.tvTeacherLevel)
        tvTeacherWelcome = findViewById(R.id.tvTeacherWelcome)
        pbSuccessRate = findViewById(R.id.pbSuccessRate)
        pbAssignments = findViewById(R.id.pbAssignments)
        tvClassesCount = findViewById(R.id.tvClassesCount)
        
        rvTeacherQuests.layoutManager = LinearLayoutManager(this)
        rvTeacherQuests.adapter = QuestAdapter(questList, { q -> }, null, null)

        loadTeacherData()

        btnSelectClasses.setOnClickListener { showClassSelectionDialog(null) }

        btnAddQuest.setOnClickListener {
            val title = etQuestTitle.text.toString().trim()
            val xpStr = etQuestXp.text.toString().trim()
            val goldStr = etQuestGold.text.toString().trim()
            
            if (title.isNotEmpty() && xpStr.isNotEmpty() && goldStr.isNotEmpty()) {
                val xp = xpStr.toIntOrNull() ?: 50
                val gold = goldStr.toDoubleOrNull() ?: 5.0
                val difficulty = when (rgDifficulty.checkedRadioButtonId) {
                    R.id.rbEasy -> "EASY"
                    R.id.rbHard -> "HARD"
                    else -> "NORMAL"
                }
                addNewQuest(title, selectedClassIds.toList(), xp, gold, difficulty)
            } else {
                Toast.makeText(this, "Please fill all quest details", Toast.LENGTH_SHORT).show()
            }
        }

        // --- NAVIGATION ---
        findViewById<LinearLayout>(R.id.navAvatar).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java)); overridePendingTransition(0, 0)
        }
        findViewById<LinearLayout>(R.id.navClasses).setOnClickListener {
            startActivity(Intent(this, ClassesActivity::class.java)); overridePendingTransition(0, 0)
        }
        findViewById<LinearLayout>(R.id.navDungeons).setOnClickListener {
            startActivity(Intent(this, TeacherDungeonsActivity::class.java)); overridePendingTransition(0, 0)
        }
    }

    private fun loadTeacherData() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).get().addOnSuccessListener { userDoc ->
            if (isFinishing || isDestroyed) return@addOnSuccessListener
            if (userDoc.exists()) {
                // Check for first time tutorial
                if (userDoc.getBoolean("isFirstTime") == true) {
                    showTeacherTutorialDialog(userId)
                }

                val name = userDoc.getString("name")?.uppercase() ?: "TEACHER"
                tvTeacherWelcome.text = name

                val questsPublished = userDoc.getLong("questsPublished") ?: 0L
                val level = (questsPublished / 10) + 1
                tvTeacherLevel.text = "Teacher Lvl $level"
                
                val success = (userDoc.getLong("successRate") ?: 0).toInt()
                pbSuccessRate.progress = success
                pbAssignments.progress = (questsPublished.toInt() % 10) * 10
                
                assignedToClassIds = (userDoc.get("classIds") as? List<String>)?.toMutableList() ?: mutableListOf()
                
                fetchTeacherClasses(userId)
                fetchQuests()
            }
        }
    }

    private fun showTeacherTutorialDialog(userId: String) {
        AlertDialog.Builder(this)
            .setTitle("📜 Welcome, Master Guide!")
            .setMessage("Welcome to Numeria! You have a vital role in guiding your students. Would you like to see how to manage quests and classes?")
            .setPositiveButton("LEARN THE SECRETS") { _, _ ->
                db.collection("users").document(userId).update("isFirstTime", false)
                val intent = Intent(this, TutorialActivity::class.java)
                intent.putExtra("USER_ROLE", "Teacher")
                startActivity(intent)
            }
            .setNegativeButton("I ALREADY KNOW", { _, _ ->
                db.collection("users").document(userId).update("isFirstTime", false)
            })
            .setCancelable(false)
            .show()
    }

    private fun showClassSelectionDialog(targetQuest: Quest?) {
        if (teacherClasses.isEmpty()) {
            Toast.makeText(this, "No classes found! Create one in the Classes tab.", Toast.LENGTH_LONG).show()
            return
        }

        val classNames = teacherClasses.map { it.name }.toTypedArray()
        val currentSelection = if (targetQuest != null) targetQuest.classIds?.toMutableList() ?: mutableListOf() else selectedClassIds
        val checkedItems = BooleanArray(teacherClasses.size) { index -> currentSelection.contains(teacherClasses[index].id) }

        AlertDialog.Builder(this)
            .setTitle(if (targetQuest == null) "Assign New Quest" else "Assign '${targetQuest.title}'")
            .setMultiChoiceItems(classNames, checkedItems) { _, which, isChecked ->
                val classId = teacherClasses[which].id
                if (isChecked) {
                    if (!currentSelection.contains(classId)) currentSelection.add(classId)
                } else {
                    currentSelection.remove(classId)
                }
            }
            .setPositiveButton("OK") { _, _ ->
                if (targetQuest == null) {
                    selectedClassIds = currentSelection
                    btnSelectClasses.text = "Classes Selected: ${selectedClassIds.size}"
                } else {
                    assignQuestToStudents(targetQuest, currentSelection)
                }
            }
            .setNegativeButton("Cancel", null).show()
    }

    private fun assignQuestToStudents(quest: Quest, classIds: List<String>) {
        db.collection("quests").document(quest.firestoreId).update("classIds", classIds)

        for (classId in classIds) {
            db.collection("users").whereArrayContains("classIds", classId).get().addOnSuccessListener { students ->
                for (student in students) {
                    val assignmentId = "${student.id}_${quest.firestoreId}"
                    val assignmentData = hashMapOf(
                        "studentId" to student.id,
                        "questId" to quest.firestoreId,
                        "title" to quest.title,
                        "status" to "assigned",
                        "rewardXp" to quest.rewardXp,
                        "rewardGold" to quest.rewardGold,
                        "difficulty" to quest.difficulty
                    )
                    db.collection("assignments").document(assignmentId).set(assignmentData)
                }
            }
        }
        Toast.makeText(this, "Quest updated for students!", Toast.LENGTH_SHORT).show()
        fetchQuests()
    }

    private fun fetchTeacherClasses(userId: String) {
        db.collection("classes").whereEqualTo("teacherId", userId).get().addOnSuccessListener { result ->
            teacherClasses.clear()
            for (doc in result) {
                // Filter: if teacher is assigned to specific classes, only show those
                if (assignedToClassIds.isNotEmpty() && !assignedToClassIds.contains(doc.id)) continue
                
                teacherClasses.add(ClassModel(doc.id, doc.getString("name") ?: "", 0))
            }
            tvClassesCount.text = "${teacherClasses.size} Classes"
        }
    }

    private fun fetchQuests() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("quests").whereEqualTo("teacherId", userId).get().addOnSuccessListener { result ->
            if (isFinishing || isDestroyed) return@addOnSuccessListener
            questList.clear()
            for (document in result) {
                val title = document.getString("title") ?: "Unnamed Quest"
                val cIds = document.get("classIds") as? List<String> ?: emptyList()
                val xp = document.getLong("rewardXp")?.toInt() ?: 50
                val gold = document.getDouble("rewardGold") ?: 5.0
                val diff = document.getString("difficulty") ?: "NORMAL"
                
                val q = Quest(title = title, classIds = cIds, rewardXp = xp, rewardGold = gold, difficulty = diff)
                q.firestoreId = document.id
                questList.add(q)
            }
            updateRecyclerView()
        }
    }

    private fun addNewQuest(title: String, classIds: List<String>, xp: Int, gold: Double, difficulty: String) {
        val userId = auth.currentUser?.uid ?: return
        val newQuest = Quest(
            title = title, 
            classIds = classIds, 
            teacherId = userId,
            rewardXp = xp,
            rewardGold = gold,
            difficulty = difficulty
        )
        db.collection("quests").add(newQuest).addOnSuccessListener { docRef ->
            db.collection("users").document(userId).update("questsPublished", FieldValue.increment(1))
            newQuest.firestoreId = docRef.id
            assignQuestToStudents(newQuest, classIds)
            Toast.makeText(this, "Quest Published!", Toast.LENGTH_SHORT).show()
            
            etQuestTitle.text.clear()
            etQuestXp.text.clear()
            etQuestGold.text.clear()
            rgDifficulty.check(R.id.rbNormal)
            selectedClassIds.clear()
            btnSelectClasses.text = "Assign to Classes"
            
            fetchQuests()
            loadTeacherData()
        }
    }

    private fun updateRecyclerView() {
        rvTeacherQuests.adapter = QuestAdapter(
            quests = questList,
            onQuestClick = { clickedQuest ->
                val intent = Intent(this, ManageQuestionsActivity::class.java)
                intent.putExtra("QUEST_DOC_ID", clickedQuest.firestoreId)
                intent.putExtra("QUEST_TITLE", clickedQuest.title)
                startActivity(intent)
            },
            onDeleteClick = { quest -> showDeleteQuestConfirmation(quest) },
            onAssignClick = { quest -> showClassSelectionDialog(quest) }
        )
    }

    private fun showDeleteQuestConfirmation(quest: Quest) {
        AlertDialog.Builder(this).setTitle("Delete Quest").setMessage("Are you sure?").setPositiveButton("Delete") { _, _ -> deleteQuest(quest) }.setNegativeButton("Cancel", null).show()
    }

    private fun deleteQuest(quest: Quest) {
        if (quest.firestoreId.isEmpty()) return
        db.collection("quests").document(quest.firestoreId).delete().addOnSuccessListener { fetchQuests() }
    }
}
