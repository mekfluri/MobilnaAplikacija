//package com.example.a18478
//
//import android.app.usage.UsageEvents
//import androidx.lifecycle.ViewModelProvider
//import android.os.Bundle
//import androidx.fragment.app.Fragment
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import com.example.a18478.databinding.FragmentEventDetailsBinding
//import com.example.a18478.databinding.FragmentEventDetailsBinding.inflate
//
//private val FragmentEventDetailsBinding.root: View
//    get() {
//        TODO("Not yet implemented")
//    }
//
//class EventDetailsFragment : Fragment() {
//
//    private lateinit var viewModel: EventDetailsViewModel
//    private var _binding: FragmentEventDetailsBinding? = null
//    private val binding get() = _binding!!
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        _binding = inflate(inflater, container, false)
//        return binding.root
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        val eventId = arguments?.getString("eventId") // Assuming you pass eventId as an argument
//
//        // Initialize ViewModel
//        viewModel = ViewModelProvider(this).get(EventDetailsViewModel::class.java)
//
//        // Use ViewModel to fetch event details from the database based on eventId
//        viewModel.getEventDetails(eventId).observe(viewLifecycleOwner, { event ->
//            // Update the UI with event details
//            event?.let {
//                updateUI(it)
//            }
//        })
//    }
//
//    private fun updateUI(event: UsageEvents.Event) {
//        // Update your UI components (TextViews, ImageViews, etc.) with event details
//        binding.eventTitleTextView.text = event.eventType
//        binding.dateTextView.text = event.date
//        binding.timeTextView.text = event.time
//        binding.descriptionTextView.text = event.description
//        // Add other UI updates as needed based on your UI design
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        _binding = null
//    }
//}
