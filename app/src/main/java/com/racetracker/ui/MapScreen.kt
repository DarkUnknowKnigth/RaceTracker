package com.racetracker.ui

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import com.racetracker.data.AppDatabase
import com.racetracker.data.TrackPointEntity
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory.*
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import org.maplibre.android.style.expressions.Expression.*
import java.text.SimpleDateFormat
import java.util.*

fun getHeatmapHex(speedKmh: Float): String {
    return when {
        speedKmh < 20f -> "#03A9F4" // Blue
        speedKmh < 60f -> "#4CAF50" // Green
        speedKmh < 100f -> "#FFEB3B" // Yellow
        speedKmh < 120f -> "#FF9800" // Orange
        else -> "#F44336" // Red
    }
}

@Composable
fun MapScreen(sessionId: Int, db: AppDatabase, onBack: () -> Unit, onNavigateToStats: ((Int) -> Unit)? = null) {
    val context = LocalContext.current
    var trackPoints by remember { mutableStateOf<List<TrackPointEntity>?>(null) }
    var selectedSpeed by remember { mutableStateOf<Float?>(null) }
    var selectedGForce by remember { mutableStateOf<Float?>(null) }
    
    LaunchedEffect(Unit) {
        MapLibre.getInstance(context)
    }

    LaunchedEffect(sessionId) {
        db.raceDao().getTrackPointsForSession(sessionId).collect {
            trackPoints = it
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (trackPoints == null) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (trackPoints!!.isEmpty()) {
            Text("No hay datos de GPS para este viaje.", color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.align(Alignment.Center))
        } else {
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        getMapAsync { mapboxMap ->
                            mapboxMap.setStyle("https://basemaps.cartocdn.com/gl/dark-matter-gl-style/style.json") { style ->
                                
                                val features = mutableListOf<Feature>()
                                for (i in 0 until trackPoints!!.size - 1) {
                                    val startP = trackPoints!![i]
                                    val endP = trackPoints!![i+1]
                                    
                                    val line = LineString.fromLngLats(listOf(
                                        Point.fromLngLat(startP.longitude, startP.latitude),
                                        Point.fromLngLat(endP.longitude, endP.latitude)
                                    ))
                                    val feature = Feature.fromGeometry(line)
                                    feature.addStringProperty("color", getHeatmapHex(startP.speedKmh))
                                    feature.addNumberProperty("speed", startP.speedKmh)
                                    val gForce = startP.acceleration / 9.81f
                                    feature.addNumberProperty("gForce", gForce)
                                    features.add(feature)
                                }
                                
                                val featureCollection = FeatureCollection.fromFeatures(features)
                                style.addSource(GeoJsonSource("route-source", featureCollection))
                                
                                val lineLayer = LineLayer("route-layer", "route-source").withProperties(
                                    lineColor(get("color")),
                                    lineWidth(10f),
                                    lineCap("round"),
                                    lineJoin("round")
                                )
                                style.addLayer(lineLayer)

                                if (trackPoints!!.isNotEmpty()) {
                                    val start = LatLng(trackPoints!!.first().latitude, trackPoints!!.first().longitude)
                                    mapboxMap.cameraPosition = org.maplibre.android.camera.CameraPosition.Builder()
                                        .target(start)
                                        .zoom(15.0)
                                        .build()
                                }
                                
                                mapboxMap.addOnMapClickListener { point ->
                                    val screenPoint = mapboxMap.projection.toScreenLocation(point)
                                    val featuresClicked = mapboxMap.queryRenderedFeatures(screenPoint, "route-layer")
                                    if (featuresClicked.isNotEmpty()) {
                                        selectedSpeed = featuresClicked[0].getNumberProperty("speed")?.toFloat()
                                        selectedGForce = featuresClicked[0].getNumberProperty("gForce")?.toFloat()
                                        true
                                    } else {
                                        selectedSpeed = null
                                        selectedGForce = null
                                        false
                                    }
                                }
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val startTimeStr = sdf.format(Date(trackPoints!!.first().timestamp))
            val endTimeStr = sdf.format(Date(trackPoints!!.last().timestamp))

            Column(modifier = Modifier.align(Alignment.TopCenter).padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    IconButton(onClick = onBack, modifier = Modifier.background(Color.Black.copy(alpha=0.5f), RoundedCornerShape(8.dp))) {
                        Icon(androidx.compose.material.icons.Icons.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                    if (onNavigateToStats != null) {
                        IconButton(onClick = { onNavigateToStats(sessionId) }, modifier = Modifier.background(Color.Black.copy(alpha=0.5f), RoundedCornerShape(8.dp))) {
                            Icon(androidx.compose.material.icons.Icons.Filled.Star, contentDescription = "Ver Stats", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Text("🟢 Salida: $startTimeStr", modifier = Modifier.padding(8.dp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Text("🏁 Llegada: $endTimeStr", modifier = Modifier.padding(8.dp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                }
            }

            if (selectedSpeed != null) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 90.dp, start = 16.dp, end = 16.dp)
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("VELOCIDAD", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("${String.format("%.1f", selectedSpeed)} km/h", color = MaterialTheme.colorScheme.primary, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("FUERZA G", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            val gText = selectedGForce?.let { String.format("%.2f G", it) } ?: "--"
                            Text(gText, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}
