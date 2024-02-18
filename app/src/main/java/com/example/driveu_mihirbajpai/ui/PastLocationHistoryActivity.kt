package com.example.driveu_mihirbajpai.ui

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.driveu_mihirbajpai.R
import com.example.driveu_mihirbajpai.data.LocationData
import com.example.driveu_mihirbajpai.data.LocationDatabaseHelper
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

class PastLocationHistoryActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap

    private lateinit var locationDatabaseHelper: LocationDatabaseHelper

    private lateinit var polyPath: List<LatLng>
    private var startLatitude = 0.0
    private var startLongitude: Double = 0.0
    private var endLatitude = 0.0
    private var endLongitude = 0.0

    private lateinit var startLatLng: LatLng
    private lateinit var endLatLng: LatLng

    private lateinit var locationData: List<LocationData>

    private var polyline: Polyline? = null

    private lateinit var customMarker: MarkerOptions
    private lateinit var progressBar: ProgressBar
    private var id = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_past_location_history)

        progressBar = findViewById(R.id.progressBar)
        val customProgressColor = ContextCompat.getColor(this, R.color.driveu_green)
        progressBar.indeterminateTintList = ColorStateList.valueOf(customProgressColor)
        progressBar.visibility = View.VISIBLE

        val intent: Intent = intent
        id = intent.getIntExtra("id", -1)

        getAllLocationDataAsync()
    }

    private fun getAllLocationDataAsync() {
        Thread {
            runOnUiThread {
                locationDatabaseHelper = LocationDatabaseHelper(this)

                locationData = getAllLocationData()

                lateinit var currentLocation: LocationData

                for (location: LocationData in locationData) {
                    if (location.id == id) {
                        currentLocation = location
                    }
                }

                polyPath = currentLocation.polyPathPoints
                startLatitude = currentLocation.startLatitude
                startLongitude = currentLocation.startLongitude
                endLatitude = currentLocation.endLatitude
                endLongitude = currentLocation.endLongitude

                startLatLng = LatLng(startLatitude, startLongitude)
                endLatLng = LatLng(endLatitude, endLongitude)

                val mapFragment =
                    supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
                mapFragment.getMapAsync(this)
            }
        }.start()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        progressBar.visibility = View.GONE
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
            .position(startLatLng)
            .icon(BitmapDescriptorFactory.fromBitmap(scaledBitmap)).title("Start point")
        mMap.addMarker(customMarker)

        customMarker = MarkerOptions()
            .position(endLatLng)
            .icon(BitmapDescriptorFactory.fromBitmap(scaledBitmap)).title("End point")
        mMap.addMarker(customMarker)

        if (polyPath.size >= 2) {
            val polylineOptions = PolylineOptions()
                .addAll(polyPath)
                .color(
                    ContextCompat.getColor(
                        this@PastLocationHistoryActivity,
                        R.color.driveu_green
                    )
                )

            polyline = mMap.addPolyline(polylineOptions)
        }

        moveAndZoomCameraToFitPoints(startLatLng, endLatLng)
    }

    private fun moveAndZoomCameraToFitPoints(point1: LatLng, point2: LatLng) {
        val builder = LatLngBounds.Builder()
        builder.include(point1)
        builder.include(point2)
        val bounds = builder.build()

        val padding = 500
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
    }

    private fun getAllLocationData(): List<LocationData> {
        val locationDataList = mutableListOf<LocationData>()

        val cursor = locationDatabaseHelper.getAllLocationData()
        cursor?.use { cursor ->
            if (cursor.moveToFirst()) {
                do {
                    val locationData = LocationData(
                        cursor.getInt(cursor.getColumnIndex(LocationDatabaseHelper.COLUMN_ID)),
                        cursor.getDouble(cursor.getColumnIndex(LocationDatabaseHelper.COLUMN_START_LATITUDE)),
                        cursor.getDouble(cursor.getColumnIndex(LocationDatabaseHelper.COLUMN_START_LONGITUDE)),
                        cursor.getDouble(cursor.getColumnIndex(LocationDatabaseHelper.COLUMN_END_LATITUDE)),
                        cursor.getDouble(cursor.getColumnIndex(LocationDatabaseHelper.COLUMN_END_LONGITUDE)),
                        cursor.getLong(cursor.getColumnIndex(LocationDatabaseHelper.COLUMN_TIMESTAMP)),
                        stringToPolyPathPoints(
                            cursor.getString(
                                cursor.getColumnIndex(
                                    LocationDatabaseHelper.COLUMN_POLYLINE_POINTS
                                )
                            )
                        ),
                        byteArrayToBitmap(
                            cursor.getBlob(
                                cursor.getColumnIndex(
                                    LocationDatabaseHelper.COLUMN_SNAPSHOT
                                )
                            )
                        )
                    )
                    locationDataList.add(locationData)
                } while (cursor.moveToNext())
            }
        }
        return locationDataList
    }

    private fun stringToPolyPathPoints(polyPath: String): List<LatLng> {
        val points = polyPath.split(";")
        val polyPathPoints = mutableListOf<LatLng>()
        for (point in points) {
            val latLng = point.split(",")
            if (latLng.size == 2) {
                val lat = latLng[0].toDouble()
                val lng = latLng[1].toDouble()
                polyPathPoints.add(LatLng(lat, lng))
            }
        }
        return polyPathPoints
    }

    private fun byteArrayToBitmap(byteArray: ByteArray?): Bitmap? {
        if (byteArray == null) return null
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }
}
