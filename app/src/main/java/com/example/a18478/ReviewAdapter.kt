package com.example.a18478

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.a18478.Review
import com.example.a18478.R

class ReviewAdapter(private val reviews: List<Review>) : RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_review, parent, false)
        return ReviewViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        val review = reviews[position]
        holder.bind(review)
    }

    override fun getItemCount(): Int {
        return reviews.size
    }

    class ReviewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ratingBar: RatingBar = itemView.findViewById(R.id.ratingBar)
        private val commentTextView: TextView = itemView.findViewById(R.id.commentTextView)

        fun bind(review: Review) {
            ratingBar.rating = review.rating
            commentTextView.text = review.comment
        }
    }
}
