package com.example.a18478


import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.a18478.databinding.ActivityMapsBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterManager
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, ClusterManager.OnClusterClickListener<Event>,
    ClusterManager.OnClusterItemClickListener<Event>{

    private lateinit var mMap: GoogleMap
    private lateinit var filterOptionsContainer: LinearLayout
    private lateinit var spinnerEventType: Spinner
    private lateinit var editTextStartDate: EditText
    private lateinit var btnApplyFilter: Button
    private lateinit var clusterManager: ClusterManager<Event>
    private lateinit var eventsRef: DatabaseReference
    private lateinit var binding: ActivityMapsBinding
    private lateinit var allEventsList: List<Event>

    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private var selectedMarker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        filterOptionsContainer = findViewById(R.id.filterOptionsContainer)
        spinnerEventType = findViewById(R.id.spinnerEventType)
        editTextStartDate = findViewById(R.id.editTextStartDate)
        btnApplyFilter = findViewById(R.id.btnApplyFilter)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        eventsRef = FirebaseDatabase.getInstance("https://project-4778345136366669416-default-rtdb.europe-west1.firebasedatabase.app").getReference("events")

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }

        binding.addEventFab.setOnClickListener {
            selectedMarker?.position?.let { selectedLocation ->
                openAddEventFragment(selectedLocation)
            }
        }

        btnApplyFilter.setOnClickListener {
            applyFilter()
            filterOptionsContainer.visibility = View.GONE
        }
        val btnFilter = findViewById<ImageButton>(R.id.btnFilter)
        btnFilter.setOnClickListener {
            toggleFilterOptionsVisibility()
        }
        val btnUserProfile = findViewById<ImageButton>(R.id.btnUserProfile)
        btnUserProfile.setOnClickListener {
            openUserProfileActivity()
        }
    }
    private fun openUserProfileActivity() {
        val intent = Intent(this, UserProfile::class.java)
        startActivity(intent)
    }
    private fun toggleFilterOptionsVisibility() {
        if (filterOptionsContainer.visibility == View.VISIBLE) {
            filterOptionsContainer.visibility = View.GONE
        } else {
            filterOptionsContainer.visibility = View.VISIBLE
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    mMap.isMyLocationEnabled = true
                }
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        mMap.setOnMyLocationClickListener { location ->
            val currentLatLng = LatLng(location.latitude, location.longitude)
            mMap.addMarker(MarkerOptions().position(currentLatLng).title("Your Location"))
            mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLatLng))
            Log.d("Location", "Latitude: ${location.latitude}, Longitude: ${location.longitude}")
        }

        mMap.setOnMapClickListener { latLng ->
            selectedMarker?.remove()
            selectedMarker = mMap.addMarker(MarkerOptions().position(latLng).title("Selected Location"))
        }

        clusterManager = ClusterManager(this, mMap)
        mMap.setOnCameraIdleListener(clusterManager)
        mMap.setOnMarkerClickListener(clusterManager)

        clusterManager.setOnClusterClickListener(this)
        clusterManager.setOnClusterItemClickListener(this)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val snapshot = eventsRef.get().await()
                withContext(Dispatchers.Main) {
                    val events = mutableListOf<Event>()
                    snapshot.children.forEach { eventSnapshot ->
                        val eventId = eventSnapshot.key ?: ""
                        val event = eventSnapshot.getValue(Event::class.java)
                        event?.let {
                            it.eventId = eventId // Assign the eventId to the event object
                            events.add(it)
                        }
                    }
                    allEventsList = events
                    for (event in allEventsList) {
                        addEventMarker(event)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("MapsActivity", "Failed to fetch events: ${e.message}")
                }
            }
        }
    }

    private fun openAddEventFragment(selectedLocation: LatLng) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.fragment_container, AddEventFragment.newInstance(selectedLocation))
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.commit()
    }

    private fun addEventMarker(event: Event) {
        val location = LatLng(event.latitude, event.longitude)
        val markerOptions = MarkerOptions()
            .position(location)
            .title(event.eventType)
            .snippet("${event.date}, ${event.time}\n${event.description}")

        clusterManager.addItem(event)
        clusterManager.cluster()
    }

    override fun onClusterClick(cluster: Cluster<Event>): Boolean {
        return true
    }

    override fun onClusterItemClick(event: Event): Boolean {
        openEventDetailsActivity(event)
        return false
    }

    private fun openEventDetailsActivity(event: Event) {
        val intent = Intent(this, EventDetailsActivity::class.java)
        intent.putExtra(EventDetailsActivity.EXTRA_EVENT, event)
        intent.putExtra(EventDetailsActivity.EXTRA_EVENT_ID, event.eventId) // Pass the eventId
        startActivity(intent)
    }


    private fun parseDate(dateString: String): Date {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        return dateFormat.parse(dateString) ?: Date()
    }


    private fun applyFilter() {
        val selectedEventType = spinnerEventType.selectedItem.toString()
        val startDateString = editTextStartDate.text.toString()

        val filteredEvents = allEventsList.filter { event ->
            // Filter by event type and start date
            (selectedEventType.isEmpty() || selectedEventType == event.eventType) &&
                    (startDateString.isEmpty() || parseDate(event.date) >= parseDate(startDateString))
        }.sortedBy { event ->
            // Sort by date in ascending order
            parseDate(event.date).time
        }

        clusterManager.clearItems() // Clear existing items from ClusterManager
        for (event in filteredEvents) {
            addEventMarker(event)
        }

        clusterManager.cluster() // Cluster the new filtered markers
    }







}