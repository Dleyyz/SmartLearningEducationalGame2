package com.example.smartlearningeducationalgame2

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// --- Data Models ---
data class QuestionModel(
    val id: String, 
    val text: String, 
    val answers: List<String>, 
    val correct: String, 
    val isBoss: Boolean = false
)

class ManageQuestionsActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var questFirestoreId: String? = null
    private var editingQuestionId: String? = null
    private var isDungeon: Boolean = false
    
    private lateinit var etQuestion: EditText
    private lateinit var etAns1: EditText
    private lateinit var etAns2: EditText
    private lateinit var etAns3: EditText
    private lateinit var etAns4: EditText
    private lateinit var etCorrect: EditText
    private lateinit var cbIsBoss: CheckBox
    private lateinit var btnSave: Button
    private lateinit var rvQuestions: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_manage_questions)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        questFirestoreId = intent.getStringExtra("QUEST_DOC_ID")
        val questTitle = intent.getStringExtra("QUEST_TITLE")
        isDungeon = intent.getBooleanExtra("IS_DUNGEON", false)

        findViewById<TextView>(R.id.tvQuestTitleHeader).text = "Manage: $questTitle"

        // Initialize UI Elements (Matching activity_manage_questions.xml exactly)
        etQuestion = findViewById(R.id.etQuestionText)
        etAns1 = findViewById(R.id.etAns1)
        etAns2 = findViewById(R.id.etAns2)
        etAns3 = findViewById(R.id.etAns3)
        etAns4 = findViewById(R.id.etAns4)
        etCorrect = findViewById(R.id.etCorrectAns)
        cbIsBoss = findViewById(R.id.cbIsBoss)
        btnSave = findViewById(R.id.btnSaveQuestion)
        rvQuestions = findViewById(R.id.rvQuestionsList)

        rvQuestions.layoutManager = LinearLayoutManager(this)

        setupNavigation()
        fetchQuestions()

        btnSave.setOnClickListener {
            saveOrUpdateQuestion()
        }
    }

    private fun setupNavigation() {
        findViewById<LinearLayout>(R.id.navQuest).setOnClickListener {
            startActivity(Intent(this, TeacherActivity::class.java))
            finish()
        }
        findViewById<LinearLayout>(R.id.navAvatar).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }
        findViewById<LinearLayout>(R.id.navClasses).setOnClickListener {
            startActivity(Intent(this, ClassesActivity::class.java))
            finish()
        }
        findViewById<LinearLayout>(R.id.navDungeons).setOnClickListener {
            startActivity(Intent(this, TeacherDungeonsActivity::class.java))
            finish()
        }
    }

    private fun fetchQuestions() {
        if (questFirestoreId == null) return
        
        val collectionName = if (isDungeon) "dungeons" else "quests"
        db.collection(collectionName).document(questFirestoreId!!)
            .collection("questions")
            .get()
            .addOnSuccessListener { result ->
                val questionList = mutableListOf<QuestionModel>()
                for (doc in result) {
                    val q = doc.toObject(FirestoreQuestion::class.java)
                    if (q != null) {
                        questionList.add(QuestionModel(doc.id, q.questionText, q.answers, q.correctAnswer, q.isBoss))
                    }
                }
                rvQuestions.adapter = QuestionsAdapter(questionList, { q -> editQuestion(q) }, { id -> deleteQuestion(id) })
            }
    }

    private fun saveOrUpdateQuestion() {
        val qText = etQuestion.text.toString().trim()
        val answers = listOf(
            etAns1.text.toString().trim(), 
            etAns2.text.toString().trim(), 
            etAns3.text.toString().trim(), 
            etAns4.text.toString().trim()
        )
        val correct = etCorrect.text.toString().trim()
        val isBoss = cbIsBoss.isChecked

        if (qText.isEmpty() || answers.any { it.isEmpty() } || correct.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val questionData = hashMapOf(
            "questionText" to qText, 
            "answers" to answers, 
            "correctAnswer" to correct,
            "isBoss" to isBoss,
            "expReward" to if (isBoss) 50 else 10,
            "coinReward" to if (isBoss) 5.0 else 1.0
        )

        val collectionName = if (isDungeon) "dungeons" else "quests"
        val collection = db.collection(collectionName).document(questFirestoreId!!).collection("questions")
        
        val task = if (editingQuestionId != null) {
            collection.document(editingQuestionId!!).set(questionData)
        } else {
            collection.add(questionData)
        }

        task.addOnSuccessListener {
            Toast.makeText(this, if (editingQuestionId != null) "Challenge Updated!" else "Challenge Saved!", Toast.LENGTH_SHORT).show()
            clearForm()
            fetchQuestions()
        }
    }

    private fun editQuestion(q: QuestionModel) {
        editingQuestionId = q.id
        etQuestion.setText(q.text)
        if (q.answers.size >= 4) {
            etAns1.setText(q.answers[0])
            etAns2.setText(q.answers[1])
            etAns3.setText(q.answers[2])
            etAns4.setText(q.answers[3])
        }
        etCorrect.setText(q.correct)
        cbIsBoss.isChecked = q.isBoss
        btnSave.text = "UPDATE CHALLENGE"
    }

    private fun deleteQuestion(id: String) {
        val collectionName = if (isDungeon) "dungeons" else "quests"
        db.collection(collectionName).document(questFirestoreId!!).collection("questions").document(id)
            .delete().addOnSuccessListener {
                Toast.makeText(this, "Challenge Deleted", Toast.LENGTH_SHORT).show()
                fetchQuestions()
            }
    }

    private fun clearForm() {
        editingQuestionId = null
        etQuestion.text.clear()
        etAns1.text.clear()
        etAns2.text.clear()
        etAns3.text.clear()
        etAns4.text.clear()
        etCorrect.text.clear()
        cbIsBoss.isChecked = false
        btnSave.text = "SAVE CHALLENGE"
    }
}

// --- Questions Adapter ---

class QuestionsAdapter(
    private val list: List<QuestionModel>,
    private val onEdit: (QuestionModel) -> Unit,
    private val onDelete: (String) -> Unit
) : RecyclerView.Adapter<QuestionsAdapter.ViewHolder>() {
    
    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val tvTitle: TextView = v.findViewById(R.id.tvQuestionTitle)
        val tvCorrect: TextView = v.findViewById(R.id.tvCorrectAnswerLabel)
        val tvBossIndicator: TextView = v.findViewById(R.id.tvBossIndicator)
        val btnEdit: ImageButton = v.findViewById(R.id.btnEditQuestion)
        val btnDel: ImageButton = v.findViewById(R.id.btnDeleteQuestion)
    }

    override fun onCreateViewHolder(p: ViewGroup, t: Int) = 
        ViewHolder(LayoutInflater.from(p.context).inflate(R.layout.item_question, p, false))

    override fun onBindViewHolder(h: ViewHolder, p: Int) {
        val q = list[p]
        h.tvTitle.text = q.text
        h.tvCorrect.text = "Correct: ${q.correct}"
        
        if (q.isBoss) {
            h.tvBossIndicator.visibility = View.VISIBLE
            h.tvBossIndicator.text = "🔥 BOSS QUESTION 🔥"
            h.tvBossIndicator.setTextColor(Color.RED)
        } else {
            h.tvBossIndicator.visibility = View.GONE
        }
        
        h.btnEdit.setOnClickListener { onEdit(q) }
        h.btnDel.setOnClickListener { onDelete(q.id) }
    }

    override fun getItemCount() = list.size
}
