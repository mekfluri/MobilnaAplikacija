package com.example.a18478

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.a18478.databinding.FragmentAddEventBinding
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.FirebaseDatabase

class AddEventFragment : Fragment() {

    companion object {
        private const val ARG_SELECTED_LOCATION = "selected_location"

        fun newInstance(selectedLocation: LatLng): AddEventFragment {
            val args = Bundle()
            args.putParcelable(ARG_SELECTED_LOCATION, selectedLocation)
            val fragment = AddEventFragment()
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var binding: FragmentAddEventBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddEventBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val selectedLocation = arguments?.getParcelable<LatLng>(ARG_SELECTED_LOCATION)

        // Show the selected location on the map using a marker or any other visual representation.
        // You can also display the latitude and longitude values in TextViews for better clarity.

        binding.saveButton.setOnClickListener {
            // Get event details and the selected location here and save it to the Realtime Database.
            val eventType = binding.eventTypeEditText.text.toString()
            val date = binding.dateEditText.text.toString()
            val time = binding.timeEditText.text.toString()
            val description = binding.descriptionEditText.text.toString()

            // Save the event to the Realtime Database under the "events" node
            val eventsRef = FirebaseDatabase.getInstance("https://project-4778345136366669416-default-rtdb.europe-west1.firebasedatabase.app").getReference("events")
            val eventKey = eventsRef.push().key
            eventKey?.let {
                val event = Event(eventType, date, time, description, selectedLocation?.latitude ?: 0.0, selectedLocation?.longitude ?: 0.0)
                eventsRef.child(it).setValue(event)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Event saved successfully", Toast.LENGTH_SHORT).show()
                        // You can go back to the previous screen or perform any other actions here.
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Failed to save event", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }
}
