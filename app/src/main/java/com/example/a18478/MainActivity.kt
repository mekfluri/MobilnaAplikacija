package com.example.a18478
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.FirebaseApp

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var signOutBtn: Button
    private lateinit var registerBtn: Button
    private lateinit var usernameTv: EditText
    private lateinit var passwordLoginTv: EditText
    private lateinit var loginBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()

        registerBtn = findViewById(R.id.registrujBtn)
        usernameTv = findViewById(R.id.korisnickoImeTv)
        passwordLoginTv = findViewById(R.id.sifraLoginTV)
        loginBtn = findViewById(R.id.loginBtn)


        registerBtn.setOnClickListener {
            startActivity(Intent(this, Registracija::class.java))
        }

        loginBtn.setOnClickListener {
            val username = usernameTv.text.toString().trim()
            val password = passwordLoginTv.text.toString().trim()


            if (username.isNotEmpty() && password.isNotEmpty()) {

                auth.signInWithEmailAndPassword("$username@example.com", password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {

                            startActivity(Intent(this, MapsActivity::class.java))
                        } else {

                            Toast.makeText(this, "Greška pri prijavi. Proverite da li ste ispravno uneli podatke.", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {

                Toast.makeText(this, "Unesite korisničko ime i šifru.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
