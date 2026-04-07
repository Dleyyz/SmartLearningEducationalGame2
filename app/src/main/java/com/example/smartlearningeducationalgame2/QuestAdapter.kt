package com.example.smartlearningeducationalgame2

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class QuestAdapter(
    private val quests: List<Quest>,
    private val onQuestClick: (Quest) -> Unit,
    private val onDeleteClick: ((Quest) -> Unit)? = null,
    private val onAssignClick: ((Quest) -> Unit)? = null
) : RecyclerView.Adapter<QuestAdapter.QuestViewHolder>() {

    class QuestViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvQuestTitle: TextView = view.findViewById(R.id.tvQuestTitle)
        val tvQuestStatus: TextView = view.findViewById(R.id.tvQuestStatus)
        val tvQuestReward: TextView = view.findViewById(R.id.tvQuestReward)
        val tvQuestDifficulty: TextView = view.findViewById(R.id.tvQuestDifficulty)
        val layoutTeacherActions: LinearLayout = view.findViewById(R.id.layoutTeacherActions)
        val btnDeleteQuest: ImageButton = view.findViewById(R.id.btnDeleteQuest)
        val btnAssignQuest: TextView = view.findViewById(R.id.btnAssignQuest)
        val ivArrowIndicator: View = view.findViewById(R.id.ivArrowIndicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuestViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_quest, parent, false)
        return QuestViewHolder(view)
    }

    override fun onBindViewHolder(holder: QuestViewHolder, position: Int) {
        val quest = quests[position]
        holder.tvQuestTitle.text = quest.title
        
        // Display Rewards
        holder.tvQuestReward.text = "+${quest.rewardXp} XP • 🪙 ${String.format("%.1f", quest.rewardGold)}"
        
        // Display Difficulty with dynamic coloring
        holder.tvQuestDifficulty.text = quest.difficulty
        when (quest.difficulty.uppercase()) {
            "EASY" -> holder.tvQuestDifficulty.setTextColor(Color.parseColor("#4CAF50"))
            "HARD" -> holder.tvQuestDifficulty.setTextColor(Color.parseColor("#F44336"))
            else -> holder.tvQuestDifficulty.setTextColor(Color.parseColor("#2196F3"))
        }

        // Teacher specific actions
        if (onDeleteClick != null || onAssignClick != null) {
            holder.layoutTeacherActions.visibility = View.VISIBLE
            holder.ivArrowIndicator.visibility = View.GONE
            holder.btnDeleteQuest.setOnClickListener { onDeleteClick?.invoke(quest) }
            holder.btnAssignQuest.setOnClickListener { onAssignClick?.invoke(quest) }
        } else {
            holder.layoutTeacherActions.visibility = View.GONE
            holder.ivArrowIndicator.visibility = View.VISIBLE
        }

        // Completion status
        if (quest.isCompleted) {
            holder.tvQuestStatus.visibility = View.VISIBLE
            holder.ivArrowIndicator.visibility = View.GONE
        } else {
            holder.tvQuestStatus.visibility = View.GONE
        }

        holder.itemView.setOnClickListener { onQuestClick(quest) }
    }

    override fun getItemCount() = quests.size
}
