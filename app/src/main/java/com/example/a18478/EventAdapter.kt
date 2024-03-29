package com.example.a18478

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.*

class EventAdapter(private var events: List<Event>) :
    RecyclerView.Adapter<EventAdapter.ViewHolder>() {

    public var filteredEventsList: List<Event> = events.toMutableList() //ne treba ti bukv

    // ViewHolder za čuvanje referenci na prikazane elemente unutar izgleda događaja
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val eventTitle: TextView = itemView.findViewById(R.id.eventTitleTextView)
        val eventLocation: TextView = itemView.findViewById(R.id.eventLocationTextView)
        val eventTime: TextView = itemView.findViewById(R.id.eventTimeTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_event, parent, false)
        return ViewHolder(itemView)
    }//poziva se samo jednom za svaku stavku

    //poziva se svaki put kada viewholder treba da se popuni novim podacima
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val event = events[position]
        holder.eventTitle.text = event.eventType

        val creatorUserId = event.creatorUserId
        val databaseRef = FirebaseDatabase.getInstance("https://project-4778345136366669416-default-rtdb.europe-west1.firebasedatabase.app")
            .reference
        val usernameRef = databaseRef.child("korisnici").child(creatorUserId).child("korisnickoIme")

        usernameRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val authorUsername = snapshot.getValue(String::class.java)
                holder.eventLocation.text = "Autor: $authorUsername"
            }

            override fun onCancelled(error: DatabaseError) {
                // Obrada greške u vezi sa bazom podataka ovde
            }
        })

        holder.eventTime.text = "${event.date} ${event.time}"
        holder.itemView.setOnClickListener {
            // Obrada klika na elementu, npr. otvaranje EventDetailsActivity sa kliknutim događajem
            val intent = Intent(holder.itemView.context, EventDetailsActivity::class.java)
            intent.putExtra(EventDetailsActivity.EXTRA_EVENT, event)
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return events.size
    }

    fun updateEvents(newEvents: List<Event>) {
        events = newEvents
        notifyDataSetChanged()
    }
}
