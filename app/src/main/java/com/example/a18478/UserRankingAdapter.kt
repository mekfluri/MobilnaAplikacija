package com.example.a18478

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// Adapter klasa za prikaz korisničkih rangiranja u RecyclerView

class UserRankingAdapter(private val usersList: List<UserRankingItem>) :
    RecyclerView.Adapter<UserRankingAdapter.UserRankingViewHolder>() {

    // Kreiranje ViewHolder objekta, tj. prikaza za svaku stavku u listi
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserRankingViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_ranking, parent, false)
        return UserRankingViewHolder(itemView)
    }

    // Postavljanje podataka za prikazivanje u ViewHolderu
    override fun onBindViewHolder(holder: UserRankingViewHolder, position: Int) {
        val currentItem = usersList[position]
        holder.textViewUsername.text = currentItem.username
        holder.textViewPoints.text = currentItem.points.toString()
    }

    // Broj stavki u listi
    override fun getItemCount() = usersList.size

    // ViewHolder klasa koja predstavlja svaku stavku u RecyclerView
    class UserRankingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewUsername: TextView = itemView.findViewById(R.id.textViewUsername)
        val textViewPoints: TextView = itemView.findViewById(R.id.textViewPoints)
    }
}
