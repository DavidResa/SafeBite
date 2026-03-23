package com.example.safebite.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.safebite.R
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: (String) -> Unit,
    onNavigateToSignup: () -> Unit,
    onLoginError: (String) -> Unit,
    loginAction: suspend (String, String) -> String?
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var usernameError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "SafeBite Logo",
            modifier = Modifier
                .height(120.dp)
                .padding(bottom = 32.dp)
        )
        
        Text(
            text = "SafeBite",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.tertiary
        )
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
        
        Button(
            onClick = {
                scope.launch {
                    usernameError = null
                    passwordError = null
                    
                    if (username.isEmpty()) {
                        usernameError = "Por favor, introduce tu nombre de usuario"
                    } else if (password.isEmpty()) {
                        passwordError = "Por favor, introduce tu contraseña"
                    } else {
                        val error = loginAction(username, password)
                        if (error == null) {
                            onLoginSuccess(username) // Pass plain username back
                        } else {
                            onLoginError(error)
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Entrar")
        }
        
        TextButton(onClick = onNavigateToSignup) {
            Text("Crear nuevo usuario")
        }
    }
}
