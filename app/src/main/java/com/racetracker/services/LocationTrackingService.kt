package com.racetracker.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.racetracker.data.AppDatabase
import com.racetracker.data.SessionEntity
import com.racetracker.data.TrackPointEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LocationTrackingService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var sessionId: Int = -1
    private var lastSpeedMs: Float = 0f
    private var lastTimestamp: Long = 0L
    private var peakBrakingGForce: Float = 0f
    private var lastLocation: Location? = null
    private var totalDistanceMeters: Float = 0f

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                p0.lastLocation?.let { location ->
                    saveLocationToDb(location)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "START_TRACKING") {
            val userId = intent.getIntExtra("USER_ID", -1)
            if (userId != -1) {
                startTracking(userId)
            }
        } else if (intent?.action == "STOP_TRACKING") {
            stopTracking()
        }
        return START_NOT_STICKY
    }

    private fun startTracking(userId: Int) {
        // Create notification channel for Foreground Service
        val channelId = "race_tracker_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Race Tracker", NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Race Tracker Active")
            .setContentText("Recording your speed and route...")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(1, notification)
        }

        serviceScope.launch {
            val db = AppDatabase.getDatabase(applicationContext)
            val session = SessionEntity(userId = userId, startTime = System.currentTimeMillis())
            sessionId = db.raceDao().insertSession(session).toInt()
            
            requestLocationUpdates()
        }
    }

    private fun requestLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
            .setMinUpdateIntervalMillis(1000)
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (unlikely: SecurityException) {
            Log.e("LocationService", "Lost location permission", unlikely)
        }
    }

    private fun saveLocationToDb(location: Location) {
        if (sessionId == -1) return
        
        serviceScope.launch {
            val currentTimestamp = System.currentTimeMillis()
            val currentSpeedKmh = if (location.hasSpeed()) location.speed * 3.6f else 0f
            val currentSpeedMs = if (location.hasSpeed()) location.speed else 0f
            
            if (lastLocation != null) {
                totalDistanceMeters += lastLocation!!.distanceTo(location)
            }
            
            var acceleration = 0f
            if (lastTimestamp > 0) {
                val deltaT = (currentTimestamp - lastTimestamp) / 1000f
                if (deltaT > 0) {
                    acceleration = (currentSpeedMs - lastSpeedMs) / deltaT
                }
            }
            
            // Calculate G-Force
            val gForce = acceleration / 9.81f
            if (gForce < peakBrakingGForce) {
                peakBrakingGForce = gForce // Will record the most negative value
            }

            lastSpeedMs = currentSpeedMs
            lastTimestamp = currentTimestamp
            lastLocation = location
            
            val point = TrackPointEntity(
                sessionId = sessionId,
                timestamp = currentTimestamp,
                latitude = location.latitude,
                longitude = location.longitude,
                speedKmh = currentSpeedKmh,
                acceleration = acceleration // stored in m/s^2
            )
            
            AppDatabase.getDatabase(applicationContext).raceDao().insertTrackPoint(point)
            
            val intent = Intent("SPEED_UPDATE")
            intent.putExtra("CURRENT_SPEED", currentSpeedKmh)
            sendBroadcast(intent)
        }
    }

    private fun stopTracking() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        
        serviceScope.launch {
            if (sessionId != -1) {
                val db = AppDatabase.getDatabase(applicationContext)
                val maxSpeed = db.raceDao().getMaxSpeedForSession(sessionId) ?: 0f
                val endTime = System.currentTimeMillis()
                
                db.raceDao().finalizeSession(sessionId, endTime, maxSpeed, peakBrakingGForce, totalDistanceMeters)
            }
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
