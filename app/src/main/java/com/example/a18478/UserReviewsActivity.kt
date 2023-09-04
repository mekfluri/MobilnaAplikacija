package com.example.a18478

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class UserReviewsActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_EVENT_ID = "extra_event_id"
    }

    private val reviewsList: MutableList<Review> = mutableListOf()
    private lateinit var reviewsRecyclerView: RecyclerView
    private lateinit var eventId: String
    private val reviewsAdapter: UserReviewAdapter by lazy {
        UserReviewAdapter(reviewsList)
    }

    private val reviewsRef: DatabaseReference by lazy {
        FirebaseDatabase.getInstance("https://project-4778345136366669416-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference("events")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_reviews)

        eventId = intent.getStringExtra(EXTRA_EVENT_ID) ?: ""

        reviewsRecyclerView = findViewById(R.id.reviewsRecyclerView)
        setupRecyclerView()

        fetchReviews(eventId)
    }

    private fun setupRecyclerView() {
        reviewsRecyclerView.layoutManager = LinearLayoutManager(this)
        reviewsRecyclerView.adapter = reviewsAdapter
    }

    private fun fetchReviews(eventId: String) {
        val reviewsRef = reviewsRef.child(eventId).child("recenzije")

        reviewsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                reviewsList.clear()

                for (reviewSnapshot in snapshot.children) {
                    val review = reviewSnapshot.getValue(Review::class.java)
                    review?.let {
                        reviewsList.add(it)
                    }
                }

                reviewsAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }
}
