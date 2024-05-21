package com.example.levkomandroid.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.levkomandroid.LevkomApplication
import com.example.levkomandroid.R
import com.example.levkomandroid.databinding.FragmentBaseRouteBinding
import com.example.levkomandroid.network.Address
import com.example.levkomandroid.network.DeliveryAddr
import com.example.levkomandroid.viewmodel.LevkomViewModel
import com.example.levkomandroid.viewmodel.LevkomViewModelFactory
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.gson.Gson
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.*
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider

open class BaseRouteFragment : Fragment() {
    protected val viewModel: LevkomViewModel by activityViewModels {
        LevkomViewModelFactory(
            (activity?.application as LevkomApplication).levkomRepository
        )
    }
    protected lateinit var binding: FragmentBaseRouteBinding
    protected lateinit var mapView: MapView
    // Client for interacting with the fused location provider
    protected lateinit var fusedLocationClient: FusedLocationProviderClient
    // Callback for receiving location updates
    protected lateinit var locationCallback: LocationCallback
    // Temporary storage for addresses to import
    protected var addressesToImport: List<Address>? = null
    // ID for the current route, default is invalid
    protected var currentRouteId: Int = -1
    // Required permissions for the app
    private val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    // Constants for different services
    private val SERVICE_LAST_KNOWN_POSITION = 100
    private val SERVICE_POSITION_TRACKING = 101
    // Request codes for permissions and settings
    private val REQUEST_PERMISSIONS_CALLBACK = 1
    private val REQUEST_CHECK_SETTINGS = 2
    // Flags to manage location updates
    private var requestingLocationUpdates = false
    private var requestedService = -1


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        binding = FragmentBaseRouteBinding.inflate(inflater, container, false)
        Configuration.getInstance().userAgentValue = requireContext().packageName

        // Retrieve and handle the current route ID from fragment arguments
        arguments?.let {
            currentRouteId = it.getInt("routeId", -1)
        }

        // Initialize map settings and overlays
        initMap()
//        setupButtons()
        restoreLocationUpdatesState(savedInstanceState)

        // Setup location services client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        setupLocationCallback()

        // Setup UI interactions for file selection and address import
        binding.btnSelectFile.setOnClickListener {
            openFileChooser()
        }
        binding.btnImport.setOnClickListener {
            importAddresses()
        }

        // Initialize RecyclerView for displaying addresses
        binding.rvAddresses.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = AddressAdapter() // Initially empty list
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load addresses for the specified route when the view is created
        viewModel.loadAddressesForRoute(currentRouteId)

