package com.example.mainquest

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import com.example.mainquest.data.Task

class HabitsAdapter(
    private var habits: List<Task>,
    private val onEditClick: (Task) -> Unit,
    private val onDeleteClick: (Task) -> Unit,
    private val onCompleteClick: (Task) -> Unit
) : RecyclerView.Adapter<HabitsAdapter.HabitViewHolder>() {

    inner class HabitViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.habit_title)
        val xpReward: TextView = itemView.findViewById(R.id.habit_xp_reward)
        val checkbox: CheckBox = itemView.findViewById(R.id.habit_checkbox)
        val editButton: ImageButton = itemView.findViewById(R.id.edit_habit_button)
        val deleteButton: ImageButton = itemView.findViewById(R.id.delete_habit_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_habit, parent, false)
        return HabitViewHolder(view)
    }

    override fun onBindViewHolder(holder: HabitViewHolder, position: Int) {
        val habit = habits[position]
        holder.title.text = habit.title
        
        // Ako XP nagrada je 0, sakrij je ili prikaži upozorenje
        if (habit.xpReward > 0) {
            holder.xpReward.text = "+${habit.xpReward} XP"
            holder.xpReward.visibility = View.VISIBLE
        } else {
            holder.xpReward.text = "Uredi za XP!"
            holder.xpReward.visibility = View.VISIBLE
        }
        
        holder.checkbox.isChecked = habit.isCompleted
        
        Log.d("HabitsAdapter", "Task: ${habit.title}, XP: ${habit.xpReward}, Completed: ${habit.isCompleted}")
        
        holder.checkbox.setOnClickListener { 
            Log.d("HabitsAdapter", "Checkbox clicked for: ${habit.title}")
            onCompleteClick(habit)
        }
        holder.editButton.setOnClickListener { onEditClick(habit) }
        holder.deleteButton.setOnClickListener { onDeleteClick(habit) }
        
        // Prikaži vizualnu razliku za završene zadatke
        if (habit.isCompleted) {
            holder.itemView.alpha = 0.6f
            holder.title.setTextColor(holder.itemView.context.getColor(android.R.color.darker_gray))
            holder.xpReward.text = "✅ ${habit.xpReward} XP"
        } else {
            holder.itemView.alpha = 1.0f
            holder.title.setTextColor(holder.itemView.context.getColor(R.color.primaryDarkBlue))
        }
    }

    override fun getItemCount(): Int = habits.size

    fun updateHabits(newHabits: List<Task>) {
        Log.d("HabitsAdapter", "Updating habits, count: ${newHabits.size}")
        habits = newHabits
        notifyDataSetChanged()
    }
} 