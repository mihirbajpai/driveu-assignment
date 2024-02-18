package com.example.driveu_mihirbajpai.data

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.driveu_mihirbajpai.util.Util
import java.io.FileWriter

class LocationDataExporter(private val activity: AppCompatActivity) {

    private val util: Util = Util()
    private lateinit var locationData: List<LocationData>

    val saveFileLauncher =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val intentData = result.data?.data
                if (intentData != null) {
                    saveToChosenDirectory(intentData)
                }
            }
        }

    fun exportToCSV(locationData: List<LocationData>) {
        val fileName = "location_data.csv"
        val fileContent = buildCsvContent(locationData)
        this.locationData = locationData

        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/csv"
            putExtra(Intent.EXTRA_TITLE, fileName)
        }

        saveFileLauncher.launch(intent)
    }

    fun buildCsvContent(locationData: List<LocationData>): String {
        val csvHeader =
            "Time,Start Location,End Location,Start Latitude,Start Longitude,End Latitude,End Longitude,Distance(in Km.)\n"
        val stringBuilder = StringBuilder(csvHeader)

        for (location in locationData) {
            val startLatitude = location.startLatitude
            val startLongitude = location.startLongitude
            val endLatitude = location.endLatitude
            val endLongitude = location.endLongitude
            val startLocation = util.getAddressFromLatLng(activity, startLatitude, startLongitude)
            val endLocation = util.getAddressFromLatLng(activity, endLatitude, endLongitude)
            val time = util.formatTimestamp(location.timestamp)
            val distance =
                util.calculateDistance(startLatitude, startLongitude, endLatitude, endLongitude)

            stringBuilder.append("\"$time\",\"$startLocation\",\"$endLocation\",$startLatitude,$startLongitude,$endLatitude,$endLongitude,$distance\n")
        }

        return stringBuilder.toString()
    }

    fun saveToChosenDirectory(uri: Uri) {
        try {
            activity.contentResolver.openFileDescriptor(uri, "w")?.use { parcelFileDescriptor ->
                FileWriter(parcelFileDescriptor.fileDescriptor).use { writer ->
                    writer.write(buildCsvContent(locationData))
                }
            }
            Log.d("gggggg", "saved")
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d("gggggg", e.message.toString())
        }
    }
}
