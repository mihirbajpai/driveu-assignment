package com.example.driveu_mihirbajpai.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.android.gms.maps.model.LatLng
import java.io.ByteArrayOutputStream

class LocationDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val TAG = "LocationDatabaseHelper"
        const val DATABASE_VERSION = 1
        const val DATABASE_NAME = "LocationTracker.db"
        const val TABLE_NAME = "LocationData"
        const val COLUMN_ID = "id"
        const val COLUMN_START_LATITUDE = "start_latitude"
        const val COLUMN_START_LONGITUDE = "start_longitude"
        const val COLUMN_END_LATITUDE = "end_latitude"
        const val COLUMN_END_LONGITUDE = "end_longitude"
        const val COLUMN_TIMESTAMP = "timestamp"
        const val COLUMN_POLYLINE_POINTS = "polyline_points"
        const val COLUMN_SNAPSHOT = "snapshot"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = "CREATE TABLE $TABLE_NAME (" +
                "$COLUMN_ID INTEGER PRIMARY KEY," +
                "$COLUMN_START_LATITUDE REAL," +
                "$COLUMN_START_LONGITUDE REAL," +
                "$COLUMN_END_LATITUDE REAL," +
                "$COLUMN_END_LONGITUDE REAL," +
                "$COLUMN_TIMESTAMP INTEGER," +
                "$COLUMN_POLYLINE_POINTS TEXT," +
                "$COLUMN_SNAPSHOT BLOB" +
                ")"
        db.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun insertLocationData(
        startLatitude: Double,
        startLongitude: Double,
        endLatitude: Double,
        endLongitude: Double,
        timestamp: Long,
        polyPathPoints: List<LatLng>,
        snapshot: Bitmap?
    ) {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put(COLUMN_START_LATITUDE, startLatitude)
            put(COLUMN_START_LONGITUDE, startLongitude)
            put(COLUMN_END_LATITUDE, endLatitude)
            put(COLUMN_END_LONGITUDE, endLongitude)
            put(COLUMN_TIMESTAMP, timestamp)
            put(COLUMN_POLYLINE_POINTS, polyPathPointsToString(polyPathPoints))
            put(COLUMN_SNAPSHOT, bitmapToByteArray(snapshot))
        }
        db.insert(TABLE_NAME, null, contentValues)
        db.close()
    }

    fun getAllLocationData(): Cursor? {
        val db = this.readableDatabase
        return db.rawQuery("SELECT * FROM $TABLE_NAME", null)
    }

    private fun polyPathPointsToString(polyPathPoints: List<LatLng>): String {
        val builder = StringBuilder()
        for (point in polyPathPoints) {
            builder.append("${point.latitude},${point.longitude};")
        }
        return builder.toString()
    }

    private fun bitmapToByteArray(bitmap: Bitmap?): ByteArray? {
        if (bitmap == null) return null
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }

    fun getAllLocationDataFromDatabase(): List<LocationData> {
        val locationDataList = mutableListOf<LocationData>()
        val cursor = getAllLocationData()
        cursor?.use { cursor ->
            while (cursor.moveToNext()) {
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
                    byteArrayToBitmap(cursor.getBlob(cursor.getColumnIndex(LocationDatabaseHelper.COLUMN_SNAPSHOT)))
                )
                locationDataList.add(locationData)
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
        return byteArray?.let {
            BitmapFactory.decodeByteArray(it, 0, it.size)
        }
    }

}
