package com.example.smartlearningeducationalgame2

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DungeonAdapter(
    private val dungeons: List<Dungeon>,
    private val playerLevel: Int,
    private val purchasedDungeonIds: List<String>,
    private val onDungeonClick: (Dungeon) -> Unit
) : RecyclerView.Adapter<DungeonAdapter.DungeonViewHolder>() {

    class DungeonViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val layoutAvailable: View = view.findViewById(R.id.layoutAvailable)
        val layoutLocked: View = view.findViewById(R.id.layoutLocked)
        val tvTitle: TextView = view.findViewById(R.id.tvDungeonTitle)
        val tvDifficulty: TextView = view.findViewById(R.id.tvDifficulty)
        val tvReward: TextView = view.findViewById(R.id.tvReward)
        val tvLockedTitle: TextView = view.findViewById(R.id.tvLockedTitle)
        val tvUnlockProgress: TextView = view.findViewById(R.id.tvUnlockProgress)
        val btnEnter: View = view.findViewById(R.id.btnEnter)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DungeonViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dungeon, parent, false)
        return DungeonViewHolder(view)
    }

    override fun onBindViewHolder(holder: DungeonViewHolder, position: Int) {
        val dungeon = dungeons[position]
        val isLevelUnlocked = playerLevel >= dungeon.requiredLevel
        val isAlreadyBought = purchasedDungeonIds.contains(dungeon.firestoreId)

        if (isLevelUnlocked) {
            holder.layoutAvailable.visibility = View.VISIBLE
            holder.layoutLocked.visibility = View.GONE
            
            holder.tvTitle.text = dungeon.title
            holder.tvReward.text = "+${(dungeon.requiredLevel * 20)} XP"
            
            // Set difficulty color based on required level
            when {
                dungeon.requiredLevel <= 5 -> {
                    holder.tvDifficulty.text = "EASY"
                    holder.tvDifficulty.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#E8F5E9"))
                    holder.tvDifficulty.setTextColor(Color.parseColor("#4CAF50"))
                }
                dungeon.requiredLevel <= 15 -> {
                    holder.tvDifficulty.text = "NORMAL"
                    holder.tvDifficulty.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FFF3E0"))
                    holder.tvDifficulty.setTextColor(Color.parseColor("#FF9800"))
                }
                else -> {
                    holder.tvDifficulty.text = "ELITE"
                    holder.tvDifficulty.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FFEBEE"))
                    holder.tvDifficulty.setTextColor(Color.parseColor("#F44336"))
                }
            }

            // Entry logic now restricted to the ENTER button specifically
            holder.btnEnter.setOnClickListener { onDungeonClick(dungeon) }
            holder.itemView.setOnClickListener(null) // Disable clicking the card background
        } else {
            holder.layoutAvailable.visibility = View.GONE
            holder.layoutLocked.visibility = View.VISIBLE
            
            holder.tvLockedTitle.text = dungeon.title
            holder.tvUnlockProgress.text = "Requires Level ${dungeon.requiredLevel} (Progress: $playerLevel / ${dungeon.requiredLevel})"
            holder.btnEnter.setOnClickListener(null)
            holder.itemView.setOnClickListener(null)
        }
    }

    override fun getItemCount() = dungeons.size
}
