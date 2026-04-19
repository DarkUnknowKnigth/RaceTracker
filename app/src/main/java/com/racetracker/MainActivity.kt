package com.racetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.racetracker.data.AppDatabase
import com.racetracker.ui.*
val RacingDarkColors = darkColorScheme(
    primary = Color(0xFFE94560), // Racing Red
    background = Color(0xFF0B0C10),
    surface = Color(0xFF1F2833),
    onPrimary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

class MainActivity : ComponentActivity() {
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = AppDatabase.getDatabase(this)
        
        setContent {
            MaterialTheme(colorScheme = RacingDarkColors) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RaceTrackerApp(db)
                }
            }
        }
    }
}

@Composable
fun RaceTrackerApp(db: AppDatabase) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("race_tracker_prefs", Context.MODE_PRIVATE)
    var currentUserId by remember { mutableStateOf<Int?>(prefs.getInt("LOGGED_IN_USER_ID", -1).takeIf { it != -1 }) }
    var globalSelectedSessionId by remember { mutableStateOf<Int?>(null) }
    
    // Bottom Bar state
    var currentRoute by remember { mutableStateOf("dashboard") }

    Scaffold(
        bottomBar = {
            if (currentUserId != null) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    NavigationBarItem(
                        selected = currentRoute == "dashboard",
                        onClick = { currentRoute = "dashboard"; navController.navigate("dashboard") { launchSingleTop = true; popUpTo("dashboard") } },
                        icon = { Icon(Icons.Filled.PlayArrow, contentDescription = "Medir") },
                        label = { Text("Medir") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == "last_map",
                        onClick = { currentRoute = "last_map"; navController.navigate("last_map") { launchSingleTop = true; popUpTo("dashboard") } },
                        icon = { Icon(Icons.Filled.Map, contentDescription = "Mapa") },
                        label = { Text("Mapa") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == "stats",
                        onClick = { currentRoute = "stats"; navController.navigate("stats") { launchSingleTop = true; popUpTo("dashboard") } },
                        icon = { Icon(Icons.Filled.Star, contentDescription = "Top Speed") },
                        label = { Text("Stats") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == "history",
                        onClick = { currentRoute = "history"; navController.navigate("history") { launchSingleTop = true; popUpTo("dashboard") } },
                        icon = { Icon(Icons.Filled.History, contentDescription = "Registro") },
                        label = { Text("Registro") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == "perfil",
                        onClick = { currentRoute = "perfil"; navController.navigate("perfil") { launchSingleTop = true; popUpTo("dashboard") } },
                        icon = { Icon(Icons.Filled.Person, contentDescription = "Perfil") },
                        label = { Text("Perfil") }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController, 
            startDestination = if (currentUserId != null) "dashboard" else "login", 
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("login") {
                LoginScreen(
                    db = db,
                    onLoginSuccess = { userId ->
                        prefs.edit().putInt("LOGGED_IN_USER_ID", userId).apply()
                        currentUserId = userId; currentRoute = "dashboard"
                        navController.navigate("dashboard") { popUpTo("login") { inclusive = true } }
                    },
                    onNavigateRegister = { navController.navigate("register") }
                )
            }
            composable("register") {
                RegisterScreen(
                    db = db,
                    onRegisterSuccess = { userId ->
                        prefs.edit().putInt("LOGGED_IN_USER_ID", userId).apply()
                        currentUserId = userId; currentRoute = "dashboard"
                        navController.navigate("dashboard") { popUpTo("login") { inclusive = true } }
                    },
                    onNavigateLogin = { navController.popBackStack() }
                )
            }
            composable("dashboard") {
                currentUserId?.let { userId ->
                    DashboardScreen(userId = userId, db = db, onLogout = {
                        globalSelectedSessionId = null // Reset on new track
                        currentRoute = "stats"
                        navController.navigate("stats") { popUpTo("dashboard") }
                    })
                }
            }
            composable("perfil") {
                currentUserId?.let { userId ->
                    PerfilScreen(userId = userId, db = db, onLogout = {
                        prefs.edit().remove("LOGGED_IN_USER_ID").apply()
                        currentUserId = null
                        globalSelectedSessionId = null
                        navController.navigate("login") { popUpTo(0) }
                    })
                }
            }
            composable("last_map") {
                currentUserId?.let { userId ->
                    var lastSessionId by remember { mutableStateOf<Int?>(null) }
                    LaunchedEffect(userId) {
                        db.raceDao().getLastSessionForUser(userId).collect { session ->
                            lastSessionId = session?.id
                        }
                    }
                    val sessionToShow = globalSelectedSessionId ?: lastSessionId
                    if (sessionToShow != null) {
                        MapScreen(
                            sessionId = sessionToShow, 
                            db = db, 
                            onBack = { navController.popBackStack() },
                            onNavigateToStats = { 
                                currentRoute = "stats"
                                navController.navigate("stats") { launchSingleTop = true; popUpTo("dashboard") } 
                            }
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text("Aún no tienes rutas guardadas.", color = Color.White)
                        }
                    }
                }
            }
            composable("stats") {
                currentUserId?.let { userId ->
                    StatsScreen(
                        userId = userId, 
                        db = db, 
                        sessionId = globalSelectedSessionId, 
                        onNavigateToMap = { 
                            currentRoute = "last_map"
                            navController.navigate("last_map") { launchSingleTop = true; popUpTo("dashboard") }
                        }
                    )
                }
            }
            composable("history") {
                currentUserId?.let { userId ->
                    HistoryScreen(userId = userId, db = db, onSessionSelected = { sessionId ->
                        globalSelectedSessionId = sessionId
                        currentRoute = "stats"
                        navController.navigate("stats") { launchSingleTop = true; popUpTo("dashboard") }
                    })
                }
            }
        }
    }
}
