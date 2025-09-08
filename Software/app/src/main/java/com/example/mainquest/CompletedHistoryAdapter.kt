package com.example.mainquest

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class CompletedHistoryAdapter(
    private var completedItems: List<CompletedTaskItem>
) : RecyclerView.Adapter<CompletedHistoryAdapter.CompletedHistoryViewHolder>() {

    inner class CompletedHistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val typeIcon: TextView = itemView.findViewById(R.id.completed_type_icon)
        val title: TextView = itemView.findViewById(R.id.completed_title)
        val category: TextView = itemView.findViewById(R.id.completed_category)
        val xpEarned: TextView = itemView.findViewById(R.id.completed_xp_earned)
        val completedAt: TextView = itemView.findViewById(R.id.completed_at)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CompletedHistoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_completed_task, parent, false)
        return CompletedHistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CompletedHistoryViewHolder, position: Int) {
        val item = completedItems[position]
        
        // Postavi ikonu ovisno o tipu zadatka
        holder.typeIcon.text = if (item.isDailyGoal) "📅" else "✅"
        
        holder.title.text = item.title
        holder.category.text = item.category
        holder.xpEarned.text = "+${item.xpEarned} XP"
        
        // Formatiraj datum i vrijeme
        val formattedDate = try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            val date = inputFormat.parse(item.completedAt)
            date?.let { outputFormat.format(it) } ?: item.completedAt
        } catch (e: Exception) {
            // Ako datum nije u očekivanom formatu, pokušaj samo s datumom
            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                val date = inputFormat.parse(item.completedAt)
                date?.let { outputFormat.format(it) } ?: item.completedAt
            } catch (e2: Exception) {
                item.completedAt
            }
        }
        
        holder.completedAt.text = formattedDate
    }

    override fun getItemCount(): Int = completedItems.size

    fun updateHistory(newCompletedItems: List<CompletedTaskItem>) {
        completedItems = newCompletedItems
        notifyDataSetChanged()
    }
} 