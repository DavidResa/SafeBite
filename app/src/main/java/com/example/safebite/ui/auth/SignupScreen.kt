package com.example.safebite.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun SignupScreen(
    onSignupSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onSignupError: (String) -> Unit,
    signupAction: suspend (String, String, String, String) -> String?
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    
    var usernameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    val allergensList = listOf("Leche", "Huevos", "Frutos de cáscara", "Trigo", "Soja", "Pescado", "Mariscos", "Cacahuetes")
    val selectedAllergens = remember { mutableStateListOf<String>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Text(text = "Crear Usuario", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(32.dp))
        
        TextField(
            value = username,
            onValueChange = { 
                username = it
                if (it.isNotEmpty()) usernameError = null
            },
            label = { Text("Nombre de usuario") },
            isError = usernameError != null,
            supportingText = {
                if (usernameError != null) {
                    Text(text = usernameError!!, color = MaterialTheme.colorScheme.error)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = email,
            onValueChange = { 
                email = it
                if (it.isNotEmpty()) emailError = null
            },
            label = { Text("Correo electrónico") },
            isError = emailError != null,
            supportingText = {
                if (emailError != null) {
                    Text(text = emailError!!, color = MaterialTheme.colorScheme.error)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        TextField(
            value = password,
            onValueChange = { 
                password = it
                if (it.isNotEmpty()) passwordError = null
            },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            isError = passwordError != null,
            supportingText = {
                if (passwordError != null) {
                    Text(text = passwordError!!, color = MaterialTheme.colorScheme.error)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "Selecciona tus alérgenos:", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        
        allergensList.forEach { allergen ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = selectedAllergens.contains(allergen),
                    onCheckedChange = { checked ->
                        if (checked) selectedAllergens.add(allergen)
                        else selectedAllergens.remove(allergen)
                    }
                )
                Text(text = allergen)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        val scope = rememberCoroutineScope()
        Button(
            onClick = {
                scope.launch {
                    usernameError = null
                    emailError = null
                    passwordError = null

                    if (username.isEmpty()) {
                        usernameError = "Por favor, introduce tu nombre de usuario"
                    } else if (email.isEmpty()) {
                        emailError = "Por favor, introduce tu correo electrónico"
                    } else if (password.isEmpty()) {
                        passwordError = "Por favor, introduce tu contraseña"
                    } else {
                        val allergensString = selectedAllergens.joinToString(",")
                        val error = signupAction(username, email, password, allergensString)
                        if (error == null) {
                            onSignupSuccess()
                        } else {
                            onSignupError(error)
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Registrarse")
        }
        
        TextButton(onClick = onNavigateToLogin) {
            Text("¿Ya tienes cuenta? Inicia sesión")
        }
    }
}
