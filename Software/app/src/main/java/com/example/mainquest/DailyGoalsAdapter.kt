package com.example.mainquest

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mainquest.data.Task

class DailyGoalsAdapter(
    private var dailyGoals: List<Task>,
    private val onCompleteClick: (Task) -> Unit,
    private val onEditClick: (Task) -> Unit,
    private val onDeleteClick: (Task) -> Unit
) : RecyclerView.Adapter<DailyGoalsAdapter.DailyGoalViewHolder>() {

    inner class DailyGoalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.daily_goal_title)
        val category: TextView = itemView.findViewById(R.id.daily_goal_category)
        val xpReward: TextView = itemView.findViewById(R.id.daily_goal_xp_reward)
        val progress: TextView = itemView.findViewById(R.id.daily_goal_progress)
        val progressBar: ProgressBar = itemView.findViewById(R.id.daily_goal_progress_bar)
        val streak: TextView = itemView.findViewById(R.id.daily_goal_streak)
        val completeButton: ImageButton = itemView.findViewById(R.id.complete_goal_button)
        val editButton: ImageButton = itemView.findViewById(R.id.edit_goal_button)
        val deleteButton: ImageButton = itemView.findViewById(R.id.delete_goal_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DailyGoalViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_daily_goal, parent, false)
        return DailyGoalViewHolder(view)
    }

    override fun onBindViewHolder(holder: DailyGoalViewHolder, position: Int) {
        val goal = dailyGoals[position]
        
        holder.title.text = goal.title
        holder.category.text = goal.category ?: "Ostalo"
        holder.xpReward.text = "+${goal.xpReward} XP"
        holder.progress.text = "${goal.dailyProgress}/${goal.dailyTarget}"
        
        // Postavi progress bar
        holder.progressBar.max = goal.dailyTarget
        holder.progressBar.progress = goal.dailyProgress
        
        // Prikaži streak
        holder.streak.text = "🔥 ${goal.streakCount}"
        
        // Postavi click listenere
        holder.completeButton.setOnClickListener { onCompleteClick(goal) }
        holder.editButton.setOnClickListener { onEditClick(goal) }
        holder.deleteButton.setOnClickListener { onDeleteClick(goal) }
        
        // Promijeni boju ako je cilj završen za danas
        if (goal.dailyProgress >= goal.dailyTarget) {
            holder.itemView.alpha = 0.7f
            holder.completeButton.isEnabled = false
            holder.xpReward.text = "✅ ${goal.xpReward} XP"
        } else {
            holder.itemView.alpha = 1.0f
            holder.completeButton.isEnabled = true
        }
    }

    override fun getItemCount(): Int = dailyGoals.size

    fun updateGoals(newGoals: List<Task>) {
        dailyGoals = newGoals
        notifyDataSetChanged()
    }
} 