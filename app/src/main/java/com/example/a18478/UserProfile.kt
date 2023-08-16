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
        // Initialize views and buttons
        val textViewUsername: TextView = findViewById(R.id.textViewUsername)

        val userScheduleFragment = UserScheduleFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.containerUserSchedule, userScheduleFragment) // Replace with your fragment container ID
            .commit()


        val btnGoToPretragaDogadjaja: Button = findViewById(R.id.btnGoToPretragaDogadjaja)
        val btnLogout: Button = findViewById(R.id.btnLogout)

        // Set click listener for the button to navigate to PretragaDogadjaja activity
        btnGoToPretragaDogadjaja.setOnClickListener {
            val intent = Intent(this, PretragaDogadjaja::class.java)
            startActivity(intent)
        }

        // Check if the permission is granted, and request it if not
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                PERMISSION_REQUEST_CODE)
        }

        // Retrieve user data
        val userId = FirebaseAuth.getInstance().currentUser!!.uid
        val databaseURL = "https://project-4778345136366669416-default-rtdb.europe-west1.firebasedatabase.app"
        val database = FirebaseDatabase.getInstance(databaseURL)
        val databaseRef = database.reference.child("korisnici").child(userId)

        // Use Coroutines to perform the database retrieval
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
                // Handle errors gracefully
                textViewUsername.text = "Error loading user data"
            }
        }

        // Set click listener for the logout button
        btnLogout.setOnClickListener {
            signOutUser()
        }
    }

    private fun signOutUser() {
        // Sign out the current user from Firebase Authentication
        FirebaseAuth.getInstance().signOut()

        // Redirect the user to the login screen or any other desired behavior
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish() // Close the current activity
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // Permission granted, proceed with your code
                } else {
                    // Permission denied, handle accordingly (e.g., show a message)
                }
                return
            }
            // Add other cases if needed
        }
    }

    private fun checkAndRequestPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("Permission", "Permission not granted. Requesting...")
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    // Other methods if you have them
}
