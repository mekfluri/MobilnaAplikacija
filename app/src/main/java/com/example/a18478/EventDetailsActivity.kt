package com.example.a18478

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.a18478.databinding.ActivityEventDetailsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*

class EventDetailsActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_EVENT = "extra_event"
        const val EXTRA_EVENT_ID = "extra_event_id"
    }

    private val reviewsList: MutableList<Review> = mutableListOf()
    private lateinit var binding: ActivityEventDetailsBinding
    private var event: Event? = null
    private lateinit var eventId: String
    private val eventsRef: DatabaseReference = FirebaseDatabase.getInstance("https://project-4778345136366669416-default-rtdb.europe-west1.firebasedatabase.app")
        .getReference("events")
    private val reviewsAdapter: ReviewAdapter by lazy {
        ReviewAdapter(reviewsList)
    }

    // Define reviewsRef as a class-level property
    private val reviewsRef: DatabaseReference by lazy {
        FirebaseDatabase.getInstance("https://project-4778345136366669416-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference("events")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEventDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        event = intent.getParcelableExtra(EXTRA_EVENT)
        eventId = intent.getStringExtra(EXTRA_EVENT_ID) ?: ""

        if (event != null) {
            displayEventDetails()
        }

        // Setup RecyclerView for reviews
        binding.reviewsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@EventDetailsActivity)
            adapter = reviewsAdapter
        }

        val btnBuyTicket: Button = findViewById(R.id.btnBuyTicket)
        val currentUser: FirebaseUser? = FirebaseAuth.getInstance().currentUser

        if (currentUser != null) {
            val eventUsersRef = FirebaseDatabase.getInstance("https://project-4778345136366669416-default-rtdb.europe-west1.firebasedatabase.app/")
                .getReference("events")
                .child(eventId)
                .child("users")

            eventUsersRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val hasBoughtTicket = snapshot.hasChild(currentUser.uid)

                    if (hasBoughtTicket) {
                        // User has already bought a ticket, hide the "Buy Ticket" button
                        btnBuyTicket.visibility = View.GONE
                    } else {
                        // User has not bought a ticket, show the "Buy Ticket" button
                        btnBuyTicket.visibility = View.VISIBLE
                        btnBuyTicket.setOnClickListener {
                            // Handle the ticket purchase logic
                            updateEventUserList(eventId, currentUser.uid)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("EventDetailsActivity", "Error checking user attendance: ${error.message}")
                }
            })
        } else {
            // If the user is not authenticated, handle the sign-in process
            // ...
        }

        val reviewBtn: Button = findViewById(R.id.reviewBtn)
        reviewBtn.setOnClickListener {
            // Create a new instance of ReviewSubmissionFragment
            val reviewFragment = ReviewSubmissionFragment.newInstance(eventId)

            // Replace the current fragment in the container with the new ReviewSubmissionFragment
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, reviewFragment)
                .addToBackStack(null)
                .commit()
        }

        // Fetch and display reviews
        fetchReviews(eventId)
    }


    // Function to fetch reviews from Firebase Realtime Database
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

                // Calculate the average rating
                val totalRating = reviewsList.sumByDouble { review -> review.rating.toDouble() }
                val averageRating = if (reviewsList.isNotEmpty()) totalRating / reviewsList.size else 0.0

                // Update the UI to display the average rating and number of reviews
                binding.textViewAverageRating.text = String.format("%.2f", averageRating)
                binding.textViewNumberOfReviews.text = resources.getQuantityString(
                    R.plurals.numberOfReviews,
                    reviewsList.size,
                    reviewsList.size
                )

                reviewsAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("EventDetailsActivity", "Error fetching reviews: ${error.message}")
            }
        })
    }




    @SuppressLint("RestrictedApi")
    private fun updateEventUserList(eventKey: String, userId: String) {
        Log.d("EventDetailsActivity", "Updating event user list for event: $eventKey, user: $userId")

        val eventRef = FirebaseDatabase.getInstance("https://project-4778345136366669416-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference("events")
            .child(eventKey)
            .child("users")
            .child(userId)

        Log.d("EventDetailsActivity", "Event reference path: ${eventRef.path}")

        // Set the value to true (you can use "false" if you want to remove the user from the event)
        eventRef.setValue(true)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("EventDetailsActivity", "Event user update successful")

                    // If the update is successful, refresh the activity to show the updated user list
                    displayEventDetails()

                    // Add event attendance information to the user node
                    val userEventRef = FirebaseDatabase.getInstance("https://project-4778345136366669416-default-rtdb.europe-west1.firebasedatabase.app")
                        .getReference("korisnici")
                        .child(userId)
                        .child("events")
                        .child(eventKey)

                    Log.d("EventDetailsActivity", "User event reference path: ${userEventRef.path}")

                    userEventRef.setValue(true)
                        .addOnSuccessListener {
                            Log.d("EventDetailsActivity", "User attendance updated for event: $eventKey")

                            // Refresh the event details after the user buys the ticket
                            displayEventDetails()
                        }
                        .addOnFailureListener { e ->
                            Log.e("EventDetailsActivity", "Failed to update user attendance for event: $eventKey", e)
                        }

                } else {
                    // Handle the error if the update fails
                    Log.e("EventDetailsActivity", "Failed to update event user list for event: $eventKey, user: $userId")
                    // For example, show an error toast or log the error message
                }
            }
    }

    fun displayEventDetails() {
        // Populate the XML views with event details
        event?.let {
            binding.textViewEventType.text = it.eventType
            binding.textViewDate.text = it.date
            binding.textViewTime.text = it.time
            binding.textViewDescription.text = it.description

            // Call the function to fetch and display user names
            fetchAndDisplayUserNames(eventId)
        }
    }






    private fun fetchAndDisplayUserNames(eventKey: String) {
        val eventUsersRef = FirebaseDatabase.getInstance("https://project-4778345136366669416-default-rtdb.europe-west1.firebasedatabase.app/")
            .getReference("events")
            .child(eventKey)
            .child("users")

        eventUsersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val userIds = mutableListOf<String>()

                // Collect the user IDs from the event's "users" node
                for (userSnapshot in snapshot.children) {
                    val userId = userSnapshot.key
                    userId?.let { userIds.add(it) }
                }

                // Fetch and display the user names
                fetchUsernames(userIds) { usernamesMap ->
                    val userListContainer = findViewById<LinearLayout>(R.id.userListContainer)

                    for ((userId, username) in usernamesMap) {
                        val userNameTextView = TextView(this@EventDetailsActivity)
                        userNameTextView.text = username
                        userListContainer.addView(userNameTextView)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("EventDetailsActivity", "Error fetching event users: ${error.message}")
            }
        })
    }

    private fun fetchUsernames(userIds: List<String>, callback: (Map<String, String>) -> Unit) {
        val usersRef = FirebaseDatabase.getInstance("https://project-4778345136366669416-default-rtdb.europe-west1.firebasedatabase.app/")
            .getReference("korisnici")

        val usernamesMap = mutableMapOf<String, String>()

        for (userId in userIds) {
            usersRef.child(userId).child("korisnickoIme")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val username = snapshot.getValue(String::class.java)
                        if (username != null) {
                            usernamesMap[userId] = username
                        }

                        if (usernamesMap.size == userIds.size) {
                            callback(usernamesMap)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("EventDetailsActivity", "Error fetching usernames: ${error.message}")
                    }
                })
        }
    }}