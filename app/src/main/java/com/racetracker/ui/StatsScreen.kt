package com.racetracker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.racetracker.R
import com.racetracker.data.AppDatabase
import com.racetracker.data.SessionEntity
import com.racetracker.data.TrackPointEntity
import kotlinx.coroutines.launch

@Composable
fun StatsScreen(userId: Int, db: AppDatabase, sessionId: Int? = null, onNavigateToMap: ((Int) -> Unit)? = null) {
    var lastSession by remember { mutableStateOf<SessionEntity?>(null) }
    var overallTopSpeed by remember { mutableStateOf(0f) }
    var trackPoints by remember { mutableStateOf<List<TrackPointEntity>?>(null) }
    var user by remember { mutableStateOf<com.racetracker.data.UserEntity?>(null) }

    LaunchedEffect(userId, sessionId) {
        if (sessionId != null) {
            db.raceDao().getSessionById(sessionId).collect { session ->
                lastSession = session
                session?.let {
                    db.raceDao().getTrackPointsForSession(it.id).collect { points ->
                        trackPoints = points
                    }
                }
            }
        } else {
            db.raceDao().getLastSessionForUser(userId).collect { last ->
                lastSession = last
                last?.let {
                    db.raceDao().getTrackPointsForSession(it.id).collect { points ->
                        trackPoints = points
                    }
                }
            }
        }
    }
    LaunchedEffect(userId) {
        launch {
            db.raceDao().getUserById(userId).collect {
                user = it
            }
        }
        launch {
            db.raceDao().getOverallTopSpeedForUser(userId).collect { speed ->
                overallTopSpeed = speed ?: 0f
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {


        Column(modifier = Modifier.padding(16.dp)) {
            if (user != null) {
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(64.dp).clip(androidx.compose.foundation.shape.CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                        if (user!!.photoUri != null) {
                            coil.compose.AsyncImage(
                                model = user!!.photoUri,
                                contentDescription = "Foto",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(Icons.Filled.Person, contentDescription = "Piloto", modifier = Modifier.size(32.dp), tint = Color.Gray)
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(user!!.username, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Text(user!!.vehicleModel, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("🏁 RECORD MAGISTRAL", color = MaterialTheme.colorScheme.primary, fontSize = 18.sp, fontWeight = FontWeight.Black)
                val context = LocalContext.current
                IconButton(onClick = {
                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(
                            android.content.Intent.EXTRA_TEXT, 
                            "🏎️ Alcancé ${String.format("%.1f", overallTopSpeed)} km/h en RaceTracker midiendo mi máxima telemetría!"
                        )
                    }
                    context.startActivity(android.content.Intent.createChooser(shareIntent, "Compartir Record"))
                }) {
                    Icon(androidx.compose.material.icons.Icons.Filled.Share, contentDescription = "Compartir", tint = Color.White)
                }
            }

            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Card(
                    modifier = Modifier.weight(1f).height(100.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("TOP ABSOLUTO", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("${String.format("%.1f", overallTopSpeed)}", color = MaterialTheme.colorScheme.primary, fontSize = 32.sp, fontWeight = FontWeight.Black)
                        Text("KM/H", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (lastSession != null) {
                    Card(
                        modifier = Modifier.weight(1f).height(100.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("TOP VIAJE", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("${String.format("%.1f", lastSession!!.maxSpeed)}", color = MaterialTheme.colorScheme.primary, fontSize = 32.sp, fontWeight = FontWeight.Black)
                            Text("KM/H", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(if (sessionId != null) "VIAJE SELECCIONADO (Fuerzas G)" else "ÚLTIMO VIAJE (Fuerzas G)", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                if (lastSession != null && onNavigateToMap != null) {
                    IconButton(onClick = { onNavigateToMap(lastSession!!.id) }) {
                        Icon(androidx.compose.material.icons.Icons.Filled.Map, contentDescription = "Ver Mapa", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (lastSession != null) {
                val avgSpeed = if (trackPoints != null && trackPoints!!.isNotEmpty()) trackPoints!!.map { it.speedKmh }.average() else 0.0
                val maxPosG = trackPoints?.maxOfOrNull { it.acceleration }?.let { it / 9.81f }?.coerceAtLeast(0f) ?: 0f
                val maxNegG = trackPoints?.minOfOrNull { it.acceleration }?.let { Math.abs(it / 9.81f) } ?: 0f
                val timeOver100 = trackPoints?.count { it.speedKmh >= 100f } ?: 0

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatBox("VEL. PROM", "${String.format("%.1f", avgSpeed)} km/h")
                    val distanceKm = lastSession!!.distanceMeters / 1000f
                    StatBox("DISTANCIA", "${String.format("%.2f", distanceKm)} km")
                    val duration = if (lastSession!!.endTime != null) ((lastSession!!.endTime!! - lastSession!!.startTime) / 60000).toString() + " min" else "0 min"
                    StatBox("TIEMPO", duration)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatBox("MAX +G (Acel)", "${String.format("%.2f", maxPosG)}")
                    StatBox("MAX -G (Freno)", "${String.format("%.2f", maxNegG)}")
                    StatBox("TIEMPO >100", "${timeOver100} s")
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("ESPECTRO DE ACELERACIÓN", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(Color(0xFF45E9CE), RoundedCornerShape(2.dp)))
                        Text(" +G", color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(start = 4.dp, end = 12.dp))
                        Box(modifier = Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)))
                        Text(" -G", color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(start = 4.dp))
                    }
                }
                Text("Cálculo continuo de Fuerzas G capturadas por GPS", color = Color.DarkGray, fontSize = 12.sp)
                
                // BAR CHART
                if (trackPoints != null && trackPoints!!.isNotEmpty()) {
                    val primaryColor = MaterialTheme.colorScheme.primary
                    
                    // Simple Moving Average for Smoothness (Window of 5)
                    val smoothedAccel = trackPoints!!.windowed(size = 5, step = 1, partialWindows = true) { window ->
                        window.map { it.acceleration }.average().toFloat()
                    }
                    
                    Canvas(modifier = Modifier.fillMaxWidth().height(120.dp).padding(top = 16.dp)) {
                        val barCount = smoothedAccel.size.coerceAtLeast(1)
                        val totalAvailableWidth = size.width
                        val spacing = 2f
                        val barWidth = ((totalAvailableWidth - (spacing * (barCount - 1))) / barCount).coerceAtLeast(1f)
                        val baseline = size.height / 2f
                        
                        // Draw Neutral baseline
                        drawLine(
                            color = Color.DarkGray,
                            start = Offset(0f, baseline),
                            end = Offset(size.width, baseline),
                            strokeWidth = 1f
                        )

                        smoothedAccel.forEachIndexed { index, rawAccel ->
                            val scaledAccel = rawAccel * 15f // amplify for visual
                            
                            val isAccel = rawAccel > 0
                            val color = if (isAccel) Color(0xFF45E9CE) else primaryColor
                            
                            val barHeight = Math.abs(scaledAccel).coerceAtMost(baseline - 4f)
                            val yOffset = if (isAccel) baseline - barHeight else baseline
                            
                            drawRoundRect(
                                color = color,
                                topLeft = Offset(x = index * (barWidth + spacing), y = yOffset),
                                size = Size(width = barWidth, height = barHeight.coerceAtLeast(2f)),
                                cornerRadius = CornerRadius(4f, 4f)
                            )
                        }
                    }
                }
            } else {
                Text("No hay viajes previos registrados.", color = Color.Gray)
            }
        }
    }
}

@Composable
fun StatBox(label: String, value: String) {
    Card(
        modifier = Modifier.width(110.dp).height(60.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.Center) {
            Text(label, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
        }
    }
}
