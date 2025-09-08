package com.example.mainquest

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mainquest.data.Reward

class RewardsAdapter(
    private var rewards: List<Reward>,
    private val onPurchaseClick: (Reward) -> Unit
) : RecyclerView.Adapter<RewardsAdapter.RewardViewHolder>() {
    
    private var userXp: Int = 0

    inner class RewardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: TextView = itemView.findViewById(R.id.reward_icon)
        val name: TextView = itemView.findViewById(R.id.reward_name)
        val description: TextView = itemView.findViewById(R.id.reward_description)
        val xpCost: TextView = itemView.findViewById(R.id.reward_xp_cost)
        val purchaseButton: Button = itemView.findViewById(R.id.purchase_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RewardViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_reward, parent, false)
        return RewardViewHolder(view)
    }

    override fun onBindViewHolder(holder: RewardViewHolder, position: Int) {
        val reward = rewards[position]
        
        // Izvuci ikonu iz naziva (prvi emoji)
        val icon = reward.name.takeWhile { it.code > 127 } // Uzmi emoji karaktere
        val nameWithoutIcon = reward.name.dropWhile { it.code > 127 }.trim()
        
        holder.icon.text = icon.ifEmpty { "🏆" }
        holder.name.text = nameWithoutIcon.ifEmpty { reward.name }
        holder.description.text = reward.description ?: "Specijalna nagrada"
        holder.xpCost.text = "${reward.xpCost} XP"
        
        // Provjeri može li korisnik kupiti nagradu
        val canAfford = userXp >= reward.xpCost
        holder.purchaseButton.isEnabled = canAfford
        holder.purchaseButton.text = if (canAfford) "💰 Kupi nagradu" else "❌ Nedovoljno XP"
        holder.purchaseButton.alpha = if (canAfford) 1.0f else 0.6f
        
        holder.purchaseButton.setOnClickListener {
            if (canAfford) {
                onPurchaseClick(reward)
            }
        }
    }

    override fun getItemCount(): Int = rewards.size

    fun updateRewards(newRewards: List<Reward>) {
        rewards = newRewards
        notifyDataSetChanged()
    }
    
    fun updateUserXp(xp: Int) {
        userXp = xp
        notifyDataSetChanged()
    }
} 