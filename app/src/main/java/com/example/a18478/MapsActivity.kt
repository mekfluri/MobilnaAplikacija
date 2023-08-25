package com.example.a18478


import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.a18478.databinding.ActivityMapsBinding
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.*
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterManager
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
    private lateinit var allEventsList: MutableList<Event>
    private lateinit var checkBoxNearby: CheckBox
    private lateinit var mapViewModel: MapViewModel
    private val DEFAULT_ZOOM = 15.0f




    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private var selectedMarker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        filterOptionsContainer = findViewById(R.id.filterOptionsContainer)
        spinnerEventType = findViewById(R.id.spinner_event_types)
        editTextStartDate = findViewById(R.id.editTextStartDate)
        btnApplyFilter = findViewById(R.id.btnApplyFilter)
        checkBoxNearby = findViewById(R.id.checkBoxNearby)
        mapViewModel = ViewModelProvider(this).get(MapViewModel::class.java)

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
        val btnUserRanking= findViewById<Button>(R.id.btnUserRanking)
        btnUserRanking.setOnClickListener {
            val intent = Intent(this, UserRankingActivity::class.java)
            startActivity(intent)
        }
        val btnEventList=findViewById<Button>(R.id.btnAllEvents)
        btnEventList.setOnClickListener{
            val intent=Intent(this,EventListActivity::class.java)
            startActivity(intent)
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
    override fun onResume() {
        super.onResume()

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mMap.isMyLocationEnabled = true
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val userLatLng = LatLng(location.latitude, location.longitude)
                    mapViewModel.userLocation = userLatLng
                }
            }
        }
        updateEventListAndMap()
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
        mapViewModel.userLocation?.let { location ->
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, DEFAULT_ZOOM))
        }
        mMap.setOnMyLocationClickListener { location ->
            val currentLatLng = LatLng(location.latitude, location.longitude)
            mMap.addMarker(MarkerOptions().position(currentLatLng).title("Your Location"))
            mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLatLng))

            // Calculate distance and filter events within a certain radius (e.g., 5 kilometers)
            val maxRadius = 5000 // in meters
            val nearbyEvents = allEventsList.filter { event ->
                val eventLatLng = LatLng(event.latitude, event.longitude)
                val distance = FloatArray(1)
                Location.distanceBetween(
                    currentLatLng.latitude, currentLatLng.longitude,
                    eventLatLng.latitude, eventLatLng.longitude, distance
                )
                distance[0] <= maxRadius
            }


            clusterManager.clearItems() // Clear existing items from ClusterManager
            for (event in nearbyEvents) {
                addEventMarker(event)
            }

            clusterManager.cluster() // Cluster the new filtered markers
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
        val dateFormat = SimpleDateFormat("dd.MM.yyyy.", Locale.getDefault())
        return dateFormat.parse(dateString) ?: Date()
    }
    fun updateEventListAndMap() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val snapshot = eventsRef.get().await()
                val updatedEvents = mutableListOf<Event>()
                snapshot.children.forEach { eventSnapshot ->
                    val eventId = eventSnapshot.key ?: ""
                    val event = eventSnapshot.getValue(Event::class.java)
                    event?.let {
                        it.eventId = eventId // Assign the eventId to the event object
                        updatedEvents.add(it)
                    }
                }

                // Update the allEventsList with the updated events

                allEventsList.clear()
                allEventsList.addAll(updatedEvents)


                // Update the map markers
                runOnUiThread {
                    clusterManager.clearItems() // Clear existing items from ClusterManager
                    for (event in allEventsList) {
                        addEventMarker(event)
                    }
                    clusterManager.cluster() // Cluster the new markers
                    // Other UI updates if needed
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Log.e("MapsActivity", "Failed to fetch events: ${e.message}")
                }
            }
        }
    }


    private fun applyFilter() {
        val selectedEventType = spinnerEventType.selectedItem.toString()
        val startDateString = editTextStartDate.text.toString()
        val isNearbyChecked = checkBoxNearby.isChecked // Get the state of the checkbox
        val editTextAuthor = findViewById<EditText>(R.id.editTextAuthor).text.toString()
        val editTextDateOfMaking = findViewById<EditText>(R.id.editTextDateOfMaking).text.toString()

        val filteredEvents = if (isNearbyChecked) {
            // Apply the "2km ffrom user's current location" filter
            val currentLatLng = LatLng(mMap.myLocation.latitude, mMap.myLocation.longitude)
            val maxRadius = 2000 // in meters
            allEventsList.filter { event ->
                calculateDistance(
                    currentLatLng.latitude, currentLatLng.longitude,
                    event.latitude, event.longitude
                ) <= maxRadius
            }
        } else {
            allEventsList
        }.filter { event ->
            // Filter by event type, start date, and date of making
            val typeFilter = selectedEventType.isEmpty() || selectedEventType == event.eventType
            val dateFilter = startDateString.isEmpty() || event.date == startDateString
            val dateOfMakingFilter = editTextDateOfMaking.isEmpty() || event.dateOfMaking == editTextDateOfMaking
            typeFilter && dateFilter && dateOfMakingFilter
        }.toMutableList()

        findUserByUsername(editTextAuthor) { authorId ->
            if (!authorId.isNullOrEmpty()) {
                filteredEvents.retainAll { event ->
                    event.creatorUserId == authorId
                }
            }

            val sortedEvents = filteredEvents.filter { event ->
                // Filter out events with null values for selected sorting criteria
                val validDate = event.date != null || startDateString.isEmpty()
                val validCreator = !event.creatorUserId.isNullOrEmpty() || editTextAuthor.isEmpty()
                val validType = !event.eventType.isNullOrEmpty() || selectedEventType.isEmpty()
                val validDateOfMaking = !event.dateOfMaking.isNullOrEmpty() || editTextDateOfMaking.isEmpty()
                validDate && validCreator && validType && validDateOfMaking
            }.sortedWith(compareBy<Event> { event ->
                // Sort by date if valid
                if (event.date != null && startDateString.isNotEmpty()) {
                    Log.d("Sorting", "Sorting by date: ${event.date}")
                    event.date
                } else {
                    Log.d("Sorting", "Date is null or startDateString is empty")
                    ""
                }
            }.thenBy { event ->
                // Sort by creatorUserId if valid
                if (!event.creatorUserId.isNullOrEmpty() && editTextAuthor.isNotEmpty()) {
                    Log.d("Sorting", "Sorting by creatorUserId: ${event.creatorUserId}")
                    event.creatorUserId
                } else {
                    Log.d("Sorting", "creatorUserId is null or editTextAuthor is empty")
                    ""
                }
            }.thenBy { event ->
                // Sort by eventType if valid
                if (!event.eventType.isNullOrEmpty() && selectedEventType.isNotEmpty()) {
                    Log.d("Sorting", "Sorting by eventType: ${event.eventType}")
                    event.eventType
                } else {
                    Log.d("Sorting", "eventType is null or selectedEventType is empty")
                    ""
                }
            }.thenBy { event ->
                // Sort by dateOfMaking if valid
                if (!event.dateOfMaking.isNullOrEmpty() && editTextDateOfMaking.isNotEmpty()) {
                    Log.d("Sorting", "Sorting by dateOfMaking: ${event.dateOfMaking}")
                    event.dateOfMaking
                } else {
                    Log.d("Sorting", "dateOfMaking is null or editTextDateOfMaking is empty")
                    ""
                }
            }.thenBy { event ->
                // Sort by isNearbyChecked (if not nearby, push to the end)
                if (!isNearbyChecked) {
                    Log.d("Sorting", "Sorting by isNearbyChecked: 1")
                    1
                } else {
                    Log.d("Sorting", "Sorting by isNearbyChecked: 0")
                    0
                }
            }).toMutableList()

            clusterManager.clearItems() // Clear existing items from ClusterManager
            for (event in sortedEvents) {
                addEventMarker(event)
            }

            clusterManager.cluster() // Cluster the new filtered markers
        }
    }
    private fun findUserByUsername(username: String, callback: (String?) -> Unit) {
        val databaseReference = FirebaseDatabase.getInstance("https://project-4778345136366669416-default-rtdb.europe-west1.firebasedatabase.app/")
            .getReference("korisnici")

        databaseReference.orderByChild("korisnickoIme").equalTo(username)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val userSnapshot = snapshot.children.firstOrNull()
                    val userId = userSnapshot?.key
                    callback(userId)
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle errors, if any
                    callback(null)
                }
            })
    }




    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }






}