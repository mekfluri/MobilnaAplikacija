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

                    // Recenzija uspešno poslata
                    Log.d("ReviewSubmissionFragment", "Recenzija uspešno poslata")

                    // Prebacivanje na glavnu (UI) nit za ažuriranje interfejsa
                    GlobalScope.launch(Dispatchers.Main) {

                        // osvezenje ekrana sa detaljima događaja kako biste prikazali ažurirane recenzije
                        (activity as? EventDetailsActivity)?.displayEventDetails()
                    }
                    closeFragment()

                } catch (e: Exception) {
                    // Neuspešno slanje recenzije
                    Log.e("ReviewSubmissionFragment", "Greška prilikom slanja recenzije", e)
                    // Možete obraditi neuspeh, prikazati poruku o grešci ili zabeležiti grešku
                }
            }
        } else {
            // Nevalidna ocena ili prazan komentar
            Log.e("ReviewSubmissionFragment", "Nevalidna ocena ili prazan komentar")
            // Možete prikazati poruku o grešci korisniku ako je potrebno
        }
    }

    private fun updatePointsAfterReview(userId: String) {
        val userRef = FirebaseDatabase.getInstance("https://project-4778345136366669416-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference("korisnici")
            .child(userId)

        userRef.child("poeni").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentPoints = snapshot.getValue(Int::class.java) ?: 0
                val newPoints = currentPoints + 1.5 // Dodavanje 1.5 poena za recenziju
                userRef.child("poeni").setValue(newPoints)
                    .addOnSuccessListener {
                        // Poeni uspešno ažurirani
                        Log.d("ReviewSubmissionFragment", "Poeni korisnika ažurirani nakon recenzije: $newPoints")
                    }
                    .addOnFailureListener {
                        // Obrada greške u slučaju neuspeha ažuriranja poena
                        Log.e("ReviewSubmissionFragment", "Greška prilikom ažuriranja poena korisnika nakon recenzije")
                    }
            }

            override fun onCancelled(error: DatabaseError) {
                // Obrada greške ako je operacija nad bazom podataka otkazana
                Log.e("ReviewSubmissionFragment", "Greška prilikom ažuriranja poena korisnika nakon recenzije: ${error.message}")
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
