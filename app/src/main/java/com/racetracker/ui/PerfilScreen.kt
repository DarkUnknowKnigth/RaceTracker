package com.racetracker.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.racetracker.data.AppDatabase
import com.racetracker.data.UserEntity
import com.racetracker.data.VehicleEntity
import kotlinx.coroutines.launch

@Composable
fun PerfilScreen(userId: Int, db: AppDatabase, onLogout: () -> Unit) {
    var user by remember { mutableStateOf<UserEntity?>(null) }
    var vehicles by remember { mutableStateOf<List<VehicleEntity>>(emptyList()) }
    var activeVehicleId by remember { mutableStateOf(-1) }
    
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("race_tracker_prefs", Context.MODE_PRIVATE)
    val coroutineScope = rememberCoroutineScope()

    var showAddVehicleDialog by remember { mutableStateOf(false) }
    var showEditVehicleDialog by remember { mutableStateOf<VehicleEntity?>(null) }

    LaunchedEffect(userId) {
        db.raceDao().getUserById(userId).collect {
            user = it
        }
    }

    LaunchedEffect(userId) {
        db.raceDao().getVehiclesForUser(userId).collect {
            vehicles = it
            activeVehicleId = prefs.getInt("ACTIVE_VEHICLE_ID_$userId", it.firstOrNull()?.id ?: -1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("PERFIL DEL PILOTO", color = MaterialTheme.colorScheme.primary, fontSize = 24.sp, fontWeight = FontWeight.Black)
        
        user?.let { u ->
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(60.dp).clip(CircleShape).background(Color.Gray),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Person, contentDescription = null, tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(u.username, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("MI GARAJE", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = { showAddVehicleDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Añadir Auto", tint = MaterialTheme.colorScheme.primary)
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(vehicles) { vehicle ->
                val isActive = vehicle.id == activeVehicleId
                VehicleItem(
                    vehicle = vehicle,
                    isActive = isActive,
                    onSelect = {
                        activeVehicleId = vehicle.id
                        prefs.edit().putInt("ACTIVE_VEHICLE_ID_$userId", vehicle.id).apply()
                    },
                    onEdit = { showEditVehicleDialog = vehicle }
                )
            }
        }

        Button(
            onClick = onLogout,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("CERRAR SESIÓN", fontWeight = FontWeight.Bold)
        }
    }

    if (showAddVehicleDialog) {
        VehicleDialog(
            title = "Añadir Vehículo",
            onDismiss = { showAddVehicleDialog = false },
            onConfirm = { model, photoUri ->
                coroutineScope.launch {
                    val newId = db.raceDao().insertVehicle(VehicleEntity(userId = userId, model = model, photoUri = photoUri))
                    if (activeVehicleId == -1) {
                        activeVehicleId = newId.toInt()
                        prefs.edit().putInt("ACTIVE_VEHICLE_ID_$userId", activeVehicleId).apply()
                    }
                }
                showAddVehicleDialog = false
            }
        )
    }

    showEditVehicleDialog?.let { vehicle ->
        VehicleDialog(
            title = "Editar Vehículo",
            initialModel = vehicle.model,
            initialPhotoUri = vehicle.photoUri,
            onDismiss = { showEditVehicleDialog = null },
            onConfirm = { model, photoUri ->
                coroutineScope.launch {
                    db.raceDao().updateVehicle(vehicle.copy(model = model, photoUri = photoUri))
                }
                showEditVehicleDialog = null
            },
            onDelete = {
                coroutineScope.launch {
                    db.raceDao().deleteVehicle(vehicle)
                    if (activeVehicleId == vehicle.id) {
                        activeVehicleId = -1
                        prefs.edit().remove("ACTIVE_VEHICLE_ID_$userId").apply()
                    }
                }
                showEditVehicleDialog = null
            }
        )
    }
}

@Composable
fun VehicleItem(vehicle: VehicleEntity, isActive: Boolean, onSelect: () -> Unit, onEdit: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .border(
                width = if (isActive) 2.dp else 0.dp,
                color = if (isActive) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(50.dp).clip(RoundedCornerShape(8.dp)).background(Color.DarkGray),
                contentAlignment = Alignment.Center
            ) {
                if (vehicle.photoUri != null) {
                    AsyncImage(model = vehicle.photoUri, contentDescription = null, contentScale = ContentScale.Crop)
                } else {
                    Icon(Icons.Filled.DirectionsCar, contentDescription = null, tint = Color.Gray)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(vehicle.model, fontWeight = FontWeight.Bold, color = Color.White)
                if (isActive) {
                    Text("ACTIVO PARA PISTA", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = "Editar", tint = Color.Gray)
            }
        }
    }
}

@Composable
fun VehicleDialog(
    title: String,
    initialModel: String = "",
    initialPhotoUri: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, String?) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var model by remember { mutableStateOf(initialModel) }
    var photoUri by remember { mutableStateOf(initialPhotoUri) }
    val context = LocalContext.current
    
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { sourceUri ->
            try {
                val inputStream = context.contentResolver.openInputStream(sourceUri)
                val file = java.io.File(context.filesDir, "vehicle_${System.currentTimeMillis()}.jpg")
                val outputStream = java.io.FileOutputStream(file)
                inputStream?.use { it.copyTo(outputStream) }
                photoUri = Uri.fromFile(file).toString()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .align(Alignment.CenterHorizontally)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Gray)
                        .clickable { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    contentAlignment = Alignment.Center
                ) {
                    if (photoUri != null) {
                        AsyncImage(model = photoUri, contentDescription = null, contentScale = ContentScale.Crop)
                    } else {
                        Icon(Icons.Filled.AddAPhoto, contentDescription = null, tint = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("Modelo del Vehículo") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(model, photoUri) }) {
                Text("ACEPTAR")
            }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                        Text("ELIMINAR")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("CANCELAR")
                }
            }
        }
    )
}
