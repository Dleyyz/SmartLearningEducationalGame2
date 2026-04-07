package com.example.smartlearningeducationalgame2

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import java.text.NumberFormat
import java.util.Locale

// --- Data Models ---
data class StudentRank(val name: String, val level: Int, val exp: Int, val totalExp: Long, val uid: String)
data class SchoolClass(val id: String, val name: String)

class ClassesActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    
    private lateinit var rvLeaderboard: RecyclerView
    private lateinit var sectionTeacherCreate: LinearLayout
    private lateinit var rgLeaderboardToggle: RadioGroup
    private lateinit var rbMyClass: RadioButton
    private lateinit var layoutLoadingOverlay: View
    private lateinit var spinnerClassFilter: Spinner
    private lateinit var tvEmptyMessage: TextView
    private lateinit var btnAddStudent: Button
    
    private var currentUserRole: String = "Student"
    private val managedClasses = mutableListOf<SchoolClass>()
    private var selectedClassId: String? = null
    private var assignedClassIds: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_classes)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        rvLeaderboard = findViewById(R.id.rvLeaderboard)
        sectionTeacherCreate = findViewById(R.id.sectionTeacherCreateClass)
        rgLeaderboardToggle = findViewById(R.id.rgLeaderboardToggle)
        rbMyClass = findViewById(R.id.rbMyClass)
        layoutLoadingOverlay = findViewById(R.id.layoutLoadingOverlay)
        spinnerClassFilter = findViewById(R.id.spinnerClassFilter)
        tvEmptyMessage = findViewById(R.id.tvEmptyMessage)
        btnAddStudent = findViewById(R.id.btnAddStudent)
        
        rvLeaderboard.layoutManager = LinearLayoutManager(this)

        // Safety Timeout: Hide loading after 10 seconds no matter what
        Handler(Looper.getMainLooper()).postDelayed({
            if (layoutLoadingOverlay.visibility == View.VISIBLE) {
                layoutLoadingOverlay.visibility = View.GONE
                Toast.makeText(this, "Connection timed out. Check Firebase Rules/AppCheck.", Toast.LENGTH_LONG).show()
            }
        }, 10000)

        checkUserRoleAndSetupUI()

        findViewById<LinearLayout>(R.id.navQuest).setOnClickListener { checkRoleAndNavigate() }
        findViewById<LinearLayout>(R.id.navAvatar).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            overridePendingTransition(0, 0)
        }
        findViewById<LinearLayout>(R.id.navDungeons).setOnClickListener {
            startActivity(Intent(this, TeacherDungeonsActivity::class.java))
            overridePendingTransition(0, 0)
        }
    }

    private fun checkUserRoleAndSetupUI() {
        val userId = auth.currentUser?.uid ?: run {
            layoutLoadingOverlay.visibility = View.GONE
            Toast.makeText(this, "Not logged in!", Toast.LENGTH_SHORT).show()
            return
        }
        layoutLoadingOverlay.visibility = View.VISIBLE
        
        db.collection("users").document(userId).get().addOnSuccessListener { doc ->
            if (isFinishing || isDestroyed) return@addOnSuccessListener
            
            val role = doc.getString("role") ?: "Student"
            currentUserRole = if (role.equals("Teacher", ignoreCase = true)) "Teacher" else "Student"
            assignedClassIds = doc.get("classIds") as? List<String> ?: emptyList()
            
            if (currentUserRole == "Teacher") {
                setupTeacherUI(userId)
            } else {
                setupStudentUI(assignedClassIds)
            }
        }.addOnFailureListener { e ->
            handleFirebaseError(e)
        }
    }

    private fun setupTeacherUI(userId: String) {
        sectionTeacherCreate.visibility = View.VISIBLE
        rbMyClass.text = "Managed"
        findViewById<RadioButton>(R.id.rbGlobal).text = "Global"
        findViewById<LinearLayout>(R.id.navDungeons).visibility = View.VISIBLE

        fetchManagedClasses(userId)

        findViewById<Button>(R.id.btnCreateClass)?.setOnClickListener {
            val className = findViewById<EditText>(R.id.etNewClassName).text.toString().trim()
            if (className.isNotEmpty()) createClass(className, userId)
        }

        rgLeaderboardToggle.setOnCheckedChangeListener { _, checkedId ->
            val isManaged = checkedId == R.id.rbMyClass
            spinnerClassFilter.visibility = if (isManaged) View.VISIBLE else View.GONE
            findViewById<TextView>(R.id.tvSelectClassLabel).visibility = if (isManaged) View.VISIBLE else View.GONE
            fetchLeaderboard(if (isManaged) selectedClassId else null)
        }

        spinnerClassFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                if (pos >= 0 && pos < managedClasses.size) {
                    selectedClassId = managedClasses[pos].id
                    btnAddStudent.visibility = View.VISIBLE
                    if (rbMyClass.isChecked) fetchLeaderboard(selectedClassId)
                }
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        btnAddStudent.setOnClickListener { selectedClassId?.let { showAddStudentDialog(it) } }
    }

    private fun fetchManagedClasses(teacherId: String) {
        db.collection("classes").whereEqualTo("teacherId", teacherId).get()
            .addOnSuccessListener { result ->
                if (isFinishing || isDestroyed) return@addOnSuccessListener
                managedClasses.clear()
                val names = mutableListOf<String>()
                for (doc in result) {
                    if (assignedClassIds.isNotEmpty() && !assignedClassIds.contains(doc.id)) continue
                    
                    val n = doc.getString("name") ?: "Unnamed"
                    managedClasses.add(SchoolClass(doc.id, n))
                    names.add(n)
                }
                
                if (names.isEmpty()) {
                    layoutLoadingOverlay.visibility = View.GONE
                    tvEmptyMessage.visibility = View.VISIBLE
                } else {
                    selectedClassId = managedClasses[0].id
                    spinnerClassFilter.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, names).apply {
                        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    }
                    if (rbMyClass.isChecked) fetchLeaderboard(selectedClassId)
                    else layoutLoadingOverlay.visibility = View.GONE
                }
            }.addOnFailureListener { handleFirebaseError(it) }
    }

    private fun fetchLeaderboard(classId: String?) {
        if (isFinishing || isDestroyed) return
        layoutLoadingOverlay.visibility = View.VISIBLE
        tvEmptyMessage.visibility = View.GONE
        
        val isGlobal = (classId == null)
        val query = if (!isGlobal) db.collection("users").whereArrayContains("classIds", classId!!)
                    else db.collection("users")

        query.get().addOnSuccessListener { result ->
            if (isFinishing || isDestroyed) return@addOnSuccessListener
            val students = result.mapNotNull { doc ->
                val role = doc.getString("role") ?: ""
                if (role.equals("Teacher", ignoreCase = true)) return@mapNotNull null
                if (isGlobal && !role.equals("Student", ignoreCase = true)) return@mapNotNull null
                
                val currentExp = doc.getLong("experience") ?: doc.getLong("exp") ?: 0L
                val totalExp = doc.getLong("totalExperience") ?: ((doc.getLong("level") ?: 1L) - 1) * 100 + currentExp
                
                StudentRank(
                    name = doc.getString("name") ?: "Hero", 
                    level = doc.getLong("level")?.toInt() ?: 1, 
                    exp = currentExp.toInt(),
                    totalExp = totalExp,
                    uid = doc.id
                )
            }
            
            if (students.isEmpty()) tvEmptyMessage.visibility = View.VISIBLE
            else {
                tvEmptyMessage.visibility = View.GONE
                val sorted = students.sortedByDescending { it.totalExp }
                rvLeaderboard.adapter = LeaderboardAdapter(if (isGlobal) sorted.take(15) else sorted, currentUserRole == "Teacher" && !isGlobal, { sid, _ -> removeStudentFromClass(sid, classId!!) }) { s ->
                    if (currentUserRole == "Teacher") showStudentProfileDialog(s.uid)
                }
            }
            layoutLoadingOverlay.visibility = View.GONE
        }.addOnFailureListener { handleFirebaseError(it) }
    }

    private fun handleFirebaseError(e: Exception) {
        layoutLoadingOverlay.visibility = View.GONE
        Log.e("ClassesDB", "Error: ${e.message}", e)
        val msg = when (e) {
            is FirebaseFirestoreException -> when (e.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED -> "Access Denied! Check Firestore Rules."
                FirebaseFirestoreException.Code.UNAVAILABLE -> "Firestore Unavailable. Check Internet."
                FirebaseFirestoreException.Code.FAILED_PRECONDITION -> "Index missing! Check Logcat for link."
                else -> "Firestore Error: ${e.code}"
            }
            else -> "Error: ${e.localizedMessage}"
        }
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

    private fun setupStudentUI(classIds: List<String>) {
        spinnerClassFilter.visibility = View.GONE
        findViewById<TextView>(R.id.tvSelectClassLabel).visibility = View.GONE
        findViewById<LinearLayout>(R.id.navDungeons).visibility = View.GONE
        btnAddStudent.visibility = View.GONE
        
        val firstClassId = classIds.firstOrNull()
        
        // Fetch class name for the toggle button
        if (firstClassId != null) {
            db.collection("classes").document(firstClassId).get().addOnSuccessListener { doc ->
                if (doc.exists()) {
                    rbMyClass.text = doc.getString("name") ?: "Class"
                }
            }
        }

        fetchLeaderboard(firstClassId)

        rgLeaderboardToggle.setOnCheckedChangeListener { _, id ->
            fetchLeaderboard(if (id == R.id.rbMyClass) firstClassId else null)
        }
    }

    private fun createClass(name: String, tid: String) {
        layoutLoadingOverlay.visibility = View.VISIBLE
        db.collection("users").document(tid).get().addOnSuccessListener { tDoc ->
            val classData = hashMapOf("name" to name, "teacherId" to tid, "teacherName" to (tDoc.getString("name") ?: "Teacher"))
            db.collection("classes").add(classData).addOnSuccessListener {
                db.collection("users").document(tid).update("classesCount", FieldValue.increment(1))
                fetchManagedClasses(tid)
            }
        }.addOnFailureListener { handleFirebaseError(it) }
    }

    private fun removeStudentFromClass(sid: String, cid: String) {
        layoutLoadingOverlay.visibility = View.VISIBLE
        db.collection("users").document(sid).update("classIds", FieldValue.arrayRemove(cid))
            .addOnSuccessListener { fetchLeaderboard(cid) }
            .addOnFailureListener { handleFirebaseError(it) }
    }

    private fun showAddStudentDialog(cid: String) {
        val input = EditText(this).apply { hint = "Student Registered Name" }
        AlertDialog.Builder(this).setTitle("Add Student").setView(input).setPositiveButton("Add") { _, _ ->
            val n = input.text.toString().trim()
            if (n.isNotEmpty()) {
                layoutLoadingOverlay.visibility = View.VISIBLE
                db.collection("users").whereEqualTo("name", n).get().addOnSuccessListener { res ->
                    if (res.isEmpty) {
                        Toast.makeText(this, "Not found", Toast.LENGTH_SHORT).show()
                        layoutLoadingOverlay.visibility = View.GONE
                    } else {
                        db.collection("users").document(res.documents[0].id).update("classIds", FieldValue.arrayUnion(cid))
                            .addOnSuccessListener { fetchLeaderboard(cid) }
                    }
                }.addOnFailureListener { handleFirebaseError(it) }
            }
        }.setNegativeButton("Cancel", null).show()
    }

    private fun showStudentProfileDialog(uid: String) {
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                val s = "Name: ${doc.getString("name")}\nLevel: ${doc.getLong("level") ?: 1}"
                AlertDialog.Builder(this).setTitle("Profile").setMessage(s).setPositiveButton("Close", null).show()
            }
        }
    }

    private fun checkRoleAndNavigate() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).get().addOnSuccessListener { doc ->
            val intent = if ((doc.getString("role") ?: "").equals("Teacher", true)) Intent(this, TeacherActivity::class.java) 
                         else Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}

