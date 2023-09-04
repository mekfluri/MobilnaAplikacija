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

import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import java.text.SimpleDateFormat

class UserScheduleFragment : Fragment() {
    // Inicijalizacija promenljivih
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

        // Povezivanje pogleda
        calendarView = view.findViewById(R.id.calendarView)
        eventRecyclerView = view.findViewById(R.id.eventRecyclerView)

        // Postavljanje RecyclerView
        eventRecyclerView.layoutManager = LinearLayoutManager(context)

        // Za demonstraciju, prikaži događaje za trenutni datum
        val currentDate = Calendar.getInstance().time // Dohvati trenutni datum
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userId = currentUser?.uid
        if (userId != null) {
            fetchEventsForUser(userId)
            // Postavljanje dekoratera za isticanje datuma sa događajima

            calendarView.addDecorator(object : DayViewDecorator {
                override fun shouldDecorate(day: CalendarDay?): Boolean {
                    val eventDates = eventList.mapNotNull { event ->
                        convertDateStringToDate(event.date)
                    }
                    return eventDates.any { eventDate -> eventDate == day?.date }
                }

                override fun decorate(view: DayViewFacade?) {
                    // Prilagodite kako će biti dekorisani datumi sa događajima (npr. promenite boju pozadine)
                    view?.setBackgroundDrawable(resources.getDrawable(R.drawable.rounded_background))
                }
            })



        }

        return view
    }
    fun convertDateStringToDate(dateString: String): Date? {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy.", Locale.getDefault())
        return try {
            dateFormat.parse(dateString)
        } catch (e: Exception) {
            null
        }
    }
    private fun fetchEventsForUser(userId: String) {
        val userEventsRef = FirebaseDatabase.getInstance("https://project-4778345136366669416-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference("korisnici")
            .child(userId)
            .child("events")

        userEventsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                eventList.clear()

                // Iteriraj kroz ID-jeve događaja i dohvati detalje događaja iz "events" čvora
                for (eventSnapshot in dataSnapshot.children) {
                    val eventId = eventSnapshot.key
                    if (eventId != null) {
                        // Dohvati detalje događaja iz "events" čvora koristeći eventId
                        val eventsRef = FirebaseDatabase.getInstance("https://project-4778345136366669416-default-rtdb.europe-west1.firebasedatabase.app")
                            .getReference("events")
                            .child(eventId)

                        eventsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(eventDataSnapshot: DataSnapshot) {
                                val event = eventDataSnapshot.getValue(Event::class.java)
                                event?.let {
                                    eventList.add(it)
                                    eventAdapter?.notifyDataSetChanged()
                                    Log.d("UserScheduleFragment", "Događaj dohvaćen: ${event.eventType}, Datum: ${event.date}, Vreme: ${event.time}")
                                }
                            }

                            override fun onCancelled(databaseError: DatabaseError) {
                                // Obradi grešku, ako se pojavi.
                                Toast.makeText(requireContext(), "Nije uspelo dohvatanje događaja", Toast.LENGTH_SHORT).show()
                                Log.e("UserScheduleFragment", "Greška pri dohvatanju događaja: ${databaseError.message}")
                            }
                        })
                    }
                }

                // Inicijalizuj eventAdapter sa ažuriranom eventList
                eventAdapter = EventAdapter(eventList)

                // Postavi inicijalizovani eventAdapter za RecyclerView
                eventRecyclerView.adapter = eventAdapter
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Obradi grešku, ako se pojavi.
                Toast.makeText(requireContext(), "Nije uspelo dohvatanje događaja", Toast.LENGTH_SHORT).show()
                Log.e("UserScheduleFragment", "Greška pri dohvatanju događaja: ${databaseError.message}")
            }
        })
    }
}
