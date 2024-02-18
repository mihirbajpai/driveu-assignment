package com.example.driveu_mihirbajpai.ui

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.driveu_mihirbajpai.R
import com.example.driveu_mihirbajpai.data.LocationData
import com.example.driveu_mihirbajpai.util.Util

class LocationRecordAdapter(
    private val locationRecords: List<LocationData>,
    private val context: Context
) :
    RecyclerView.Adapter<LocationRecordAdapter.LocationRecordViewHolder>() {

    private val util: Util = Util()

    inner class LocationRecordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewStartLocation: TextView = itemView.findViewById(R.id.textView_start_location)
        val textViewEndLocation: TextView = itemView.findViewById(R.id.textView_end_location)
        val textViewDistance: TextView = itemView.findViewById(R.id.textView_distance)
        val textViewTime: TextView = itemView.findViewById(R.id.textView_time)
        val imageViewSnapshot: ImageView = itemView.findViewById(R.id.imageView_snapshot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationRecordViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_location_record, parent, false)
        return LocationRecordViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: LocationRecordViewHolder, position: Int) {
        val currentRecord = locationRecords[position]

        holder.imageViewSnapshot.setImageBitmap(currentRecord.snapshot)

        val distance: Double = util.calculateDistance(
            currentRecord.startLatitude,
            currentRecord.startLongitude,
            currentRecord.endLatitude,
            currentRecord.endLongitude
        )

        if (distance < 1.0) {
            holder.textViewDistance.text = "${String.format("%.2f", distance * 1000.0)} meters"
        } else {
            holder.textViewDistance.text = "${String.format("%.2f", distance)} Km."
        }

        holder.textViewTime.text = util.formatTimestamp(currentRecord.timestamp)
        holder.textViewStartLocation.text = util.getAddressFromLatLng(
            context,
            currentRecord.startLatitude,
            currentRecord.startLongitude
        )
        holder.textViewEndLocation.text = util.getAddressFromLatLng(
            context,
            currentRecord.endLatitude,
            currentRecord.endLongitude
        )

        holder.itemView.setOnClickListener {
            val intent = Intent(context, PastLocationHistoryActivity::class.java)
            intent.putExtra("id", currentRecord.id)
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = locationRecords.size
}
