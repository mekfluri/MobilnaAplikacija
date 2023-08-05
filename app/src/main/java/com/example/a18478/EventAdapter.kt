package com.example.a18478

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.*
class EventAdapter(private val events: List<Event>) :
    RecyclerView.Adapter<EventAdapter.ViewHolder>() {

    public var filteredEventsList: List<Event> = events.toMutableList()

    // ViewHolder to hold references to the views in the event item layout
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val eventTitle: TextView = itemView.findViewById(R.id.eventTitleTextView)
        val eventLocation: TextView = itemView.findViewById(R.id.eventLocationTextView)
        val eventTime: TextView = itemView.findViewById(R.id.eventTimeTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_event, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val event = events[position]
        holder.eventTitle.text = event.eventType
        holder.eventLocation.text = "Latitude: ${event.latitude}, Longitude: ${event.longitude}"
        holder.eventTime.text = "${event.date} ${event.time}"
    }

    override fun getItemCount(): Int {
        return events.size
    }
}
