package com.example.safebite

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.safebite.data.AppDatabase
import com.example.safebite.data.User
import com.example.safebite.ui.auth.LoginScreen
import com.example.safebite.ui.auth.SignupScreen
import com.example.safebite.ui.home.HomeScreen
import com.example.safebite.ui.scanner.ResultScreen
import com.example.safebite.ui.scanner.ScannerScreen
import com.example.safebite.ui.theme.SafeBiteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = AppDatabase.getDatabase(this)
        val userDao = database.userDao()

        enableEdgeToEdge()
        setContent {
            SafeBiteTheme {
                val navController = rememberNavController()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "login",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("login") {
                            LoginScreen(
                                onLoginSuccess = { name ->
                                    navController.navigate("home/$name") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                },
                                onNavigateToSignup = { navController.navigate("signup") },
                                onLoginError = { /* Show Snackbar or toast */ },
                                loginAction = { name, pass ->
                                    val user = userDao.login(name, pass)
                                    user != null
                                }
                            )
                        }
                        composable("signup") {
                            SignupScreen(
                                onSignupSuccess = {
                                    navController.navigate("login")
                                },
                                onNavigateToLogin = { navController.navigate("login") },
                                onSignupError = { /* Show error */ },
                                signupAction = { name, pass, allergens ->
                                    if (!userDao.userExists(name)) {
                                        userDao.signup(User(name = name, password = pass, allergens = allergens))
                                        true
                                    } else {
                                        false
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
                            
                            // Get user allergens from database
                            var userAllergens by remember { mutableStateOf("") }
                            LaunchedEffect(name) {
                                val user = userDao.getUserByName(name)
                                userAllergens = user?.allergens ?: ""
                            }
                            
                            ResultScreen(
                                barcode = barcode,
                                userAllergens = userAllergens,
                                onBack = { navController.navigateUp() }
                            )
                        }
                    }
                }
            }
        }
    }
}