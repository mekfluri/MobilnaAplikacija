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

        // Ostali kod za postavljanje korisničkog interfejsa

    }

    // Funkcija za dohvatanje preferiranih tipova događaja korisnika
    private suspend fun fetchPreferredEventTypes(): List<String> {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val preferredEventTypes = mutableListOf<String>()

        // Dohvatanje događaja za trenutnog korisnika koristeći korutine
        currentUser?.let { user ->
            val userId = user.uid
            val userEventsRef = FirebaseDatabase.getInstance("https://project-4778345136366669416-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference("korisnici")
                .child(userId)
                .child("events")

            try {
                val snapshot = userEventsRef.get().await() // Koristimo await() da bismo asinhrono dobili rezultate

                for (eventSnapshot in snapshot.children) {
                    val eventKey = eventSnapshot.key
                    if (eventKey != null) {
                        val eventTypeRef = FirebaseDatabase.getInstance("https://project-4778345136366669416-default-rtdb.europe-west1.firebasedatabase.app")
                            .getReference("events")
                            .child(eventKey)
                            .child("eventType")

                        val eventTypeSnapshot = eventTypeRef.get().await() // Takođe koristimo await() ovde
                        val eventType = eventTypeSnapshot.getValue(String::class.java)
                        eventType?.let {
                            preferredEventTypes.add(it)
                        }
                    }
                }
            } catch (e: Exception) {
                // Obrada eventualnih izuzetaka
                // npr. logovanje greške ili prikazivanje poruke putem toasta
            }
        }

        return preferredEventTypes
    }

    // Funkcija za dohvatanje događaja preferiranih tipova
    private fun fetchEventsOfPreferredTypes(preferredEventTypes: List<String>) {
        eventsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                eventsList.clear()

                for (eventSnapshot in snapshot.children) {
                    val event = eventSnapshot.getValue(Event::class.java)
                    event?.let {
                        // Provera da li tip događaja odgovara preferiranim tipovima korisnika
                        if (it.eventType in preferredEventTypes) {
                            eventsList.add(it)
                        }
                    }
                }

                eventsAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                // Obrada greške u slučaju problema sa dohvatanjem podataka
                Toast.makeText(this@PretragaDogadjaja, "Neuspelo dohvatanje podataka: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }




}
