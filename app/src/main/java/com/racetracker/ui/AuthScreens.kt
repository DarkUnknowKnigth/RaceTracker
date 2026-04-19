package com.racetracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.racetracker.data.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(db: AppDatabase, onLoginSuccess: (Int) -> Unit, onNavigateRegister: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    var errorMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("RACE TRACKER", color = MaterialTheme.colorScheme.primary, fontSize = 32.sp, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Usuario") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            modifier = Modifier.fillMaxWidth() // In a real app add visual transformation for password
        )
        
        if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                coroutineScope.launch {
                    val user = db.raceDao().getUserByUsername(username)
                    if (user != null && user.passwordHash == password) {
                        onLoginSuccess(user.id)
                    } else {
                        errorMessage = "Credenciales incorrectas"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("ENTRAR", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onNavigateRegister) {
            Text("¿Nuevo Piloto? Regístrate")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(db: AppDatabase, onRegisterSuccess: (Int) -> Unit, onNavigateLogin: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var vehicleModel by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("NUEVO PILOTO", color = MaterialTheme.colorScheme.primary, fontSize = 28.sp, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Usuario") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = vehicleModel,
            onValueChange = { vehicleModel = it },
            label = { Text("Modelo de Vehículo") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                coroutineScope.launch {
                    val userId = db.raceDao().insertUser(UserEntity(username = username, passwordHash = password, vehicleModel = vehicleModel)).toInt()
                    db.raceDao().insertVehicle(VehicleEntity(userId = userId, model = vehicleModel))
                    onRegisterSuccess(userId)
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("REGISTRARSE", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onNavigateLogin) {
            Text("Volver al Login")
        }
    }
}
