package com.example.a18478

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*

class UserScheduleFragment : Fragment() {
    // Initialize variables
    private lateinit var calendarView: MaterialCalendarView
    private lateinit var eventRecyclerView: RecyclerView
    private val eventList: MutableList<Event> = mutableListOf()
    private var eventAdapter: EventAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_user_schedule, container, false)

        // Bind views
        calendarView = view.findViewById(R.id.calendarView)
        eventRecyclerView = view.findViewById(R.id.eventRecyclerView)

        // Set up RecyclerView
        eventRecyclerView.layoutManager = LinearLayoutManager(context)

        // For the demo, let's show events for the current date initially
        val currentDate = Calendar.getInstance().time // Get the current date
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userId = currentUser?.uid
        if (userId != null) {
            fetchEventsForUser(userId)
        }

        return view
    }


    private fun fetchEventsForUser(userId: String) {
        val userEventsRef = FirebaseDatabase.getInstance("https://project-4778345136366669416-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference("korisnici")
            .child(userId)
            .child("events")

        userEventsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                eventList.clear()

                // Iterate through the event IDs and fetch the event details from the "events" node
                for (eventSnapshot in dataSnapshot.children) {
                    val eventId = eventSnapshot.key
                    if (eventId != null) {
                        // Fetch the event details from the "events" node using the eventId
                        val eventsRef = FirebaseDatabase.getInstance("https://project-4778345136366669416-default-rtdb.europe-west1.firebasedatabase.app")
                            .getReference("events")
                            .child(eventId)

                        eventsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(eventDataSnapshot: DataSnapshot) {
                                val event = eventDataSnapshot.getValue(Event::class.java)
                                event?.let {
                                    eventList.add(it)
                                    eventAdapter?.notifyDataSetChanged()
                                    Log.d("UserScheduleFragment", "Event fetched: ${event.eventType}, Date: ${event.date}, Time: ${event.time}")
                                }
                            }

                            override fun onCancelled(databaseError: DatabaseError) {
                                // Handle the error, if any.
                                Toast.makeText(requireContext(), "Failed to fetch events", Toast.LENGTH_SHORT).show()
                                Log.e("UserScheduleFragment", "Error fetching events: ${databaseError.message}")
                            }
                        })
                    }
                }

                // Initialize the eventAdapter with the updated eventList
                eventAdapter = EventAdapter(eventList)

                // Set the initialized eventAdapter to the RecyclerView
                eventRecyclerView.adapter = eventAdapter
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle the error, if any.
                Toast.makeText(requireContext(), "Failed to fetch events", Toast.LENGTH_SHORT).show()
                Log.e("UserScheduleFragment", "Error fetching events: ${databaseError.message}")
            }
        })
    }
}