        // Observe changes in addresses and update UI accordingly
        viewModel.addresses.observe(viewLifecycleOwner) { addresses ->
            (binding.rvAddresses.adapter as AddressAdapter).submitList(addresses)
            plotAddressesOnMap(addresses)
        }
        checkPositionPermissions()
    }

    // Plots addresses on the map using markers
    private fun plotAddressesOnMap(addresses: List<DeliveryAddr>) {
        val geoPoints = addresses.map { GeoPoint(it.latitude, it.longitude) }
        val markersLayer = ItemizedOverlayWithFocus<OverlayItem>(requireContext(), ArrayList(), null)
        geoPoints.forEachIndexed { index, geoPoint ->
            val marker = OverlayItem("$index", "Address $index", geoPoint)
            markersLayer.addItem(marker)
        }

        mapView.overlays.add(markersLayer)
        mapView.invalidate()

        // Adjust map view to include all markers
        if (geoPoints.isNotEmpty()) {
            val minLatitude = geoPoints.minByOrNull { it.latitude }?.latitude ?: 0.0
            val maxLatitude = geoPoints.maxByOrNull { it.latitude }?.latitude ?: 0.0
            val minLongitude = geoPoints.minByOrNull { it.longitude }?.longitude ?: 0.0
            val maxLongitude = geoPoints.maxByOrNull { it.longitude }?.longitude ?: 0.0

            val boundingBox = BoundingBox(maxLatitude, maxLongitude, minLatitude, minLongitude)
            mapView.zoomToBoundingBox(boundingBox, true, 50) // 50 - padding in pixels
        }
    }

    // Opens a file chooser to select a JSON file for importing addresses
    private fun openFileChooser() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "application/json"
        startActivityForResult(intent, FILE_SELECT_CODE) // Using the defined constant
    }

    // Handles the result from file chooser
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILE_SELECT_CODE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                readJsonFile(uri)
            }
        }
    }

    // Reads a JSON file and parses addresses to be imported
    private fun readJsonFile(uri: Uri) {
        val inputStream = requireContext().contentResolver.openInputStream(uri)
        val jsonString = inputStream?.bufferedReader().use { it?.readText() }
        addressesToImport = Gson().fromJson(jsonString, Array<Address>::class.java).toList()

        // Update UI based on file content
        if (addressesToImport?.isNotEmpty() == true) {
            binding.tvFileName.text = uri.lastPathSegment  // show file name
            binding.btnImport.isEnabled = true  // Enable import button
        } else {
            binding.tvFileName.text = "No valid data found"
            binding.btnImport.isEnabled = false
        }
    }

    // Imports addresses into the route
    private fun importAddresses() {
        addressesToImport?.let { addresses ->
            if (addresses.isNotEmpty()) {
                viewModel.importAddresses(addresses, currentRouteId)
                Toast.makeText(context, "Addresses imported successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "No addresses to import", Toast.LENGTH_SHORT).show()
            }
            binding.btnImport.isEnabled = false // Disable button after use
        } ?: run {
            Toast.makeText(context, "Please select a file first", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val FILE_SELECT_CODE = 1000 // Unique identifier for file chooser intent
    }

    // Initializes the map and overlays
    protected open fun initMap() {
        mapView = binding.map
        mapView.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.ALWAYS)
            setMultiTouchControls(true)
            controller.apply {
                setZoom(9.5)
                setCenter(GeoPoint(68.3675, 17.3391))
            }
        }
        addMapOverlays()
    }

    // Adds overlays such as compass and scale bar to the map
    private fun addMapOverlays() {
        val compassOverlay = CompassOverlay(requireContext(), InternalCompassOrientationProvider(requireContext()), mapView).apply {
            enableCompass()
        }
        mapView.overlays.add(compassOverlay)

        val scaleBarOverlay = ScaleBarOverlay(mapView).apply {
            setCentred(true)
            setScaleBarOffset(resources.displayMetrics.widthPixels / 2, 10)
        }
        mapView.overlays.add(scaleBarOverlay)

        // Responds to map events such as single taps and long presses
        val eventsReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(geoPoint: GeoPoint): Boolean {
                addMarker(geoPoint, R.drawable.ic_geopoint2)
                return false
            }

            override fun longPressHelper(geoPoint: GeoPoint): Boolean {
                return false
            }
        }
        mapView.overlays.add(MapEventsOverlay(eventsReceiver))
    }

    // Sets up buttons for location services and tracking
