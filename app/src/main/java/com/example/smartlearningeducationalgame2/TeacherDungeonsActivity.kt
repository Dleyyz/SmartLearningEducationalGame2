package com.example.smartlearningeducationalgame2

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.smartlearningeducationalgame2.R

class TeacherDungeonsActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var rvManageDungeons: RecyclerView
    private lateinit var etDungeonTitle: EditText
    private lateinit var etRequiredLevel: EditText
    private lateinit var etEntryFee: EditText
    private lateinit var btnAddDungeon: Button
    private lateinit var btnCancelEdit: Button
    private lateinit var tvFormTitle: TextView

    private var dungeonList = mutableListOf<Dungeon>()
    private var editingDungeonId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_teacher_dungeons)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        etDungeonTitle = findViewById(R.id.etDungeonTitle)
        etRequiredLevel = findViewById(R.id.etRequiredLevel)
        etEntryFee = findViewById(R.id.etEntryFee)
        btnAddDungeon = findViewById(R.id.btnAddDungeon)
        btnCancelEdit = findViewById(R.id.btnCancelEdit)
        tvFormTitle = findViewById(R.id.tvFormTitle)
        rvManageDungeons = findViewById(R.id.rvManageDungeons)

        rvManageDungeons.layoutManager = LinearLayoutManager(this)

        fetchDungeons()

        btnAddDungeon.setOnClickListener {
            val title = etDungeonTitle.text.toString().trim()
            val levelStr = etRequiredLevel.text.toString().trim()
            val feeStr = etEntryFee.text.toString().trim()
            
            if (title.isNotEmpty() && levelStr.isNotEmpty() && feeStr.isNotEmpty()) {
                saveOrUpdateDungeon(title, levelStr.toInt(), feeStr.toDouble())
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }

        btnCancelEdit.setOnClickListener { resetForm() }

        // Navigation
        findViewById<LinearLayout>(R.id.navQuest).setOnClickListener {
            startActivity(Intent(this, TeacherActivity::class.java)); overridePendingTransition(0, 0)
        }
        findViewById<LinearLayout>(R.id.navAvatar).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java)); overridePendingTransition(0, 0)
        }
        findViewById<LinearLayout>(R.id.navClasses).setOnClickListener {
            startActivity(Intent(this, ClassesActivity::class.java)); overridePendingTransition(0, 0)
        }
        findViewById<LinearLayout>(R.id.navDungeons).setOnClickListener {
            fetchDungeons()
        }
    }

    private fun fetchDungeons() {
        db.collection("dungeons").get().addOnSuccessListener { result ->
            dungeonList.clear()
            for (document in result) {
                val dungeon = document.toObject(Dungeon::class.java)
                dungeon.firestoreId = document.id
                dungeonList.add(dungeon)
            }
            updateRecyclerView()
        }
    }

    private fun saveOrUpdateDungeon(title: String, level: Int, fee: Double) {
        val dungeonData = hashMapOf("title" to title, "requiredLevel" to level, "entryFee" to fee)
        
        val task = if (editingDungeonId != null) {
            db.collection("dungeons").document(editingDungeonId!!).set(dungeonData)
        } else {
            db.collection("dungeons").add(dungeonData)
        }

        task.addOnSuccessListener {
            Toast.makeText(this, if (editingDungeonId != null) "Dungeon Updated!" else "Dungeon Created!", Toast.LENGTH_SHORT).show()
            resetForm()
            fetchDungeons()
        }
    }

    private fun updateRecyclerView() {
        rvManageDungeons.adapter = TeacherDungeonAdapter(dungeonList, 
            onEdit = { d -> editDungeon(d) },
            onDelete = { d -> showDeleteConfirmation(d) },
            onManageQuestions = { d -> 
                val intent = Intent(this, ManageQuestionsActivity::class.java)
                intent.putExtra("QUEST_DOC_ID", d.firestoreId)
                intent.putExtra("QUEST_TITLE", d.title)
                intent.putExtra("IS_DUNGEON", true) // Flag to tell activity it's a dungeon
                startActivity(intent)
            }
        )
    }

    private fun editDungeon(d: Dungeon) {
        editingDungeonId = d.firestoreId
        etDungeonTitle.setText(d.title)
        etRequiredLevel.setText(d.requiredLevel.toString())
        etEntryFee.setText(d.entryFee.toString())
        tvFormTitle.text = "EDIT DUNGEON"
        btnAddDungeon.text = "UPDATE DUNGEON"
        btnCancelEdit.visibility = View.VISIBLE
    }

    private fun resetForm() {
        editingDungeonId = null
        etDungeonTitle.text.clear()
        etRequiredLevel.text.clear()
        etEntryFee.text.clear()
        tvFormTitle.text = "ADD NEW DUNGEON"
        btnAddDungeon.text = "CREATE DUNGEON"
        btnCancelEdit.visibility = View.GONE
    }

    private fun showDeleteConfirmation(dungeon: Dungeon) {
        AlertDialog.Builder(this).setTitle("Delete Dungeon").setMessage("Permanently delete ${dungeon.title}?")
            .setPositiveButton("Delete") { _, _ ->
                db.collection("dungeons").document(dungeon.firestoreId).delete().addOnSuccessListener {
                    Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
                    fetchDungeons()
                }
            }.setNegativeButton("Cancel", null).show()
    }
}

class TeacherDungeonAdapter(
    private val list: List<Dungeon>, 
    private val onEdit: (Dungeon) -> Unit, 
    private val onDelete: (Dungeon) -> Unit,
    private val onManageQuestions: (Dungeon) -> Unit
) : RecyclerView.Adapter<TeacherDungeonAdapter.ViewHolder>() {
    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val tvTitle: TextView = v.findViewById(R.id.tvDungeonName)
        val tvLevel: TextView = v.findViewById(R.id.tvRequiredLevel)
        val btnDel: ImageButton = v.findViewById(R.id.btnDeleteDungeon)
        val btnQuestions: ImageButton = v.findViewById(R.id.btnManageQuestions)
    }
    override fun onCreateViewHolder(p: ViewGroup, t: Int) = ViewHolder(android.view.LayoutInflater.from(p.context).inflate(R.layout.item_teacher_dungeon, p, false))
    override fun onBindViewHolder(h: ViewHolder, p: Int) {
        val d = list[p]
        h.tvTitle.text = "${d.title} ($${d.entryFee})"
        h.tvLevel.text = "Min Lvl: ${d.requiredLevel}"
        h.itemView.setOnClickListener { onEdit(d) }
        h.btnDel.setOnClickListener { onDelete(d) }
        h.btnQuestions.setOnClickListener { onManageQuestions(d) }
    }
    override fun getItemCount() = list.size
}
