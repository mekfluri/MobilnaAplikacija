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
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
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

    private val LOCATION_REQUEST_INTERVAL = 10000L // Interval ažuriranja u milisekundama (npr. 10 sekundi)
    private val LOCATION_REQUEST_FASTEST_INTERVAL = 5000L // Najbrži interval ažuriranja u milisekundama (npr. 5 sekundi)

    // Deklarišite zahtev za lokaciju i druge varijable
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private val DEFAULT_ZOOM = 0.0f




    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private var selectedMarker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        allEventsList = mutableListOf()
        filterOptionsContainer = findViewById(R.id.filterOptionsContainer)
        spinnerEventType = findViewById(R.id.spinner_event_types)
        editTextStartDate = findViewById(R.id.editTextStartDate)
        btnApplyFilter = findViewById(R.id.btnApplyFilter)
        checkBoxNearby = findViewById(R.id.checkBoxNearby)
        mapViewModel = ViewModelProvider(this).get(MapViewModel::class.java)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this) //Za prikaz mape

        eventsRef = FirebaseDatabase.getInstance("https://project-4778345136366669416-default-rtdb.europe-west1.firebasedatabase.app").getReference("events")

        if (ContextCompat.checkSelfPermission( //provera dozvole za pristup lokaciji uredjaja
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

        // Proveri dozvolu za lokaciju
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
            mapFragment.getMapAsync { googleMap ->
                mMap = googleMap
                mMap.isMyLocationEnabled = true
                if (allEventsList == null) {
                    allEventsList = mutableListOf()
                }
                // Kreirajte zahtev za lokaciju
            locationRequest = LocationRequest.create().apply {
                interval = LOCATION_REQUEST_INTERVAL //na svakih 10sec
                fastestInterval = LOCATION_REQUEST_FASTEST_INTERVAL
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            }

                //  povratni poziv lokacije
            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    locationResult.lastLocation?.let { location ->
                        val userLatLng = LatLng(location.latitude, location.longitude)
                        mapViewModel.userLocation = userLatLng

                        // Ažurirajte mapu ili izvršite bilo koju drugu radnju sa korisnikovom lokacijom ovde
                        //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, DEFAULT_ZOOM))

                        // Proverite blizinu događaja i prikažite iskačući prozor
                        val proximityThreshold = 100 // U metrima

                        val allEventsListCopy = ArrayList(allEventsList)

                        for (event in allEventsListCopy) {
                            val eventLocation = Location("")
                            eventLocation.latitude = event.latitude
                            eventLocation.longitude = event.longitude

                            val distance = location.distanceTo(eventLocation) //izracunaj distancu



                            // ako je u blizini korisnika ispisi tip dogadjaja
                            if (distance <= proximityThreshold) {
                                Snackbar.make(
                                    findViewById(android.R.id.content),
                                    "Blizu vas je ${event.eventType}!",
                                    Snackbar.LENGTH_SHORT
                                ).show()


                            }
                        }

                        clusterManager.cluster()

                    }
                }
            }

            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
        }
    }}




    override fun onRequestPermissionsResult( //ako korisnik odbije ili odobri koriscnenje lokacije
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) { //ako je odobrio
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    mMap.isMyLocationEnabled = true //prikazuje korisnikovu lokaciju
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

            // Izračunajte udaljenost i filtrirajte događaje unutar određenog radijusa (npr. 5 kilometara)
            val maxRadius = 5000 // u metrima, prikazuje evente na 5km od kosirnikove lokacije
            val nearbyEvents = allEventsList.filter { event ->
                val eventLatLng = LatLng(event.latitude, event.longitude)
                val distance = FloatArray(1)
                Location.distanceBetween(
                    currentLatLng.latitude, currentLatLng.longitude,
                    eventLatLng.latitude, eventLatLng.longitude, distance
                )
                distance[0] <= maxRadius
            }


            clusterManager.clearItems() // Obriši postojeće stavke iz ClusterManager-a
            for (event in nearbyEvents) {
                addEventMarker(event)
            }

            clusterManager.cluster() // Grupisanje novih filtriranih markera
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
                            it.eventId = eventId // Dodela id eventa (ne treba ti)
                            events.add(it)
                        }
                    }
                    allEventsList = events
                    for (event in allEventsList) {
                        addEventMarker(event)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {//prelazak na glavnu nit
                    Log.e("MapsActivity", "Neuspesno preuzimanje dogadjaja: ${e.message}")
                }
            }
        }
    }

    private fun openAddEventFragment(selectedLocation: LatLng) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.fragment_container, AddEventFragment.newInstance(selectedLocation)) //prosledjujes mu izabranu lokaciju
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
        clusterManager.cluster() //grupisanje markera u klastere
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
        intent.putExtra(EventDetailsActivity.EXTRA_EVENT, event) //prosledjujem event za koj treba da se otvori activity
        intent.putExtra(EventDetailsActivity.EXTRA_EVENT_ID, event.eventId) // Pass the eventId
        startActivity(intent)
    }


    private fun parseDate(dateString: String): Date {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy.", Locale.getDefault())
        return dateFormat.parse(dateString) ?: Date()
    }
    //Ne pozivas ovu funkciju nigde mozes da je obrises
    fun updateEventListAndMap() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Dobijanje trenutnog stanja događaja iz baze podataka
                val snapshot = eventsRef.get().await()
                val updatedEvents = mutableListOf<Event>()

                // Iteracija kroz sve događaje iz baze podataka
                snapshot.children.forEach { eventSnapshot ->
                    val eventId = eventSnapshot.key ?: ""
                    val event = eventSnapshot.getValue(Event::class.java)

                    // Ako je događaj uspešno pročitan, dodeljujemo mu eventId
                    event?.let {
                        it.eventId = eventId
                        updatedEvents.add(it)
                    }
                }

                // Ažuriranje liste svih događaja sa novim događajima
                allEventsList.clear()
                allEventsList.addAll(updatedEvents)

                // Ažuriranje markera na mapi i klasterisanje novih markera
                runOnUiThread {
                    clusterManager.clearItems() // Brisanje postojećih markera iz ClusterManager-a
                    for (event in allEventsList) {
                        addEventMarker(event) // Dodavanje novih markera na mapu
                    }
                    clusterManager.cluster() // Klasterisanje novih markera


                }
            } catch (e: Exception) {
                // U slučaju greške pri preuzimanju događaja, prijavljujemo grešku u LogCat
                runOnUiThread {
                    Log.e("MapsActivity", "Nije uspelo preuzimanje događaja: ${e.message}")
                }
            }
        }
    }



    private fun applyFilter() {
        val selectedEventType = spinnerEventType.selectedItem.toString()
        val startDateString = editTextStartDate.text.toString()
        val isNearbyChecked = checkBoxNearby.isChecked // Dobijanje stanja checkboxa
        val editTextAuthor = findViewById<EditText>(R.id.editTextAuthor).text.toString()
        val editTextDateOfMaking = findViewById<EditText>(R.id.editTextDateOfMaking).text.toString()

        val filteredEvents = if (isNearbyChecked) {
            // Primeni filter "2km od trenutne lokacije korisnika"
            val currentLatLng = LatLng(mMap.myLocation.latitude, mMap.myLocation.longitude)
            val maxRadius = 2000 // u metrima
            allEventsList.filter { event ->
                calculateDistance(
                    currentLatLng.latitude, currentLatLng.longitude,
                    event.latitude, event.longitude
                ) <= maxRadius
            }
        } else {
            allEventsList
        }.filter { event ->
            // Filtriranje prema vrsti događaja, datumu početka i datumu kreiranja
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
                // Filtriraj događaje sa null vrednostima za odabrane kriterijume za sortiranje
                val validDate = event.date != null || startDateString.isEmpty()
                val validCreator = !event.creatorUserId.isNullOrEmpty() || editTextAuthor.isEmpty()
                val validType = !event.eventType.isNullOrEmpty() || selectedEventType.isEmpty()
                val validDateOfMaking = !event.dateOfMaking.isNullOrEmpty() || editTextDateOfMaking.isEmpty()
                validDate && validCreator && validType && validDateOfMaking
            }.sortedWith(compareBy<Event> { event ->
                // Sortiraj po datumu ako je validan tj ako nije null
                if (event.date != null && startDateString.isNotEmpty()) {
                    Log.d("Sortiranje", "Sortiranje po datumu: ${event.date}")
                    event.date
                } else {
                    Log.d("Sortiranje", "Datum je null ili startDateString je prazan")
                    ""
                }
            }.thenBy { event ->
                // Sortiraj po creatorUserId ako je validan
                if (!event.creatorUserId.isNullOrEmpty() && editTextAuthor.isNotEmpty()) {
                    Log.d("Sortiranje", "Sortiranje po creatorUserId: ${event.creatorUserId}")
                    event.creatorUserId
                } else {
                    Log.d("Sortiranje", "creatorUserId je null ili editTextAuthor je prazan")
                    ""
                }
            }.thenBy { event ->
                // Sortiraj po vrsti događaja ako je validna
                if (!event.eventType.isNullOrEmpty() && selectedEventType.isNotEmpty()) {
                    Log.d("Sortiranje", "Sortiranje po vrsti događaja: ${event.eventType}")
                    event.eventType
                } else {
                    Log.d("Sortiranje", "eventType je null ili selectedEventType je prazan")
                    ""
                }
            }.thenBy { event ->
                // Sortiraj po datumu kreiranja ako je validan
                if (!event.dateOfMaking.isNullOrEmpty() && editTextDateOfMaking.isNotEmpty()) {
                    Log.d("Sortiranje", "Sortiranje po datumu kreiranja: ${event.dateOfMaking}")
                    event.dateOfMaking
                } else {
                    Log.d("Sortiranje", "dateOfMaking je null ili editTextDateOfMaking je prazan")
                    ""
                }
            }.thenBy { event ->
                // Sortiraj po isNearbyChecked
                if (!isNearbyChecked) {
                    Log.d("Sortiranje", "Sortiranje po isNearbyChecked: 1")
                    1
                } else {
                    Log.d("Sortiranje", "Sortiranje po isNearbyChecked: 0")
                    0
                }
            }).toMutableList()

            clusterManager.clearItems() // Obriši postojeće stavke iz ClusterManager-a
            for (event in sortedEvents) {
                addEventMarker(event)
            }

            clusterManager.cluster() // Klasterizuj nove filtrirane markere
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
                    // Greske
                    callback(null)
                }
            })
    }




    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results) //andriodova metoda za racunanje udaljenosti
        return results[0]
    }






}