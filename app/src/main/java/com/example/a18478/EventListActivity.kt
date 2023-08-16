package com.example.a18478

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class EventListActivity : AppCompatActivity() {

    private lateinit var eventsRecyclerView: RecyclerView
    private lateinit var eventAdapter: EventAdapter
    private val eventsRef: DatabaseReference by lazy {
        FirebaseDatabase.getInstance("https://project-4778345136366669416-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference("events")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_list)

        eventsRecyclerView = findViewById(R.id.reviewsRecyclerView)
        eventAdapter = EventAdapter(emptyList()) // Initialize with an empty list

        eventsRecyclerView.adapter = eventAdapter
        eventsRecyclerView.layoutManager = LinearLayoutManager(this)

        // Fetch events from the database
        fetchEvents()
    }

    private fun fetchEvents() {
        eventsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val eventsList = mutableListOf<Event>()

                for (eventSnapshot in snapshot.children) {
                    val event = eventSnapshot.getValue(Event::class.java)
                    event?.let {
                        eventsList.add(it)
                    }
                }

                // Update the adapter with fetched events
                eventAdapter.updateEvents(eventsList)
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle database error here
            }
        })
    }
}
