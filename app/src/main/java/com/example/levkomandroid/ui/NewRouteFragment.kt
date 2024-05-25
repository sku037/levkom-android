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
        // Set up observers
        setupObservers()

        // Create a new route when the fragment is created
        viewModel.createNewRoute()

        // Initialize the view and base fragment components
        val view = super.onCreateView(inflater, container, savedInstanceState)

        return view
    }

    private fun setupObservers() {
        // Observe changes in the new route ID
        viewModel.newRouteId.observe(viewLifecycleOwner) { newRouteId ->
            if (newRouteId != null && newRouteId > 0) {
                // Update currentRouteId with the new route ID
                currentRouteId = newRouteId
                // Display the new route ID and potentially update UI elements
                Toast.makeText(context, "New Route ID: $newRouteId created successfully.", Toast.LENGTH_LONG).show()
                binding.tvRouteId.text = "Route ID: $newRouteId"  // Assuming tvRouteId is a TextView in your layout
                // Now load addresses or perform other operations with the valid route ID
                viewModel.loadAddressesForRoute(newRouteId)
            } else {
                // Handle the error case where route creation failed
                Toast.makeText(context, "Failed to create new route.", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Additional actions that depend on the view being completely created can be placed here
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
