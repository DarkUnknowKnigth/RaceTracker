package com.racetracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.racetracker.data.AppDatabase
import com.racetracker.data.UserEntity

@Composable
fun PerfilScreen(userId: Int, db: AppDatabase, onLogout: () -> Unit) {
    var user by remember { mutableStateOf<UserEntity?>(null) }

    LaunchedEffect(userId) {
        db.raceDao().getUserById(userId).collect {
            user = it
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("PERFIL DEL PILOTO", color = MaterialTheme.colorScheme.primary, fontSize = 24.sp, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(32.dp))

        if (user != null) {
            Card(
                modifier = Modifier.fillMaxWidth().height(150.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("PILOTO", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(user!!.username, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("VEHÍCULO", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(user!!.vehicleModel, color = MaterialTheme.colorScheme.primary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            CircularProgressIndicator()
        }

        Spacer(modifier = Modifier.height(64.dp))
        
        Button(
            onClick = onLogout, 
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("CERRAR SESIÓN", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}
