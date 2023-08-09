package com.example.a18478

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class UserRankingAdapter(private val usersList: List<UserRankingItem>) :
    RecyclerView.Adapter<UserRankingAdapter.UserRankingViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserRankingViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_ranking, parent, false)
        return UserRankingViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: UserRankingViewHolder, position: Int) {
        val currentItem = usersList[position]
        holder.textViewUsername.text = currentItem.username
        holder.textViewPoints.text = currentItem.points.toString()
    }

    override fun getItemCount() = usersList.size

    class UserRankingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewUsername: TextView = itemView.findViewById(R.id.textViewUsername)
        val textViewPoints: TextView = itemView.findViewById(R.id.textViewPoints)
    }
}