//    private fun setupButtons() {
//        binding.btnPosition.setOnClickListener {
//            requestedService = SERVICE_LAST_KNOWN_POSITION
//            checkPositionPermissions()
//        }
//
//        binding.btnStartTracking.setOnClickListener {
//            requestedService = SERVICE_POSITION_TRACKING
//            requestingLocationUpdates = true
//            checkPositionPermissions()
//        }
//
//        binding.btnStopTracking.setOnClickListener {
//            stopLocationUpdates()
//        }
//    }

    // Restores the state of location updates when the view is recreated
    private fun restoreLocationUpdatesState(savedInstanceState: Bundle?) {
        savedInstanceState?.let {
            if (it.containsKey("REQUESTING_LOCATION_UPDATES_KEY")) {
                requestingLocationUpdates = it.getBoolean("REQUESTING_LOCATION_UPDATES_KEY")
            }
        }
    }

    // Configures the callback for receiving location updates
    protected open fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.locations.forEach { location ->
                    updateLocationUI(location)
                }
            }
        }
    }

    // Updates the UI with the current location information
    private fun updateLocationUI(location: Location) {
        val posString = "Sporing:\nLat: ${location.latitude}\nLng: ${location.longitude}"
//        binding.tvTracking.text = posString
        val geoPoint = GeoPoint(location.latitude, location.longitude)
        mapView.controller.setCenter(geoPoint)
        addMarker(geoPoint, R.drawable.ic_rdot)
    }

    // Adds a marker to the map at the specified geopoint
    private fun addMarker(geoPoint: GeoPoint, drawableId: Int) {
        Marker(mapView).apply {
            position = geoPoint
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            icon = resources.getDrawable(drawableId, null)
            title = "Klikkpunkt"
            mapView.overlays.add(this)
        }
    }

    // Lifecycle methods to manage location updates
    override fun onResume() {
        super.onResume()

        if (requestingLocationUpdates) startLocationUpdates()
        mapView.onResume() // needed for compass, my location overlays, v6.0.0 and up



    }

    override fun onPause() {
        super.onPause()
        if (requestingLocationUpdates) stopLocationUpdates()
        mapView.onPause() // needed for compass, my location overlays, v6.0.0 and up
    }

    // Saves the state of location updates
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("REQUESTING_LOCATION_UPDATES_KEY", requestingLocationUpdates)
    }

    // Checks if location permissions are granted
//    private fun checkPositionPermissions() {
//        when {
//            hasPermissions() -> checkLocationRequirements()
//            shouldShowRequestPermissionRationale(permissions[0]) -> showPositionPermissionRationale()
//            else -> requestPositionPermission()
//        }
//    }
    private fun checkPositionPermissions() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ->
                startLocationUpdates()
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) ->
                showPositionPermissionRationale()
            else ->
                requestPositionPermission()
        }
    }

    // Requests location permissions
    private fun requestPositionPermission() {
        ActivityCompat.requestPermissions(requireActivity(), permissions, REQUEST_PERMISSIONS_CALLBACK)
    }

    // Shows rationale for requesting position permissions
    private fun showPositionPermissionRationale() {
        AlertDialog.Builder(requireContext()).apply {
            setTitle("Position permission needed")
            setMessage("This app requires permission to use your location to function properly.")
            setPositiveButton("Grant permission") { _, _ -> requestPositionPermission() }
            setNegativeButton("Cancel", null)
            show()
        }
    }

    // Checks if the required permissions are granted
    private fun hasPermissions() = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    // Handles the result of permission request
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSIONS_CALLBACK && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            checkLocationRequirements()
        } else {
            Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    // Starts location updates with specific settings
    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    // Stops location updates
    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        requestingLocationUpdates = false
//        binding.tvTracking.text = ""
        requestedService = -1
    }

    // Gets the last known position if permissions are granted
    @SuppressLint("MissingPermission")
    private fun getLastKnownDevicePosition() {
        if (hasPermissions()) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        updateLocationUI(location)
                    } else {
                        Toast.makeText(requireContext(), "No location detected. Make sure location is enabled on the device.", Toast.LENGTH_LONG).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed to get location.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // Checks if the location settings are adequate for the location request
    private fun checkLocationRequirements() {
        val locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        LocationServices.getSettingsClient(requireContext()).checkLocationSettings(builder.build()).apply {
            addOnSuccessListener { handleLocationSettingsResponse(true) }
            addOnFailureListener { exception -> handleLocationSettingsFailure(exception) }
        }
    }

    // Handles response for location settings check
    private fun handleLocationSettingsResponse(success: Boolean) {
        if (success) {
            when (requestedService) {
                SERVICE_LAST_KNOWN_POSITION -> getLastKnownDevicePosition()
                SERVICE_POSITION_TRACKING -> startLocationUpdates()
            }
        }
    }

    // Handles failures in checking location settings
    private fun handleLocationSettingsFailure(exception: Exception) {
        if (exception is ResolvableApiException) {
            try {
                exception.startResolutionForResult(requireActivity(), REQUEST_CHECK_SETTINGS)
            } catch (sendEx: IntentSender.SendIntentException) {
                // Ignore the error.
            }
        }
    }
}