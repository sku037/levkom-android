package com.example.levkomandroid.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

class EditRouteFragment : BaseRouteFragment() {
    private var routeId: Int = 0
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Retrieve the routeId passed from RoutelistFragment using Safe Args
        arguments?.let {
            val args = EditRouteFragmentArgs.fromBundle(it)
            routeId = args.routeId
        }

        // Set routeId to arguments to get in com.example.levkomandroid.ui.BaseRouteFragment and assign to currentRouteId
        arguments = Bundle().apply {
            putInt("routeId", routeId)
        }
        super.onCreateView(inflater, container, savedInstanceState)

        binding.tvRouteId.text = "Route ID: $routeId"

        return binding.root
    }
}