package com.example.a18478

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.a18478.databinding.ActivityRegistracijaBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.Manifest
import android.util.Log


class Registracija : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityRegistracijaBinding
    private val korisnikMap: HashMap<String, Any> = HashMap()
    private val CAMERA_ACTION_CODE = 1
    val CAMERA_PERMISSION_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        binding = ActivityRegistracijaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.registracijaBtn.setOnClickListener {
            registerUser()
        }
        binding.izaberiSlikuBtn.setOnClickListener {
            openCamera()
        }
    }

    private fun openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            // Permission already granted, open the camera
            Log.d("CameraApp", "Camera permission already granted. Opening camera...")

            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (intent.resolveActivity(packageManager) != null) {
                Log.d("CameraApp", "Found camera app. Starting camera intent...")
                startActivityForResult(intent, CAMERA_ACTION_CODE)
            } else {
                Log.e("CameraApp", "No camera app found.")
            }
        } else {
            // Request camera permission
            Log.d("CameraApp", "Requesting camera permission...")

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, open the camera
                openCamera()
            } else {
                // Permission denied
                Toast.makeText(this, "Camera permission denied.", Toast.LENGTH_SHORT).show()
            }
        }
    }





    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CAMERA_ACTION_CODE && resultCode == RESULT_OK && data != null) {
            val bundle: Bundle? = data.extras
            val finalPhoto = bundle?.get("data") as Bitmap?
            binding.imageView3.setImageBitmap(finalPhoto)

            // Process the image asynchronously using coroutines
            CoroutineScope(Dispatchers.IO).launch {
                saveImageToStorage(finalPhoto)
            }
        }
    }

    private suspend fun saveImageToStorage(image: Bitmap?) {
        // Perform image-saving operations here
        withContext(Dispatchers.IO) {
            // Save the image to storage
            // You can use methods like FileOutputStream to save the image
        }
    }

    private fun saveUserDataToDatabase(userId: String) {
        val database: DatabaseReference = FirebaseDatabase.getInstance(
            "https://project-4778345136366669416-default-rtdb.europe-west1.firebasedatabase.app"
        ).reference

        korisnikMap["poeni"] = 0
        korisnikMap["notifikacija"]=0

        database.child("korisnici").child(userId).setValue(korisnikMap)
            .addOnCompleteListener { dbTask ->
                if (dbTask.isSuccessful) {
                    Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@Registracija, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Error saving data", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun registerUser() {
        val korisnickoIme = binding.korisnickoTV.text.toString()
        val sifra = binding.sifraTV.text.toString()
        val ime = binding.imeTV.text.toString()
        val prezime = binding.prezimeTV.text.toString()
        val telefon = binding.telefonTV.text.toString()

        auth.createUserWithEmailAndPassword("$korisnickoIme@example.com", sifra)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        val userId = user.uid
                        korisnikMap["korisnickoIme"] = korisnickoIme
                        korisnikMap["ime"] = ime
                        korisnikMap["prezime"] = prezime
                        korisnikMap["telefon"] = telefon


                        saveUserDataToDatabase(userId)
                    }
                } else {
                    Toast.makeText(this, "Registration error", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
