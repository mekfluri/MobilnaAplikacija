package com.example.a18478
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class UserReviewAdapter(private val reviews: List<Review>) :
    RecyclerView.Adapter<UserReviewAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ratingBar: RatingBar = itemView.findViewById(R.id.ratingBar)
        val commentTextView: TextView = itemView.findViewById(R.id.commentTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_review, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val review = reviews[position]
        holder.ratingBar.rating = review.rating.toFloat()
        holder.commentTextView.text = review.comment
    }

    override fun getItemCount(): Int {
        return reviews.size
    }
}