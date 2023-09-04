package com.example.a18478

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class UserProfile : AppCompatActivity() {

    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>
    private val PERMISSION_REQUEST_CODE = 100
    private lateinit var imageViewProfilePicture: ImageView
    private lateinit var userId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)
        userId = FirebaseAuth.getInstance().currentUser!!.uid
        // Inicijalizacija prikaza i dugmadi
        val textViewUsername: TextView = findViewById(R.id.textViewUsername)

        val userScheduleFragment = UserScheduleFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.containerUserSchedule, userScheduleFragment)
            .commit()

        val btnGoToPretragaDogadjaja: Button = findViewById(R.id.btnGoToPretragaDogadjaja)
        val btnLogout: Button = findViewById(R.id.btnLogout)

        // Postavljanje klika na dugme za navigaciju ka aktivnosti PretragaDogadjaja
        btnGoToPretragaDogadjaja.setOnClickListener {
            val intent = Intent(this, PretragaDogadjaja::class.java)
            startActivity(intent)
        }

        // Provera i zahtevanje dozvole za čitanje spoljnog skladišta ako nije već odobreno
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                PERMISSION_REQUEST_CODE)
        }

        // Dohvatanje korisničkih podataka
        val userId = FirebaseAuth.getInstance().currentUser!!.uid
        val databaseURL = "https://project-4778345136366669416-default-rtdb.europe-west1.firebasedatabase.app"
        val database = FirebaseDatabase.getInstance(databaseURL)
        val databaseRef = database.reference.child("korisnici").child(userId)

        // Korišćenje Coroutines za dohvat podataka iz baze podataka
        lifecycleScope.launch {
            try {
                val dataSnapshot = databaseRef.get().await()
                if (dataSnapshot.exists()) {
                    val userData = dataSnapshot.getValue(User::class.java)
                    userData?.let {
                        val name = userData.ime
                        textViewUsername.text = name
                    }
                }
            } catch (e: Exception) {
                // Gracefulno rukovanje greškama
                textViewUsername.text = "Greška pri učitavanju korisničkih podataka"
            }
        }

        // Postavljanje klika na dugme za odjavu
        btnLogout.setOnClickListener {
            odjavaKorisnika()
        }
    }

    private fun odjavaKorisnika() {
        // Odjava trenutnog korisnika iz Firebase autentifikacije
        FirebaseAuth.getInstance().signOut()

        // Preusmeravanje korisnika na ekran za prijavu ili drugo željeno ponašanje
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish() // Zatvaranje trenutne aktivnosti
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // Dozvola je odobrena
                } else {

                }
                return
            }
            // Dodajte druge slučajeve po potrebi
        }
    }



    // Ostale metode ako ih imate
}
