package com.example.a18478

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class UserRankingActivity : AppCompatActivity() {

    private val usersList: MutableList<UserRankingItem> = mutableListOf()
    private lateinit var rankingRecyclerView: RecyclerView
    private lateinit var rankingAdapter: UserRankingAdapter

    // Referenca ka Firebase bazi podataka za korisnike
    private val usersRef: DatabaseReference by lazy {
        FirebaseDatabase.getInstance("https://project-4778345136366669416-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference("korisnici")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_ranking)

        rankingRecyclerView = findViewById(R.id.rankingRecyclerView)
        rankingAdapter = UserRankingAdapter(usersList)
        rankingRecyclerView.adapter = rankingAdapter
        rankingRecyclerView.layoutManager = LinearLayoutManager(this)

        // Preuzmi i prikaži rangiranje korisnika
        preuzmiRangiranjeKorisnika()
    }

    private fun preuzmiRangiranjeKorisnika() {
        // Postavljanje redosleda korisnika prema poenima u Firebase bazi podataka
        usersRef.orderByChild("poeni").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                usersList.clear()

                // Iteriranje kroz korisnike i izvlačenje njihovih podataka
                for (userSnapshot in snapshot.children) {
                    val korisnickoIme = userSnapshot.child("korisnickoIme").getValue(String::class.java) ?: ""
                    val poeni = userSnapshot.child("poeni").getValue(Int::class.java) ?: 0
                    val stavkaRangiranjaKorisnika = UserRankingItem(korisnickoIme, poeni)
                    usersList.add(stavkaRangiranjaKorisnika)
                }

                // Sortiranje liste korisnika po opadajućem broju poena
                usersList.sortByDescending { it.points }

                // Obavesti adapter da je lista promenjena
                rankingAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                // Obrada greške ako ne uspe preuzimanje rangiranja korisnika
            }
        })
    }
}
