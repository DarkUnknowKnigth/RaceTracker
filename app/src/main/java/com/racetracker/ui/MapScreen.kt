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
fun MapScreen(sessionId: Int, db: AppDatabase, onBack: () -> Unit) {
    val context = LocalContext.current
    var trackPoints by remember { mutableStateOf<List<TrackPointEntity>?>(null) }
    
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
                factory = { context ->
                    MapView(context).apply {
                        getMapAsync { mapboxMap ->
                            mapboxMap.setStyle("https://basemaps.cartocdn.com/gl/dark-matter-gl-style/style.json") { style ->
                                
                                val features = mutableListOf<Feature>()
                                // Break line into segments for heatmap
                                for (i in 0 until trackPoints!!.size - 1) {
                                    val startP = trackPoints!![i]
                                    val endP = trackPoints!![i+1]
                                    
                                    val line = LineString.fromLngLats(listOf(
                                        Point.fromLngLat(startP.longitude, startP.latitude),
                                        Point.fromLngLat(endP.longitude, endP.latitude)
                                    ))
                                    val feature = Feature.fromGeometry(line)
                                    feature.addStringProperty("color", getHeatmapHex(startP.speedKmh))
                                    features.add(feature)
                                }
                                
                                val featureCollection = FeatureCollection.fromFeatures(features)
                                style.addSource(GeoJsonSource("route-source", featureCollection))
                                
                                val lineLayer = LineLayer("route-layer", "route-source").withProperties(
                                    lineColor(get("color")),
                                    lineWidth(5f),
                                    lineCap("round"),
                                    lineJoin("round")
                                )
                                style.addLayer(lineLayer)

                                // Move camera
                                if (trackPoints!!.isNotEmpty()) {
                                    val start = LatLng(trackPoints!!.first().latitude, trackPoints!!.first().longitude)
                                    mapboxMap.cameraPosition = org.maplibre.android.camera.CameraPosition.Builder()
                                        .target(start)
                                        .zoom(15.0)
                                        .build()
                                }
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Overlaid Tags for Start and End Time
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val startTimeStr = sdf.format(Date(trackPoints!!.first().timestamp))
            val endTimeStr = sdf.format(Date(trackPoints!!.last().timestamp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(32.dp),
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
    }
}
