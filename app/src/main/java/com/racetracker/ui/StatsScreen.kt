package com.racetracker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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

@Composable
fun StatsScreen(userId: Int, db: AppDatabase) {
    var lastSession by remember { mutableStateOf<SessionEntity?>(null) }
    var overallTopSpeed by remember { mutableStateOf(0f) }
    var trackPoints by remember { mutableStateOf<List<TrackPointEntity>?>(null) }

    LaunchedEffect(userId) {
        db.raceDao().getLastSessionForUser(userId).collect { last ->
            lastSession = last
            last?.let {
                db.raceDao().getTrackPointsForSession(it.id).collect { points ->
                    trackPoints = points
                }
            }
        }
    }
    LaunchedEffect(userId) {
        db.raceDao().getOverallTopSpeedForUser(userId).collect { speed ->
            overallTopSpeed = speed ?: 0f
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        Image(
            painter = painterResource(id = R.drawable.m3_banner),
            contentDescription = "Racing Banner",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
        )

        Column(modifier = Modifier.padding(16.dp)) {
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

            Card(
                modifier = Modifier.fillMaxWidth().height(100.dp).padding(top = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("TOP SPEED ABSOLUTO", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("${String.format("%.1f", overallTopSpeed)} KM/H", color = MaterialTheme.colorScheme.primary, fontSize = 42.sp, fontWeight = FontWeight.Black)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("ÚLTIMO VIAJE (Fuerzas G)", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            if (lastSession != null) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatBox("TOP VIAJE", "${String.format("%.1f", lastSession!!.maxSpeed)} km/h")
                    val distanceKm = lastSession!!.distanceMeters / 1000f
                    StatBox("DISTANCIA", "${String.format("%.2f", distanceKm)} km")
                    val duration = if (lastSession!!.endTime != null) ((lastSession!!.endTime!! - lastSession!!.startTime) / 60000).toString() + " min" else "0 min"
                    StatBox("TIEMPO", duration)
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
