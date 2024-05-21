package com.example.levkomandroid.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast

class NewRouteFragment : BaseRouteFragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        // Initialize the view and base fragment components
        super.onCreateView(inflater, container, savedInstanceState)

        // Set up observers
        setupObservers()

        // Create a new route when the fragment is created
        viewModel.createNewRoute()

        return binding.root
    }

    private fun setupObservers() {
        // Observe changes in the new route ID
        viewModel.newRouteId.observe(viewLifecycleOwner) { newRouteId ->
            if (newRouteId != null && newRouteId > 0) {
                // Display the new route ID and potentially update UI elements
                Toast.makeText(context, "New Route ID: $newRouteId created successfully.", Toast.LENGTH_LONG).show()
                binding.tvRouteId.text = "Route ID: $newRouteId"  // Assuming tvRouteId is a TextView in your layout
            } else {
                Toast.makeText(context, "Failed to create new route.", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun initMap() {
        super.initMap()
        // Additional map setup specific to NewRouteFragment if needed
    }

    override fun setupLocationCallback() {
        super.setupLocationCallback()
        // Custom location callback setup for NewRouteFragment if necessary
    }
}