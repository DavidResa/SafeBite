package com.example.safebite

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.safebite.ui.auth.LoginScreen
import com.example.safebite.ui.auth.SignupScreen
import com.example.safebite.ui.home.HomeScreen
import com.example.safebite.ui.profile.ProfileScreen
import com.example.safebite.ui.scanner.ResultScreen
import com.example.safebite.ui.scanner.ScannerScreen
import com.example.safebite.ui.scanner.CameraScannerTab
import com.example.safebite.ui.scanner.UserDetailScreen
import com.example.safebite.ui.theme.SafeBiteTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val auth: FirebaseAuth = Firebase.auth
        val db: FirebaseFirestore = Firebase.firestore

        enableEdgeToEdge()
        setContent {
            SafeBiteTheme {
                val navController = rememberNavController()
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()
                
                var loggedUserName by remember { mutableStateOf<String?>(null) }
                var hasNotifications by remember { mutableStateOf(false) }
                var hasWarnings by remember { mutableStateOf(false) }
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                LaunchedEffect(auth.currentUser) {
                    val user = auth.currentUser
                    if (user != null) {
                        // Admin Creation Routine
                        try {
                            val adminUsername = "Admin"
                            val adminEmail = "admin@safebite.com"
                            val adminPass = "Admin1234"
                            
                            val existingAdmin = db.collection("usernames").document(adminUsername).get().await()
                            if (!existingAdmin.exists()) {
                                try {
                                    auth.createUserWithEmailAndPassword(adminEmail, adminPass).await()
                                } catch (e: Exception) {
                                    try { auth.signInWithEmailAndPassword(adminEmail, adminPass).await() } catch(e: Exception) {}
                                }
                                
                                val userId = auth.currentUser?.uid
                                if (userId != null) {
                                    val userMap = hashMapOf(
                                        "username" to adminUsername,
                                        "email" to adminEmail,
                                        "password" to adminPass,
                                        "isAdmin" to true
                                    )
                                    db.collection("users").document(userId).set(userMap).await()
                                    db.collection("usernames").document(adminUsername).set(hashMapOf("email" to adminEmail)).await()
                                }
                            } else {
                                val usersQuery = db.collection("users").whereEqualTo("username", adminUsername).get().await()
                                for (doc in usersQuery) {
                                    if (doc.getBoolean("isAdmin") != true) {
                                        db.collection("users").document(doc.id).update("isAdmin", true).await()
                                    }
                                }
                            }
                        } catch (e: com.google.firebase.firestore.FirebaseFirestoreException) {
                            if (e.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                                Log.e("AdminSetup", "Permiso denegado en rutina de admin. Revisa las reglas de Firestore.")
                            } else {
                                Log.e("AdminSetup", "Error en rutina de admin", e)
                            }
                        } catch (e: Exception) {
                            Log.e("AdminSetup", "Error inesperado en rutina de admin", e)
                        }

                        // Friend requests listener
                        db.collection("friend_requests")
                            .whereEqualTo("toId", user.uid)
                            .whereEqualTo("status", "pending")
                            .addSnapshotListener { snapshot, _ ->
                                hasNotifications = snapshot != null && !snapshot.isEmpty
                                Log.d("MainActivity", "Notifications listener: hasNotifications=$hasNotifications")
                            }

                        // Warnings listener
                        db.collection("notifications")
                            .whereEqualTo("toId", user.uid)
                            .whereEqualTo("type", "warning")
                            .whereEqualTo("read", false)
                            .addSnapshotListener { snapshot, _ ->
                                hasWarnings = snapshot != null && !snapshot.isEmpty
                                Log.d("MainActivity", "Warnings listener: hasWarnings=$hasWarnings for UID ${user.uid}")
                            }
                    } else {
                        hasNotifications = false
                        hasWarnings = false
                    }
                }

                LaunchedEffect(Unit) {
                    val user = auth.currentUser
                    if (user != null) {
                        try {
                            val doc = db.collection("users").document(user.uid).get().await()
                            
                            Log.d("Auth", "Logged in as ${doc.getString("username")}, isAdmin=${doc.getBoolean("isAdmin")}")
                            
                            // Check for ban
                            val bannedUntil = doc.getLong("bannedUntil")
                            if (bannedUntil != null && bannedUntil > System.currentTimeMillis()) {
                                auth.signOut()
                                val remaining = bannedUntil - System.currentTimeMillis()
                                val hours = remaining / (3600000)
                                val minutes = (remaining % 3600000) / 60000
                                snackbarHostState.showSnackbar("cuenta baneada, le quedan $hours horas y $minutes minutos")
                                return@LaunchedEffect
                            }

                            loggedUserName = doc.getString("username") ?: "Usuario"
                            navController.navigate("home/$loggedUserName") {
                                popUpTo("login") { inclusive = true }
                            }
                        } catch (e: Exception) {
                            Log.e("Auth", "Error en autologin", e)
                        }
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    bottomBar = {
                        if (currentRoute != null && (currentRoute.startsWith("home") || currentRoute.startsWith("scanner") || currentRoute == "profile")) {
                            NavigationBar {
                                NavigationBarItem(
                                    selected = currentRoute.startsWith("home"),
                                    onClick = { 
                                        if (!currentRoute.startsWith("home")) {
                                            navController.navigate("home/${loggedUserName ?: "Usuario"}") {
                                                popUpTo(navController.graph.startDestinationId)
                                                launchSingleTop = true
                                            }
                                        }
                                    },
                                    icon = { Icon(Icons.Default.Home, contentDescription = "Inicio") },
                                    label = { Text("Inicio") }
                                )
                                NavigationBarItem(
                                    selected = currentRoute.startsWith("scanner"),
                                    onClick = {
                                        if (!currentRoute.startsWith("scanner")) {
                                            navController.navigate("scanner/${loggedUserName ?: "Usuario"}") {
                                                launchSingleTop = true
                                            }
                                        }
                                    },
                                    icon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
                                    label = { Text("Buscar") }
                                )
                                NavigationBarItem(
                                    selected = currentRoute == "notifications",
                                    onClick = {
                                        if (currentRoute != "notifications") {
                                            navController.navigate("notifications") {
                                                launchSingleTop = true
                                            }
                                        }
                                    },
                                    icon = { 
                                        Icon(
                                            Icons.Default.Notifications, 
                                            contentDescription = "Alertas",
                                            tint = when {
                                                hasWarnings && currentRoute != "notifications" -> Color(0xFFFFD700) // Gold
                                                hasNotifications && currentRoute != "notifications" -> Color.Red
                                                else -> LocalContentColor.current
                                            }
                                        ) 
                                    },
                                    label = { Text("Alertas", maxLines = 1, softWrap = false) }
                                )
                                NavigationBarItem(
                                    selected = currentRoute == "profile",
                                    onClick = {
                                        if (currentRoute != "profile") {
                                            navController.navigate("profile") {
                                                launchSingleTop = true
                                            }
                                        }
                                    },
                                    icon = { Icon(Icons.Default.Person, contentDescription = "Perfil") },
                                    label = { Text("Perfil") }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "login",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("login") {
                            LoginScreen(
                                onLoginSuccess = { name ->
                                    loggedUserName = name
                                    navController.navigate("home/$name") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                },
                                onNavigateToSignup = { navController.navigate("signup") },
                                onLoginError = { errorMessage ->
                                    scope.launch { snackbarHostState.showSnackbar(errorMessage) }
                                },
                                 loginAction = { usernameOrEmail, pass ->
                                    try {
                                        var emailToUse = usernameOrEmail
                                        if (!usernameOrEmail.contains("@")) {
                                            try {
                                                val doc = db.collection("usernames").document(usernameOrEmail).get().await()
                                                if (doc.exists()) {
                                                    emailToUse = doc.getString("email") ?: throw Exception("Formato de usuario inválido en la base de datos")
                                                } else {
                                                    throw Exception("Usuario no encontrado")
                                                }
                                            } catch (e: com.google.firebase.firestore.FirebaseFirestoreException) {
                                                if (e.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                                                    throw Exception("Error de permisos: Las reglas de Firestore están bloqueando el acceso. Por favor, revisa la consola de Firebase.")
                                                } else throw e
                                            }
                                        }
                                        
                                        val result = auth.signInWithEmailAndPassword(emailToUse, pass).await()
                                        val userId = result.user?.uid
                                        if (userId != null) {
                                            try {
                                                val doc = db.collection("users").document(userId).get().await()
                                                if (!doc.exists()) {
                                                    throw Exception("Perfil de usuario no encontrado en Firestore")
                                                }
                                                val bannedUntil = doc.getLong("bannedUntil")
                                                if (bannedUntil != null && bannedUntil > System.currentTimeMillis()) {
                                                    auth.signOut()
                                                    val remaining = bannedUntil - System.currentTimeMillis()
                                                    val hours = remaining / (3600000)
                                                    val minutes = (remaining % 3600000) / 60000
                                                    throw Exception("Cuenta baneada, le quedan $hours horas y $minutes minutos")
                                                }
                                            } catch (e: com.google.firebase.firestore.FirebaseFirestoreException) {
                                                if (e.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                                                    throw Exception("Permisos insuficientes para leer tu perfil. Revisa las reglas de seguridad.")
                                                } else throw e
                                            }
                                        }
                                        null
                                    } catch (e: Exception) {
                                        Log.e("Auth", "Login error: ${e.message}")
                                        e.localizedMessage ?: "Error de login"
                                    }
                                }
                            )
                        }

                        composable("signup") {
                            SignupScreen(
                                onSignupSuccess = {
                                    scope.launch { snackbarHostState.showSnackbar("La cuenta ya está creada") }
                                    navController.navigate("login") {
                                        popUpTo("signup") { inclusive = true }
                                    }
                                },
                                onNavigateToLogin = { navController.navigate("login") },
                                onSignupError = { errorMessage ->
                                    scope.launch { snackbarHostState.showSnackbar(errorMessage) }
                                },
                                 signupAction = { username, email, pass, allergens ->
                                    try {
                                        try {
                                            val existing = db.collection("usernames").document(username).get().await()
                                            if (existing.exists()) {
                                                "El nombre de usuario ya está en uso"
                                            } else {
                                                val result = auth.createUserWithEmailAndPassword(email, pass).await()
                                                val userId = result.user?.uid
                                                if (userId != null) {
                                                    val userMap = hashMapOf(
                                                        "username" to username,
                                                        "email" to email,
                                                        "password" to pass,
                                                        "allergens" to allergens,
                                                        "isAdmin" to false
                                                    )
                                                    db.collection("users").document(userId).set(userMap).await()
                                                    db.collection("usernames").document(username).set(hashMapOf("email" to email)).await()
                                                    null
                                                } else "ID error"
                                            }
                                        } catch (e: com.google.firebase.firestore.FirebaseFirestoreException) {
                                            if (e.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                                                "Error de permisos al verificar el usuario. Revisa las reglas de Firestore."
                                            } else throw e
                                        }
                                    } catch (e: Exception) {
                                        Log.e("Auth", "Signup error: ${e.message}")
                                        e.localizedMessage ?: "Error de registro"
                                    }
                                }
                            )
                        }

                        composable(
                            "home/{name}",
                            arguments = listOf(navArgument("name") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val name = backStackEntry.arguments?.getString("name") ?: "Usuario"
                            HomeScreen(
                                userName = name,
                                onNavigateToScanner = { navController.navigate("camera/$name") },
                                onSearchBarcode = { barcode ->
                                    navController.navigate("result/$name/$barcode")
                                },
                                onNavigateToShoppingList = { navController.navigate("shoppingList") }
                            )
                        }

                        composable(
                            "camera/{name}",
                            arguments = listOf(navArgument("name") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val name = backStackEntry.arguments?.getString("name") ?: ""
                            CameraScannerTab(
                                onBarcodeDetected = { barcode ->
                                    navController.navigate("result/$name/$barcode") {
                                        popUpTo("camera/$name") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable(
                            "scanner/{name}",
                            arguments = listOf(navArgument("name") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val name = backStackEntry.arguments?.getString("name") ?: ""
                            ScannerScreen(
                                onBarcodeDetected = { barcode ->
                                    navController.navigate("result/$name/$barcode") {
                                        popUpTo("scanner/$name") { inclusive = true }
                                    }
                                },
                                onUserSelected = { targetUserId ->
                                    navController.navigate("userDetail/$targetUserId")
                                }
                            )
                        }

                        composable(
                            "result/{name}/{barcode}",
                            arguments = listOf(
                                navArgument("name") { type = NavType.StringType },
                                navArgument("barcode") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val name = backStackEntry.arguments?.getString("name") ?: ""
                            val barcode = backStackEntry.arguments?.getString("barcode") ?: ""
                            var userAllergens by remember { mutableStateOf("") }
                            
                            LaunchedEffect(name) {
                                try {
                                    val userId = auth.currentUser?.uid
                                    if(userId != null) {
                                        val doc = db.collection("users").document(userId).get().await()
                                        userAllergens = doc.getString("allergens") ?: ""
                                    }
                                } catch (e: Exception) { }
                            }
                            
                            ResultScreen(
                                barcode = barcode,
                                userAllergens = userAllergens,
                                onBack = { navController.navigateUp() }
                            )
                        }
                        
                        composable("profile") {
                            ProfileScreen(
                                onLogout = {
                                    loggedUserName = null
                                    navController.navigate("login") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("notifications") {
                            com.example.safebite.ui.notifications.NotificationsScreen(
                                onNavigateToUser = { userId ->
                                    navController.navigate("userDetail/$userId")
                                }
                            )
                        }
                        
                        composable(
                            "userDetail/{userId}",
                            arguments = listOf(navArgument("userId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val userId = backStackEntry.arguments?.getString("userId") ?: ""
                            UserDetailScreen(
                                targetUserId = userId,
                                onBack = { navController.navigateUp() }
                            )
                        }

                        composable("shoppingList") {
                            com.example.safebite.ui.shoppinglist.ShoppingListScreen(
                                onBack = { navController.navigateUp() },
                                onProductClick = { barcode ->
                                    val currentUser = auth.currentUser
                                    if (currentUser != null) {
                                        // We need the username for the result route
                                        db.collection("users").document(currentUser.uid).get().addOnSuccessListener { doc ->
                                            val name = doc.getString("username") ?: "Usuario"
                                            navController.navigate("result/$name/$barcode")
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}