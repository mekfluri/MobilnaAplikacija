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

        // Fetch and display user rankings
        fetchUserRankings()
    }

    private fun fetchUserRankings() {
        usersRef.orderByChild("poeni").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                usersList.clear()

                for (userSnapshot in snapshot.children) {
                    val username = userSnapshot.child("korisnickoIme").getValue(String::class.java) ?: ""
                    val points = userSnapshot.child("poeni").getValue(Int::class.java) ?: 0
                    val userRankingItem = UserRankingItem(username, points)
                    usersList.add(userRankingItem)
                }

                // Sort the usersList in descending order based on points
                usersList.sortByDescending { it.points }

                rankingAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle the error if fetching user rankings fails
            }
        })
    }

}
