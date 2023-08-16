package com.example.a18478

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class PretragaDogadjaja : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var eventsAdapter: EventAdapter
    private val eventsList: MutableList<Event> = mutableListOf()
    private val filteredEventsList: MutableList<Event> = mutableListOf()

    private val eventsRef: DatabaseReference by lazy {
        FirebaseDatabase.getInstance("https://project-4778345136366669416-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference("events")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pretraga_dogadjaja)

        recyclerView = findViewById(R.id.reviewsRecyclerView)
        eventsAdapter = EventAdapter(eventsList)

        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@PretragaDogadjaja)
            adapter = eventsAdapter
        }

        CoroutineScope(Dispatchers.Main).launch {
            val preferredEventTypes = fetchPreferredEventTypes()
            fetchEventsOfPreferredTypes(preferredEventTypes)
        }

        // Other UI setup code


    }


    private suspend fun fetchPreferredEventTypes(): List<String> {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val preferredEventTypes = mutableListOf<String>()

        // Fetch events for the current user using coroutines
        currentUser?.let { user ->
            val userId = user.uid
            val userEventsRef = FirebaseDatabase.getInstance("https://project-4778345136366669416-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference("korisnici")
                .child(userId)
                .child("events")

            try {
                val snapshot = userEventsRef.get().await() // Use await() to get the result asynchronously

                for (eventSnapshot in snapshot.children) {
                    val eventKey = eventSnapshot.key
                    if (eventKey != null) {
                        val eventTypeRef = FirebaseDatabase.getInstance("https://project-4778345136366669416-default-rtdb.europe-west1.firebasedatabase.app")
                            .getReference("events")
                            .child(eventKey)
                            .child("eventType")

                        val eventTypeSnapshot = eventTypeRef.get().await() // Use await() here as well
                        val eventType = eventTypeSnapshot.getValue(String::class.java)
                        eventType?.let {
                            preferredEventTypes.add(it)
                        }
                    }
                }
            } catch (e: Exception) {
                // Handle any potential exceptions here
                // e.g., log an error or show a toast message
            }
        }

        return preferredEventTypes
    }

    private fun fetchEventsOfPreferredTypes(preferredEventTypes: List<String>) {
        eventsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                eventsList.clear()

                for (eventSnapshot in snapshot.children) {
                    val event = eventSnapshot.getValue(Event::class.java)
                    event?.let {
                        // Check if the event type matches the user's preferred types
                        if (it.eventType in preferredEventTypes) {
                            eventsList.add(it)
                        }
                    }
                }

                eventsAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error if fetching data fails
                Toast.makeText(this@PretragaDogadjaja, "Failed to fetch data: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun filterEvents(query: String) {
        // Log the start of the filtering process
        Log.d("FilterEvents", "Filtering events with query: $query")

        filteredEventsList.clear()
        for (event in eventsList) {
            if (eventContainsQuery(event, query)) {
                filteredEventsList.add(event)
            }
        }

        // Log the end of the filtering process
        Log.d("FilterEvents", "Filtered events count: ${filteredEventsList.size}")

        eventsAdapter.filteredEventsList = filteredEventsList
        eventsAdapter.notifyDataSetChanged()
    }



    private fun eventContainsQuery(event: Event, query: String): Boolean {
        val queryLowerCase = query.toLowerCase(Locale.getDefault())
        val titleContainsQuery = event.title?.toLowerCase(Locale.getDefault())?.contains(queryLowerCase) ?: false
        val descriptionContainsQuery = event.description?.toLowerCase(Locale.getDefault())?.contains(queryLowerCase) ?: false
        return titleContainsQuery || descriptionContainsQuery
    }


    private fun parseDate(dateString: String): Date {
        // Parse the date string to a Date object using SimpleDateFormat
        val dateFormat = SimpleDateFormat("dd.MM.yyyy.", Locale.getDefault())
        return dateFormat.parse(dateString) ?: Date(0)
    }
}