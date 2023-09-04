package com.example.a18478

import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng

class MapViewModel : ViewModel() {
    //cuvas korisnikovu lokaciju
    //koristi ViewModel komponentu kako bi te
    //informacije ostale nepromenjene prilikom promena ekrana
    // ili drugih konfiguracionih promena
    var userLocation: LatLng? = null
}
