package com.example.a18478

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.a18478.databinding.FragmentAddEventBinding
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AddEventFragment : Fragment() {
    private var eventType: String? = null
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
    private fun addEventSuccess() {
        // Update the event list in the MapsActivity
        val mapsActivity = activity as? MapsActivity
        mapsActivity?.updateEventListAndMap()

        // Close the fragment
        requireActivity().supportFragmentManager.beginTransaction().remove(this).commit()
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val selectedLocation = arguments?.getParcelable<LatLng>(ARG_SELECTED_LOCATION)

        // Show the selected location on the map using a marker or any other visual representation.
        // You can also display the latitude and longitude values in TextViews for better clarity.
        val spinner2 = binding.spinner2
        val eventTypesArray = resources.getStringArray(R.array.event_types)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, eventTypesArray)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner2.adapter = adapter

        spinner2.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                eventType = eventTypesArray[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                eventType = null
            }
        }
        binding.saveButton.setOnClickListener {
            // Get event details and the selected location here and save it to the Realtime Database.

            val date = binding.dateEditText.text.toString()
            val time = binding.timeEditText.text.toString()
            val description = binding.descriptionEditText.text.toString()
            val currentUser = FirebaseAuth.getInstance().currentUser
            val userId = currentUser?.uid
            // Save the event to the Realtime Database under the "events" node
            val eventsRef = FirebaseDatabase.getInstance("https://project-4778345136366669416-default-rtdb.europe-west1.firebasedatabase.app").getReference("events")
            val eventKey = eventsRef.push().key
            eventKey?.let {
                val userId = FirebaseAuth.getInstance().currentUser?.uid // Get the current active user ID
                val event = Event(eventType!!, date, time, description, selectedLocation?.latitude ?: 0.0, selectedLocation?.longitude ?: 0.0, userId ?: "")

                eventsRef.child(it).setValue(event)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Dogadjaj sačuvan uspešno", Toast.LENGTH_SHORT).show()

                        // Get the current active user ID
                        val userId = FirebaseAuth.getInstance().currentUser?.uid

                        // Update the points attribute of the current active user
                        userId?.let { uid ->
                            val userRef = FirebaseDatabase.getInstance("https://project-4778345136366669416-default-rtdb.europe-west1.firebasedatabase.app").getReference("korisnici").child(uid)
                            userRef.child("poeni").addListenerForSingleValueEvent(object :
                                ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    val currentPoints = snapshot.getValue(Int::class.java) ?: 0
                                    val newPoints = currentPoints + 2
                                    userRef.child("poeni").setValue(newPoints)
                                        .addOnSuccessListener {

                                               }
                                        .addOnFailureListener {
                                            Toast.makeText(requireContext(), "Greška u dodavanju poena", Toast.LENGTH_SHORT).show()
                                        }
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    Toast.makeText(requireContext(), "Greška prilikom čuvanja poena", Toast.LENGTH_SHORT).show()
                                }
                            })
                        }
                        addEventSuccess()
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Greška prilikom čuvanja dogadjaja", Toast.LENGTH_SHORT).show()
                    }

            }
        }
    }

}
