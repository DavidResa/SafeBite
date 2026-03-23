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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
import com.example.safebite.ui.theme.SafeBiteTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
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
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

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
                                    icon = { Icon(Icons.Default.Search, contentDescription = "Scanner") },
                                    label = { Text("Scanner") }
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
                                            val doc = db.collection("usernames").document(usernameOrEmail).get().await()
                                            if (doc.exists()) {
                                                emailToUse = doc.getString("email") ?: throw Exception("Formato inválido")
                                            } else {
                                                throw Exception("Usuario no encontrado")
                                            }
                                        }
                                        auth.signInWithEmailAndPassword(emailToUse, pass).await()
                                        null
                                    } catch (e: Exception) {
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
                                                    "allergens" to allergens
                                                )
                                                db.collection("users").document(userId).set(userMap).await()
                                                db.collection("usernames").document(username).set(hashMapOf("email" to email)).await()
                                                null
                                            } else "ID error"
                                        }
                                    } catch (e: Exception) {
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
                                onNavigateToScanner = { navController.navigate("scanner/$name") },
                                onSearchBarcode = { barcode ->
                                    navController.navigate("result/$name/$barcode")
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
                            ProfileScreen()
                        }
                    }
                }
            }
        }
    }
}