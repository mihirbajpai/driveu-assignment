package com.example.driveu_mihirbajpai.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.driveu_mihirbajpai.R
import com.example.driveu_mihirbajpai.data.LocationDataExporter
import com.example.driveu_mihirbajpai.data.LocationDatabaseHelper
import com.example.driveu_mihirbajpai.notification.NotificationHelper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var mMap: GoogleMap
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationDatabaseHelper: LocationDatabaseHelper
    private lateinit var customMarker: MarkerOptions

    private var polyPathPoints = mutableListOf<LatLng>()
    private var polyline: Polyline? = null

    private var locationPermissionGranted = false

    private var btn: Int = 0

    private lateinit var startLocation: Location

    private lateinit var progressBar: ProgressBar
    private lateinit var startStop: Button

    private val notificationHelper by lazy { NotificationHelper(this) }

    private var trackingInterval = 1

    private lateinit var locationDataExporter: LocationDataExporter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startStop = findViewById(R.id.start_stop)

        progressBar = findViewById(R.id.progressBar)
        val customProgressColor = ContextCompat.getColor(this, R.color.driveu_green)
        progressBar.indeterminateTintList = ColorStateList.valueOf(customProgressColor)

        showProgress()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationDatabaseHelper = LocationDatabaseHelper(this)

        locationDataExporter = LocationDataExporter(this)

        if (!isLocationEnabled()) {
            showLocationSettingsDialog()
        } else {
            initializeMap()
            setupClickListener()
            getLocationPermission()
        }
    }

    private fun initializeMap() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun setupClickListener() {
        startStop.setOnClickListener {
            when (btn) {
                0 -> {
                    startLocationUpdates()
                    startStop.text = "Stop"
                    btn = 1
                }

                else -> {
                    stopLocationUpdates()
                    startStop.text = "Start"
                    btn = 0
                }
            }
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    private fun showLocationSettingsDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Location Services Not Enabled")
        builder.setMessage("Please enable location services to use this app.")
        builder.setPositiveButton("Go to settings") { dialog, _ ->
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
            finish()
        }
        builder.setCancelable(false)
        builder.show()
    }

    private fun getLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this.applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionGranted = true
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        locationPermissionGranted = false
        when (requestCode) {
            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    locationPermissionGranted = true
                    updateLocationUI()
                }
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        hideProgress()
        updateLocationUI()
    }

    private fun updateLocationUI() {
        moveToCurrentLocation()
        if (locationPermissionGranted) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                mMap.isMyLocationEnabled = true
            }
        } else {
            mMap.isMyLocationEnabled = false
        }
    }

    private fun startLocationUpdates() {
        notificationHelper.showTrackingNotification()
        clearMap()
        if (locationPermissionGranted) {
            locationRequest = LocationRequest.create().apply {
                interval = (trackingInterval * 1000).toLong()
                fastestInterval = (trackingInterval * 1000).toLong()
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            }
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)

            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    startLocation = location
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))

                    val locationDrawable = ContextCompat.getDrawable(this, R.drawable.location)
                    val locationBitmap = Bitmap.createBitmap(
                        locationDrawable!!.intrinsicWidth,
                        locationDrawable.intrinsicHeight,
                        Bitmap.Config.ARGB_8888
                    )
                    val canvas = Canvas(locationBitmap)
                    locationDrawable.setBounds(0, 0, canvas.width, canvas.height)
                    locationDrawable.draw(canvas)

                    val desiredWidth = 100
                    val desiredHeight = 100

                    val scaledBitmap =
                        Bitmap.createScaledBitmap(locationBitmap, desiredWidth, desiredHeight, true)

                    customMarker = MarkerOptions()
                        .position(currentLatLng)
                        .icon(BitmapDescriptorFactory.fromBitmap(scaledBitmap))
                    mMap.addMarker(customMarker)

                }
            }
            Toast.makeText(this, "Location updates started", Toast.LENGTH_SHORT).show()
        }
    }

    private fun moveToCurrentLocation() {
        if (locationPermissionGranted) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                } else {
                    Toast.makeText(
                        this,
                        "Unable to get current location",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } else {
            Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopLocationUpdates() {
        notificationHelper.cancelTrackingNotification()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val currentLatLng = LatLng(location.latitude, location.longitude)

                val endLocation = location

                moveAndZoomCameraToFitPoints(
                    LatLng(startLocation.latitude, startLocation.longitude),
                    LatLng(endLocation.latitude, endLocation.longitude)
                )

                showProgress()

                mMap.snapshot {
                    locationDatabaseHelper.insertLocationData(
                        startLatitude = startLocation.latitude,
                        startLongitude = startLocation.longitude,
                        endLatitude = endLocation.latitude,
                        endLongitude = endLocation.longitude,
                        timestamp = System.currentTimeMillis(),
                        polyPathPoints = polyPathPoints,
                        snapshot = it
                    )
                    hideProgress()
                }

                val locationDrawable = ContextCompat.getDrawable(this, R.drawable.location)
                val locationBitmap = Bitmap.createBitmap(
                    locationDrawable!!.intrinsicWidth,
                    locationDrawable.intrinsicHeight,
                    Bitmap.Config.ARGB_8888
                )
                val canvas = Canvas(locationBitmap)
                locationDrawable.setBounds(0, 0, canvas.width, canvas.height)
                locationDrawable.draw(canvas)

                val desiredWidth = 100
                val desiredHeight = 100

                val scaledBitmap =
                    Bitmap.createScaledBitmap(locationBitmap, desiredWidth, desiredHeight, true)

                customMarker = MarkerOptions()
                    .position(currentLatLng)
                    .icon(BitmapDescriptorFactory.fromBitmap(scaledBitmap))
                mMap.addMarker(customMarker)
            }
        }
        Toast.makeText(this, "Location updates stopped", Toast.LENGTH_SHORT).show()
    }

    private val locationCallback = object : com.google.android.gms.location.LocationCallback() {
        override fun onLocationResult(locationResult: com.google.android.gms.location.LocationResult) {
            super.onLocationResult(locationResult)
            for (location in locationResult.locations) {
                val currentLatLng = LatLng(location.latitude, location.longitude)
                polyPathPoints.add(currentLatLng)

                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))

                if (polyPathPoints.size >= 2) {
                    if (polyline != null) {
                        polyline?.remove()
                    }
                    val polylineOptions = PolylineOptions()
                        .addAll(polyPathPoints)
                        .color(ContextCompat.getColor(this@MainActivity, R.color.driveu_green))

                    polyline = mMap.addPolyline(polylineOptions)
                }
            }
        }
    }

    companion object {
        private const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1
    }

    // Action Bar
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        val addItem = menu.findItem(R.id.action_history)
        addItem.setOnMenuItemClickListener {
            startActivity(Intent(this, LocationRecordActivity::class.java))

            true
        }

        val setUpdateInterval = menu.findItem(R.id.action_set_time)
        setUpdateInterval.setOnMenuItemClickListener {

            showIntervalInputDialog()

            true
        }

        val exportData = menu.findItem(R.id.action_export_data)
        exportData.setOnMenuItemClickListener {
            Toast.makeText(this, "Please wait few second.", Toast.LENGTH_SHORT).show()
            locationDataExporter
                .exportToCSV(locationDatabaseHelper.getAllLocationDataFromDatabase())

            true
        }
        return true
    }

    //Set Interval
    private fun showIntervalInputDialog() {
        val editText = EditText(this)
        editText.hint = "Enter tracking interval in seconds"

        val dialog = AlertDialog.Builder(this)
            .setTitle("Set Tracking Interval")
            .setView(editText)
            .setPositiveButton("Done") { dialog, _ ->
                val inputText = editText.text.toString()

                trackingInterval = inputText.toIntOrNull() ?: 1

                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
        dialog.show()
    }


    private fun moveAndZoomCameraToFitPoints(point1: LatLng, point2: LatLng) {
        val builder = LatLngBounds.Builder()
        builder.include(point1)
        builder.include(point2)
        val bounds = builder.build()

        val padding = 200

        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
    }

    private fun clearMap() {
        mMap.clear()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        if (polyline != null) {
            polyline?.remove()
            polyline = null
        }
        polyPathPoints.clear()
    }

    //Show and hide progress
    private fun showProgress() {
        progressBar.visibility = View.VISIBLE
        startStop.visibility = View.GONE
        invalidateOptionsMenu()
    }

    private fun hideProgress() {
        progressBar.visibility = View.GONE
        startStop.visibility = View.VISIBLE
        invalidateOptionsMenu()
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val isProgressVisible = progressBar.visibility == View.VISIBLE
        menu.findItem(R.id.action_history)?.isEnabled = !isProgressVisible
        menu.findItem(R.id.action_set_time)?.isEnabled = !isProgressVisible
        menu.findItem(R.id.action_export_data)?.isEnabled = !isProgressVisible
        return super.onPrepareOptionsMenu(menu)
    }
}
