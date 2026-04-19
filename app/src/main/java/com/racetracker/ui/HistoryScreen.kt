package com.racetracker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.racetracker.data.AppDatabase
import com.racetracker.data.SessionEntity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(userId: Int, db: AppDatabase, onSessionSelected: (Int) -> Unit) {
    var sessions by remember { mutableStateOf<List<SessionEntity>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(userId) {
        db.raceDao().getSessionsForUser(userId).collect {
            sessions = it
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("HISTORIAL DE VIAJES", color = MaterialTheme.colorScheme.primary, fontSize = 28.sp, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(24.dp))
        
        if (sessions.isEmpty()) {
            Text("Aún no hay rutas guardadas. ¡Inicia una carrera!", color = MaterialTheme.colorScheme.onBackground)
        } else {
            LazyColumn {
                items(sessions) { session ->
                    SessionCard(
                        session = session,
                        db = db,
                        onClick = { onSessionSelected(session.id) },
                        onDelete = {
                            coroutineScope.launch {
                                db.raceDao().deleteTrackPointsForSession(session.id)
                                db.raceDao().deleteSession(session.id)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SessionCard(session: SessionEntity, db: AppDatabase, onClick: () -> Unit, onDelete: () -> Unit) {
    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    val dateStr = sdf.format(Date(session.startTime))
    var vehicleModel by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(session.vehicleId) {
        session.vehicleId?.let { vId ->
            db.raceDao().getVehicleById(vId).collect {
                vehicleModel = it?.model
            }
        }
    }

    val durationText = if (session.endTime != null) {
        val duration = (session.endTime - session.startTime) / 60000
        "$duration min"
    } else {
        "En progreso..."
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(dateStr, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Black, fontSize = 18.sp)
                if (vehicleModel != null) {
                    Text(vehicleModel!!, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("TOP: ", color = MaterialTheme.colorScheme.outline, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("${String.format("%.1f", session.maxSpeed)} KM/H", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("DUR: ", color = MaterialTheme.colorScheme.outline, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(durationText, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(onClick = onDelete) {
                Text("🗑️", fontSize = 24.sp)
            }
        }
    }
}
