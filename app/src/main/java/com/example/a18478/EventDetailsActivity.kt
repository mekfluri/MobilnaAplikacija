package com.example.a18478

import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.a18478.databinding.ActivityEventDetailsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class EventDetailsActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_EVENT = "extra_event"
        const val EXTRA_EVENT_ID = "extra_event_id"
    }

    private lateinit var binding: ActivityEventDetailsBinding
    private var event: Event? = null
    private lateinit var eventId: String
    private val eventsRef: DatabaseReference = FirebaseDatabase.getInstance("https://project-4778345136366669416-default-rtdb.europe-west1.firebasedatabase.app")
        .getReference("events")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEventDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        event = intent.getParcelableExtra(EXTRA_EVENT)
        eventId = intent.getStringExtra(EXTRA_EVENT_ID) ?: ""

        if (event != null) {
            displayEventDetails()
        }

        // Handle the ticket button click
        val btnBuyTicket: Button = findViewById(R.id.btnBuyTicket)
        btnBuyTicket.setOnClickListener {
            val currentUser: FirebaseUser? = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                // Add the current authenticated user's ID to the event's userList
                updateEventUserList(eventId, currentUser.uid) // Pass the eventKey (eventId) as the first argument
            } else {
                // If the user is not authenticated, handle the sign-in process
                // For example, you can show a sign-in prompt or redirect the user to the login page.
                // You can use FirebaseUI Auth for a quick and easy way to implement Firebase Auth UI: https://github.com/firebase/FirebaseUI-Android
            }
        }

    }

    private fun displayEventDetails() {
        // Populate the XML views with event details
        event?.let {
            binding.textViewEventType.text = it.eventType
            binding.textViewDate.text = it.date
            binding.textViewTime.text = it.time
            binding.textViewDescription.text = it.description

            val userList = it.userList // Store userList in a local variable

            // Fetch and display the list of users who have bought tickets for this event
            fetchUsernames(userList) { usernamesMap ->
                val usernames = userList.mapNotNull { userId -> usernamesMap[userId] }
                val userListText = usernames.joinToString(", ")
                binding.textViewUserList.text = userListText
            }
        }
    }



    private fun updateEventUserList(eventKey: String, userId: String) {
        Log.d("EventDetailsActivity", "eventKey: $eventKey") // Log the value of eventKey
        val eventRef = FirebaseDatabase.getInstance("https://project-4778345136366669416-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference("events")
            .child(eventKey)
            .child("users")
            .child(userId)

        // Set the value to true (you can use "false" if you want to remove the user from the event)
        eventRef.setValue(true)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // If the update is successful, refresh the activity to show the updated user list
                    displayEventDetails()

                    // Add event attendance information to the user node
                    val userEventRef = FirebaseDatabase.getInstance("https://project-4778345136366669416-default-rtdb.europe-west1.firebasedatabase.app")
                        .getReference("korisnici")
                        .child(userId)
                        .child("events")
                        .child(eventKey)

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
                    // For example, show an error toast or log the error message
                }
            }
    }







    private fun fetchUsernames(userIds: List<String>, callback: (Map<String, String>) -> Unit) {
        val usersRef = FirebaseDatabase.getInstance("https://project-4778345136366669416-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference("users")

        val usernamesMap = mutableMapOf<String, String>()

        for (userId in userIds) {
            usersRef.child(userId).get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userSnapshot = task.result // Get the user data snapshot
                    if (userSnapshot != null && userSnapshot.exists()) {
                        // Extract the username from the user data snapshot
                        val username = userSnapshot.child("korisnickoIme").getValue(String::class.java)
                        if (username != null) {
                            // Store the username in the usernamesMap
                            usernamesMap[userId] = username
                        }
                    }

                    // Check if all usernames have been fetched
                    if (usernamesMap.size == userIds.size) {
                        callback(usernamesMap)
                    }
                } else {
                    // Handle the error if fetching user data fails
                }
            }
        }
    }
}