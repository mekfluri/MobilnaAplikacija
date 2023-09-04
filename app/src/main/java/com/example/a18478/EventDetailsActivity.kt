package com.example.a18478

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.a18478.databinding.ActivityEventDetailsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*

class EventDetailsActivity : AppCompatActivity() {

    // Definisana konstanta za ključ događaja
    companion object {
        const val EXTRA_EVENT = "extra_event"
        const val EXTRA_EVENT_ID = "extra_event_id"
    }

    private val reviewsList: MutableList<Review> = mutableListOf()
    private lateinit var binding: ActivityEventDetailsBinding
    private var event: Event? = null
    private lateinit var eventId: String
    private val eventsRef: DatabaseReference = FirebaseDatabase.getInstance("https://project-4778345136366669416-default-rtdb.europe-west1.firebasedatabase.app")
        .getReference("events")

    // Definisan RecyclerView adapter za prikaz recenzija
    private val reviewsAdapter: ReviewAdapter by lazy {
        ReviewAdapter(reviewsList)
    }

    // Definisan reviewsRef kao svojstvo klase
    private val reviewsRef: DatabaseReference by lazy {
        FirebaseDatabase.getInstance("https://project-4778345136366669416-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference("events")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEventDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        event = intent.getParcelableExtra(EXTRA_EVENT)
        eventId = intent.getStringExtra(EXTRA_EVENT_ID) ?: ""

        if (event != null) {
            // Prikaz detalja o događaju
            displayEventDetails()
        }

        // Postavljanje RecyclerView za prikaz recenzija
        binding.reviewsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@EventDetailsActivity)
            adapter = reviewsAdapter
        }
        val recenzijeButton: Button = findViewById(R.id.RecenzijeButton)
        recenzijeButton.setOnClickListener {
            val intent = Intent(this, UserReviewsActivity::class.java)
            intent.putExtra(UserReviewsActivity.EXTRA_EVENT_ID, eventId)
            startActivity(intent)
        }

        val btnBuyTicket: Button = findViewById(R.id.btnBuyTicket)
        val currentUser: FirebaseUser? = FirebaseAuth.getInstance().currentUser

