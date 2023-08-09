package com.example.a18478
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RatingBar
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ReviewSubmissionFragment : Fragment() {

    private lateinit var ratingBar: RatingBar
    private lateinit var editTextComment: EditText
    private lateinit var submitReviewBtn: Button

    private lateinit var eventId: String
    private val reviewsRef: DatabaseReference by lazy {
        FirebaseDatabase.getInstance("https://project-4778345136366669416-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference("events")
            .child(eventId)
            .child("recenzije")
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_review_submission, container, false)

        ratingBar = view.findViewById(R.id.ratingBar)
        editTextComment = view.findViewById(R.id.editTextComment)
        submitReviewBtn = view.findViewById(R.id.submitReviewBtn)

        submitReviewBtn.setOnClickListener {
            submitReview()
        }

        return view
    }

    private fun submitReview() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userId = currentUser?.uid ?: return

        val rating = ratingBar.rating
        val comment = editTextComment.text.toString().trim()

        if (rating > 0 && comment.isNotEmpty()) {
            val review = Review(userId, rating, comment)

            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val reviewId = reviewsRef.push().key ?: return@launch
                    reviewsRef.child(reviewId).setValue(review).await()
                    updatePointsAfterReview(userId)

                    // Review submitted successfully
                    Log.d("ReviewSubmissionFragment", "Review submitted successfully")

                    // Switch to the main (UI) thread to update the UI
                    GlobalScope.launch(Dispatchers.Main) {
                        // Optionally, you can show a success message or handle other actions
                        // Refresh the event details screen to show the updated reviews
                        (activity as? EventDetailsActivity)?.displayEventDetails()
                    }
                    closeFragment()

                } catch (e: Exception) {
                    // Review submission failed
                    Log.e("ReviewSubmissionFragment", "Failed to submit review", e)
                    // You can handle the failure, show an error message, or log the error
                }
            }
        } else {
            // Invalid rating or empty comment
            Log.e("ReviewSubmissionFragment", "Invalid rating or empty comment")
            // You can display an error message to the user if required
        }
    }
    private fun updatePointsAfterReview(userId: String) {
        val userRef = FirebaseDatabase.getInstance("https://project-4778345136366669416-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference("korisnici")
            .child(userId)

        userRef.child("poeni").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentPoints = snapshot.getValue(Int::class.java) ?: 0
                val newPoints = currentPoints + 1.5 // Adding 1.5 points for the review
                userRef.child("poeni").setValue(newPoints)
                    .addOnSuccessListener {
                        // Points updated successfully
                        Log.d("ReviewSubmissionFragment", "User points updated after review: $newPoints")
                    }
                    .addOnFailureListener {
                        // Handle the error if updating points fails
                        Log.e("ReviewSubmissionFragment", "Failed to update user points after review")
                    }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle the error if the database operation is canceled
                Log.e("ReviewSubmissionFragment", "Error updating user points after review: ${error.message}")
            }
        })
    }

    companion object {
        fun newInstance(eventId: String): ReviewSubmissionFragment {
            val fragment = ReviewSubmissionFragment()
            fragment.eventId = eventId
            return fragment
        }
    }
    private fun closeFragment() {
        requireFragmentManager().beginTransaction().remove(this).commit()
    }

}
