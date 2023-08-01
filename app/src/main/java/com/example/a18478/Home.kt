package com.example.a18478


import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.OnSuccessListener

class Home : AppCompatActivity(), OnMapReadyCallback {
    private var googleMap: GoogleMap? = null
    private var fusedLocationClient: FusedLocationProviderClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Dohvati referencu na MapView
        val mapView = findViewById<MapView>(R.id.mapView2)

        // Obavezno pozvati ovu metodu kako biste mogli manipulirati MapView-om
        mapView.onCreate(savedInstanceState)

        // Konfiguriraj MapView
        mapView.getMapAsync(this)

        // Inicijaliziraj FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }


    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap

        // Postavite opcije za prikazivanje mape
        googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL

        // Provjerite dozvole za pristup lokaciji i zatražite ih ako nisu već dozvoljene
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Ako su dozvole već odobrene, omogućite mogućnost prikaza trenutne lokacije
            googleMap.isMyLocationEnabled = true

            // Dohvati trenutnu lokaciju korisnika i postavi kameru na tu lokaciju
            fusedLocationClient?.lastLocation
                ?.addOnSuccessListener(this, OnSuccessListener<Location?> { location ->
                    if (location != null) {
                        val latitude: Double = location.latitude
                        val longitude: Double = location.longitude
                        val currentLocation = LatLng(latitude, longitude)
                        googleMap.moveCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                currentLocation,
                                15f
                            )
                        )
                    }
                })
        } else {
            // Ako dozvole nisu odobrene, zatražite ih od korisnika
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
        }
    }

}
