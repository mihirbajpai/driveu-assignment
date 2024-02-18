package com.example.driveu_mihirbajpai.util

import android.content.Context
import android.location.Address
import android.location.Geocoder
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class Util {
    fun calculateDistance(
        startLatitude: Double,
        startLongitude: Double,
        endLatitude: Double,
        endLongitude: Double
    ): Double {
        val earthRadius = 6371

        val dLat = Math.toRadians(endLatitude - startLatitude)
        val dLon = Math.toRadians(endLongitude - startLongitude)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(startLatitude)) * cos(Math.toRadians(endLatitude)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadius * c
    }

    fun formatTimestamp(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
        val date = Date(timestamp)
        return dateFormat.format(date)
    }


    fun getAddressFromLatLng(
        context: Context,
        latitude: Double,
        longitude: Double
    ): String {
        val geocoder = Geocoder(context, Locale.getDefault())
        var addressText = ""
        try {
            val addresses: List<Address>? = geocoder.getFromLocation(latitude, longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val address: Address = addresses[0]
                val stringBuilder = StringBuilder()
                for (i in 0..address.maxAddressLineIndex) {
                    stringBuilder.append(address.getAddressLine(i)).append("\n")
                }
                addressText = stringBuilder.toString()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return addressText
    }
}