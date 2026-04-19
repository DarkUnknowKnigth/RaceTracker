package com.racetracker.ui

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.paint
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.racetracker.R
import com.racetracker.services.LocationTrackingService
import kotlinx.coroutines.delay

@Composable
fun DashboardScreen(userId: Int, db: com.racetracker.data.AppDatabase, onLogout: () -> Unit) {
    val context = LocalContext.current
    var isTracking by remember { mutableStateOf(false) }
    var currentSpeed by remember { mutableStateOf(0f) }
    var secondsElapsed by remember { mutableStateOf(0) }

    LaunchedEffect(userId) {
        val activeSession = db.raceDao().getActiveSessionForUser(userId)
        if (activeSession != null) {
            isTracking = true
            val elapsedMs = System.currentTimeMillis() - activeSession.startTime
            secondsElapsed = (elapsedMs / 1000).toInt()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        if (fineGranted) {
            val intent = Intent(context, LocationTrackingService::class.java).apply { 
                action = "START_TRACKING" 
                putExtra("USER_ID", userId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            isTracking = true
        }
    }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                currentSpeed = intent.getFloatExtra("CURRENT_SPEED", 0f)
            }
        }
        val filter = IntentFilter("SPEED_UPDATE")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }
    
    LaunchedEffect(isTracking) {
        if (isTracking) {
            while (true) {
                delay(1000)
                secondsElapsed++
            }
        } else {
            secondsElapsed = 0
            currentSpeed = 0f
        }
    }

    val heatmapColor = when {
        currentSpeed < 20f -> Color(0xFF03A9F4) // Blue
        currentSpeed < 60f -> Color(0xFF4CAF50) // Green
        currentSpeed < 100f -> Color(0xFFFFEB3B) // Yellow
        currentSpeed < 120f -> Color(0xFFFF9800) // Orange
        else -> Color(0xFFF44336) // Red
    }

    val formattedTime = String.format("%02d:%02d:%02d", secondsElapsed / 3600, (secondsElapsed % 3600) / 60, secondsElapsed % 60)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .paint(painterResource(id = R.drawable.m3_bg), contentScale = ContentScale.Crop),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .shadow(10.dp, CircleShape)
                    .background(heatmapColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(formattedTime, fontSize = 24.sp, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(48.dp))

        Text("${String.format("%.1f", currentSpeed)}", fontSize = 100.sp, fontWeight = FontWeight.Black, color = heatmapColor)
        Text("KM/H", fontSize = 32.sp, color = Color.Gray, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(64.dp))

        ExtendedFloatingActionButton(
            onClick = {
                if (isTracking) {
                    val intent = Intent(context, LocationTrackingService::class.java).apply { action = "STOP_TRACKING" }
                    context.startService(intent)
                    isTracking = false
                    onLogout() // redirect to stats
                } else {
                    val perms = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        perms.add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    permissionLauncher.launch(perms.toTypedArray())
                }
            },
            containerColor = if (isTracking) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            icon = { Icon(if (isTracking) Icons.Filled.Stop else Icons.Filled.PlayArrow, contentDescription = null) },
            text = { Text(if (isTracking) "DETENER" else "INICIAR RUTA", fontWeight = FontWeight.Bold) }
        )
    }
}