class LeaderboardAdapter(private val list: List<StudentRank>, private val showDel: Boolean, val onDel: (String, String) -> Unit, val onClick: (StudentRank) -> Unit) : RecyclerView.Adapter<LeaderboardAdapter.VH>() {
    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvRank: TextView = v.findViewById(R.id.tvRank)
        val tvName: TextView = v.findViewById(R.id.tvStudentName)
        val tvLevel: TextView = v.findViewById(R.id.tvStudentLevel)
        val pbXp: ProgressBar = v.findViewById(R.id.pbXpProgress)
        val btnDel: ImageButton = v.findViewById(R.id.btnRemoveStudent)
        val tvPoints: TextView = v.findViewById(R.id.tvPoints)
    }
    override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(LayoutInflater.from(p.context).inflate(R.layout.item_leaderboard, p, false))
    override fun onBindViewHolder(h: VH, p: Int) {
        val s = list[p]
        h.tvRank.text = "${p + 1}"
        h.tvName.text = s.name
        h.tvLevel.text = "Lvl ${s.level}"
        h.pbXp.progress = s.exp % 100
        h.tvPoints.text = NumberFormat.getNumberInstance(Locale.US).format(s.totalExp)
        h.btnDel.visibility = if (showDel) View.VISIBLE else View.GONE
        h.btnDel.setOnClickListener { onDel(s.uid, s.name) }
        h.itemView.setOnClickListener { onClick(s) }
    }
    override fun getItemCount() = list.size
}
