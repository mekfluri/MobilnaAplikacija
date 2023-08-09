
package com.example.a18478
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.a18478.databinding.ActivityRegistracijaBinding
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class Registracija : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var binding: ActivityRegistracijaBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        FirebaseApp.initializeApp(this)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://project-4778345136366669416-default-rtdb.europe-west1.firebasedatabase.app").reference



        binding = ActivityRegistracijaBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.registracijaBtn.setOnClickListener {
            registerUser()
        }
    }

    private fun registerUser() {
        val korisnickoIme = binding.korisnickoTV.text.toString()
        val sifra = binding.sifraTV.text.toString()
        val ime = binding.imeTV.text.toString()
        val prezime = binding.prezimeTV.text.toString()
        val telefon = binding.telefonTV.text.toString()

        // Koristite Firebase Authentication za registraciju korisnika
        auth.createUserWithEmailAndPassword("$korisnickoIme@example.com", sifra)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser

                    // Dodajte dodatne informacije o korisniku u Firebase Realtime Database
                    if (user != null) {
                        val userId = user.uid
                        val korisnikMap = HashMap<String, Any>()
                        korisnikMap["korisnickoIme"] = korisnickoIme
                        korisnikMap["ime"] = ime
                        korisnikMap["prezime"] = prezime
                        korisnikMap["telefon"] = telefon

                        // Add the new attribute with value 0
                        korisnikMap["poeni"] = 0

                        database.child("korisnici").child(userId).setValue(korisnikMap)
                            .addOnCompleteListener { dbTask ->
                                if (dbTask.isSuccessful) {
                                    // Korisnik je uspešno registrovan i podaci su sačuvani u bazi
                                    Toast.makeText(
                                        this,
                                        "Registracija uspešna!",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    // Open MainActivity
                                    val intent = Intent(this@Registracija, MainActivity::class.java)
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                                    startActivity(intent)
                                    finish() // Close the current activity
                                } else {
                                    // Greška prilikom čuvanja podataka u bazi
                                    Toast.makeText(
                                        this,
                                        "Greška pri čuvanju podataka!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                    }
                } else {
                    // Greška prilikom registracije korisnika
                    Toast.makeText(this, "Greška pri registraciji!", Toast.LENGTH_SHORT).show()
                }
            }

    }

}