        if (currentUser != null) {
            val eventUsersRef = FirebaseDatabase.getInstance("https://project-4778345136366669416-default-rtdb.europe-west1.firebasedatabase.app/")
                .getReference("events")
                .child(eventId)
                .child("users")

            eventUsersRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val hasBoughtTicket = snapshot.hasChild(currentUser.uid)

                    if (hasBoughtTicket) {
                        // Korisnik je već kupio kartu, sakrij dugme za "Kupi kartu"
                        btnBuyTicket.visibility = View.GONE
                    } else {
                        // Korisnik nije kupio kartu, prikaži dugme za "Kupi kartu"
                        btnBuyTicket.visibility = View.VISIBLE
                        btnBuyTicket.setOnClickListener {
                            // Obradi logiku za kupovinu karte
                            updateEventUserList(eventId, currentUser.uid)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("EventDetailsActivity", "Greška pri proveri prisustva korisnika: ${error.message}")
                }
            })
        } else {
            // Ako korisnik nije autentifikovan, obradi proces prijave
            // ...
        }

        val reviewBtn: Button = findViewById(R.id.reviewBtn)
        reviewBtn.setOnClickListener {
            // Kreiraj novu instancu ReviewSubmissionFragment
            val reviewFragment = ReviewSubmissionFragment.newInstance(eventId)

            // Zameni trenutni fragment u kontejneru sa novim ReviewSubmissionFragment-om
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, reviewFragment)
                .addToBackStack(null)
                .commit()
        }

        // Dohvati i prikaži recenzije
        fetchReviews(eventId)
    }

    // Funkcija za dohvatanje recenzija iz Firebase Realtime Database
    private fun fetchReviews(eventId: String) {
        val reviewsRef = reviewsRef.child(eventId).child("recenzije")

        reviewsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                reviewsList.clear()

                for (reviewSnapshot in snapshot.children) {
                    val review = reviewSnapshot.getValue(Review::class.java)
                    review?.let {
                        reviewsList.add(it)
                    }
                }

                // Izračunaj prosečnu ocenu
                val totalRating = reviewsList.sumByDouble { review -> review.rating.toDouble() }
                val averageRating = if (reviewsList.isNotEmpty()) totalRating / reviewsList.size else 0.0

                // Ažuriraj UI za prikaz prosečne ocene i broja recenzija
                binding.textViewAverageRating.text = String.format("%.2f", averageRating)
                binding.textViewNumberOfReviews.text = resources.getQuantityString(
                    R.plurals.numberOfReviews,
                    reviewsList.size,
                    reviewsList.size
                )

                reviewsAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("EventDetailsActivity", "Greška pri dohvatanju recenzija: ${error.message}")
            }
        })
    }

    // Ažuriranje liste korisnika koji prisustvuju događaju
    @SuppressLint("RestrictedApi")
    private fun updateEventUserList(eventKey: String, userId: String) {
        Log.d("EventDetailsActivity", "Ažuriranje liste korisnika za događaj: $eventKey, korisnik: $userId")

        val eventRef = FirebaseDatabase.getInstance("https://project-4778345136366669416-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference("events")
            .child(eventKey)
            .child("users")
            .child(userId)

        Log.d("EventDetailsActivity", "Putanja reference događaja: ${eventRef.path}")

        // Postavi vrednost na "true" (možete koristiti "false" ako želite da uklonite korisnika sa događaja)
        eventRef.setValue(true)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("EventDetailsActivity", "Ažuriranje korisnika za događaj je uspešno")

                    // Ako je ažuriranje uspešno, osveži aktivnost da prikaže ažuriranu listu korisnika
                    displayEventDetails()

                    // Dodaj informaciju o prisustvu korisnika u čvor korisnika
                    val userEventRef = FirebaseDatabase.getInstance("https://project-4778345136366669416-default-rtdb.europe-west1.firebasedatabase.app")
                        .getReference("korisnici")
                        .child(userId)
                        .child("events")
                        .child(eventKey)

                    Log.d("EventDetailsActivity", "Putanja reference događaja za korisnika: ${userEventRef.path}")

                    userEventRef.setValue(true)
                        .addOnSuccessListener {
                            Log.d("EventDetailsActivity", "Ažuriranje prisustva korisnika za događaj: $eventKey je uspešno")

                            // Osveži detalje događaja nakon što korisnik kupi kartu
                            displayEventDetails()
                        }
                        .addOnFailureListener { e ->
                            Log.e("EventDetailsActivity", "Neuspelo ažuriranje prisustva korisnika za događaj: $eventKey", e)
                        }

                } else {
                    // Obradi grešku ako ažuriranje nije uspelo
                    Log.e("EventDetailsActivity", "Neuspelo ažuriranje liste korisnika za događaj: $eventKey, korisnik: $userId")
                    // Na primer, prikaži poruku o grešci ili zabeleži poruku o grešci
                }
            }
    }

    // Prikaz detalja o događaju
    fun displayEventDetails() {
        // Popuni XML prikaze sa detaljima o događaju
        event?.let {
            binding.textViewEventType.text = it.eventType
            binding.textViewDate.text = it.date
            binding.textViewTime.text = it.time
            binding.textViewDescription.text = it.description

            // Pozovi funkciju za dohvatanje i prikaz imena korisnika
            fetchAndDisplayUserNames(eventId)
        }
    }

    // Funkcija za dohvatanje i prikaz imena korisnika koji prisustvuju nekom dogadjaju
    private fun fetchAndDisplayUserNames(eventKey: String) {
        val eventUsersRef = FirebaseDatabase.getInstance("https://project-4778345136366669416-default-rtdb.europe-west1.firebasedatabase.app/")
            .getReference("events")
            .child(eventKey)
            .child("users")

        eventUsersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val userIds = mutableListOf<String>()

                // Prikupi ID-jeve korisnika iz čvora "users" događaja
                for (userSnapshot in snapshot.children) {
                    val userId = userSnapshot.key
                    userId?.let { userIds.add(it) }
                }

                // Dohvati i prikaži imena korisnika
                fetchUsernames(userIds) { usernamesMap ->
                    val userListContainer = findViewById<LinearLayout>(R.id.userListContainer)

                    for ((userId, username) in usernamesMap) {
                        val userNameTextView = TextView(this@EventDetailsActivity)
                        userNameTextView.text = username
                        userListContainer.addView(userNameTextView)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("EventDetailsActivity", "Greška pri dohvatanju korisnika događaja: ${error.message}")
            }
        })
    }

    // Funkcija za dohvatanje imena korisnika
    private fun fetchUsernames(userIds: List<String>, callback: (Map<String, String>) -> Unit) {
        val usersRef = FirebaseDatabase.getInstance("https://project-4778345136366669416-default-rtdb.europe-west1.firebasedatabase.app/")
            .getReference("korisnici")

        val usernamesMap = mutableMapOf<String, String>()

        for (userId in userIds) {
            usersRef.child(userId).child("korisnickoIme")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val username = snapshot.getValue(String::class.java)
                        if (username != null) {
                            usernamesMap[userId] = username
                        }

                        if (usernamesMap.size == userIds.size) {
                            callback(usernamesMap)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("EventDetailsActivity", "Greška pri dohvatanju korisničkih imena: ${error.message}")
                    }
                })
        }
    }
}
