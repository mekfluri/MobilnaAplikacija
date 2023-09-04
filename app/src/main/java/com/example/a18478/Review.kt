package com.example.a18478

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

data class Review(
    val userId: String = "",
    val rating: Float = 0.0f,
    val comment: String = ""
) {

    constructor() : this("", 0.0f, "")
}

class ReviewSubmissionFragmentt : Fragment() {

}

