package com.example.mainquest

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class RecentActivityAdapter(
    private var activities: List<RecentActivity>
) : RecyclerView.Adapter<RecentActivityAdapter.ActivityViewHolder>() {

    inner class ActivityViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val taskName: TextView = itemView.findViewById(R.id.activity_task_name)
        val category: TextView = itemView.findViewById(R.id.activity_category)
        val xpEarned: TextView = itemView.findViewById(R.id.activity_xp_earned)
        val completedAt: TextView = itemView.findViewById(R.id.activity_completed_at)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_recent_activity, parent, false)
        return ActivityViewHolder(view)
    }

    override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
        val activity = activities[position]
        
        holder.taskName.text = activity.taskName
        holder.category.text = activity.category
        holder.xpEarned.text = "+${activity.xpEarned} XP"
        
        // Formatiraj datum
        val formattedDate = try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd.MM. HH:mm", Locale.getDefault())
            val date = inputFormat.parse(activity.completedAt)
            date?.let { outputFormat.format(it) } ?: activity.completedAt
        } catch (e: Exception) {
            activity.completedAt
        }
        
        holder.completedAt.text = formattedDate
    }

    override fun getItemCount(): Int = activities.size

    fun updateActivities(newActivities: List<RecentActivity>) {
        activities = newActivities
        notifyDataSetChanged()
    }
} 