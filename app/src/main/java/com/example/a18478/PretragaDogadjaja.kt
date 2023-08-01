package com.example.a18478

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

class PretragaDogadjaja : Fragment() {

    companion object {
        fun newInstance() = PretragaDogadjaja()
    }

    private lateinit var viewModel: PretragaDogadjajaViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_pretraga_dogadjaja, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(PretragaDogadjajaViewModel::class.java)
        // TODO: Use the ViewModel
    }

}