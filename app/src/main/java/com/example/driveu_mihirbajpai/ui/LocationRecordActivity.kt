package com.example.driveu_mihirbajpai.ui

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.driveu_mihirbajpai.R
import com.example.driveu_mihirbajpai.data.LocationData
import com.example.driveu_mihirbajpai.data.LocationDatabaseHelper

class LocationRecordActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: LocationRecordAdapter
    private lateinit var locationDatabaseHelper: LocationDatabaseHelper
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_record)

        recyclerView = findViewById(R.id.recyclerView_location_records)
        recyclerView.layoutManager = LinearLayoutManager(this)

        progressBar = findViewById(R.id.progressBar)
        progressBar.visibility = View.VISIBLE

        val customProgressColor = ContextCompat.getColor(this, R.color.driveu_green)
        progressBar.indeterminateTintList = ColorStateList.valueOf(customProgressColor)


        locationDatabaseHelper = LocationDatabaseHelper(this)
        getAllLocationDataAsync()
    }

    private fun getAllLocationDataAsync() {
        Thread {
            val locationRecords = LocationDatabaseHelper(this).getAllLocationDataFromDatabase()
            runOnUiThread {
                updateRecyclerView(locationRecords)
                progressBar.visibility = View.GONE
            }
        }.start()
    }

    private fun updateRecyclerView(locationRecords: List<LocationData>) {
        adapter = LocationRecordAdapter(locationRecords, this@LocationRecordActivity)
        recyclerView.adapter = adapter
    }
}
