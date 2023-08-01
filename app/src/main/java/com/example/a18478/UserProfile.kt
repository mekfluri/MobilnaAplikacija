package com.example.a18478

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.google.firebase.auth.FirebaseAuth

class UserProfile : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        // Show the UserScheduleFragment in the container
        if (savedInstanceState == null) {
            val fragment = UserScheduleFragment()
            supportFragmentManager.beginTransaction()
                .replace(R.id.containerUserSchedule, fragment)
                .commit()
        }

        // Set click listener for the logout button
        val btnLogout: Button = findViewById(R.id.btnLogout)
        btnLogout.setOnClickListener {
            signOutUser()
        }
    }

    private fun signOutUser() {
        // Sign out the current user from Firebase Authentication
        FirebaseAuth.getInstance().signOut()

        // Redirect the user to the login screen or any other desired behavior
        // For example, if you have a LoginActivity, you can use an Intent to start it.
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)

        // Finish the current activity to prevent the user from returning to the profile without login
        finish()
    }

    // ... Other code for the user profile activity
}
