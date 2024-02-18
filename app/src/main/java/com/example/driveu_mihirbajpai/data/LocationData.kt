package com.example.driveu_mihirbajpai.data

import android.graphics.Bitmap
import com.google.android.gms.maps.model.LatLng

data class LocationData(
    val id: Int,
    val startLatitude: Double,
    val startLongitude: Double,
    val endLatitude: Double,
    val endLongitude: Double,
    val timestamp: Long,
    val polyPathPoints: List<LatLng>,
    val snapshot: Bitmap?
)
