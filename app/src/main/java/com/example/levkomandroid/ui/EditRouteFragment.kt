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
import com.example.levkomandroid.databinding.FragmentEditRouteBinding
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
import kotlin.math.max
import kotlin.math.min
// for icon creation
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

class EditRouteFragment : Fragment() {
    private lateinit var binding: FragmentEditRouteBinding
    private lateinit var mapView: MapView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var routeId: Int = 0
    private var addressesToImport: List<Address>? = null

    private val viewModel: LevkomViewModel by activityViewModels {
        LevkomViewModelFactory(
            (activity?.application as LevkomApplication).levkomRepository
        )
    }

    // ID for the current route, default is invalid
    private var currentRouteId: Int = -1
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentEditRouteBinding.inflate(inflater, container, false)
        Configuration.getInstance().userAgentValue = requireContext().packageName

        arguments?.let {
            routeId = it.getInt("routeId", -1)
        }
        binding.tvRouteId.text = "Route ID: $routeId"

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

        binding.btnCalculateRoute.setOnClickListener {
            calculateRoute()
        }

        // Initialize RecyclerView for displaying addresses
        binding.rvAddresses.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = AddressAdapter() // Initially empty list
        }
        setupSwipeListener(binding.rvAddresses, routeId)

        initMap()

        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //viewModel.loadAddressesForRoute(routeId)
        viewModel.loadAddressesAndRoutePublic(routeId)
        viewModel.addresses.observe(viewLifecycleOwner) { addresses ->
            (binding.rvAddresses.adapter as AddressAdapter).submitList(addresses)
            plotAddressesOnMap(addresses)
        }
        viewModel.routeGeometry.observe(viewLifecycleOwner) { geometry ->
            if (geometry.isNotEmpty()) {
                Log.d("RouteGeometry", "Geometry received: $geometry")
                plotRouteOnMap(geometry)
            } else {
                Log.d("RouteGeometry", "Empty geometry")
            }
        }
        checkPositionPermissions()
    }

    // Calculate route draw route
    private fun calculateRoute() {
        viewModel.calculateRoute(routeId)
    }

    private fun plotRouteOnMap(routeGeometry: String) {
        parseAndDrawRoute(routeGeometry, mapView)
        // Find first Polyline object in overlays
        mapView.overlays.firstOrNull { it is Polyline }?.let {
            val polyline = it as Polyline
            if (polyline.actualPoints.isNotEmpty()) {
                mapView.controller.setCenter(polyline.actualPoints.first())
                mapView.controller.setZoom(15)
            }
        }

        mapView.invalidate()
    }

    private fun parseAndDrawRoute(wktString: String, mapView: MapView) {
        val cleanWkt = wktString.removePrefix("MULTILINESTRING((").removeSuffix("))")
        val lines = cleanWkt.split("),(")

        lines.forEach { line ->
            val routeLayer = Polyline() // Create new object Polyline for each line
            routeLayer.width = 10f
            routeLayer.color = Color.parseColor("#FF0000")

            val points = line.trim().split(",")
            val geoPoints = points.map { coord ->
                val parts = coord.trim().split(" ")
                GeoPoint(parts[1].toDouble(), parts[0].toDouble())  // Lat, Lon order
            }
            routeLayer.setPoints(geoPoints)
            mapView.overlays.add(routeLayer) // Добавляем Polyline на карту
        }

        mapView.invalidate() // Обновить карту для отображения новых слоев
    }




    // Delete addresses from rvAddresses
    private fun setupSwipeListener(recyclerView: RecyclerView, routeId: Int) {
        val callback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                return false // Move operations are not supported
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val item = (binding.rvAddresses.adapter as AddressAdapter).currentList[viewHolder.adapterPosition]
                viewModel.deleteAddress(item.id, routeId)
                Toast.makeText(context, "Address deleted", Toast.LENGTH_SHORT).show()
            }
        }
        val itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    // Plots addresses on the map using markers
    private fun plotAddressesOnMap(addresses: List<DeliveryAddr>) {
        val markersLayer = ItemizedIconOverlay<OverlayItem>(requireContext(), ArrayList(), null)
        addresses.forEachIndexed { index, address ->
            val geoPoint = GeoPoint(address.latitude, address.longitude)
            val marker = Marker(mapView)
            marker.position = geoPoint
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.icon = createCustomMarkerIcon(index + 1) // Создать иконку с номером
            marker.title = "${index + 1}. ${address.street}, ${address.city}"

            mapView.overlays.add(marker)
        }

        mapView.invalidate()
    }

    private fun createCustomMarkerIcon(number: Int): BitmapDrawable {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 40f
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.color = Color.BLACK

        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        circlePaint.color = Color.YELLOW

        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawCircle(50f, 50f, 50f, circlePaint) // Рисуем жёлтый круг
        canvas.drawText(number.toString(), 50f, 65f, paint) // Добавляем номер

        return BitmapDrawable(resources, bitmap)
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
                val gson = Gson()
                val jsonContent = gson.toJson(addresses)
                viewModel.importAddresses(jsonContent, routeId)
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
    private fun initMap() {
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
//        val compassOverlay = CompassOverlay(requireContext(), InternalCompassOrientationProvider(requireContext()), mapView).apply {
//            enableCompass()
//        }
//        mapView.overlays.add(compassOverlay)

        val scaleBarOverlay = ScaleBarOverlay(mapView).apply {
            setCentred(true)
            setScaleBarOffset(resources.displayMetrics.widthPixels / 2, 10)
        }
        mapView.overlays.add(scaleBarOverlay)
    }
    private fun restoreLocationUpdatesState(savedInstanceState: Bundle?) {
        savedInstanceState?.let {
            if (it.containsKey("REQUESTING_LOCATION_UPDATES_KEY")) {
                requestingLocationUpdates = it.getBoolean("REQUESTING_LOCATION_UPDATES_KEY")
            }
        }


    }

    // Configures the callback for receiving location updates
    private fun setupLocationCallback() {
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
        if (mapView != null) {
            val marker = Marker(mapView)
            marker.position = geoPoint
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            marker.icon = resources.getDrawable(drawableId, null)
            marker.title = "Klikkpunkt"
            mapView.overlays.add(marker)
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

    override fun onDestroyView() {
        super.onDestroyView()
        mapView.onDetach()
    }


    // Saves the state of location updates
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("REQUESTING_LOCATION_UPDATES_KEY", requestingLocationUpdates)
    }
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