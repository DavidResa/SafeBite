package com.example.safebite.ui.scanner

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import com.example.safebite.ui.profile.AvatarImage
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.layout.ContentScale
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDetailScreen(
    targetUserId: String,
    onBack: () -> Unit
) {
    var username by remember { mutableStateOf<String?>(null) }
    var allergens by remember { mutableStateOf<String?>(null) }
    var profileImageUrl by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isFriend by remember { mutableStateOf(false) }
    var actionLoading by remember { mutableStateOf(false) }

    val db = Firebase.firestore
    val auth = Firebase.auth
    val currentUserId = auth.currentUser?.uid

    LaunchedEffect(targetUserId) {
        isLoading = true
        try {
            // Fetch target user data
            val doc = db.collection("users").document(targetUserId).get().await()
            if (doc.exists()) {
                username = doc.getString("username")
                allergens = doc.getString("allergens")
                profileImageUrl = doc.getString("profileImageUrl")
            }

            // Check if already friends
            if (currentUserId != null) {
                val currentUserDoc = db.collection("users").document(currentUserId).get().await()
                val friendsList = currentUserDoc.get("friends") as? List<String>
                isFriend = friendsList?.contains(targetUserId) == true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(username ?: "Perfil de Usuario") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (username == null) {
                Text(
                    "Usuario no encontrado",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AvatarImage(
                        imageUrl = profileImageUrl,
                        modifier = Modifier.size(100.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop,
                        placeholderTint = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = username!!,
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Registro de Alergias",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            val allergensText = if (allergens.isNullOrEmpty()) "Ninguna alergia registrada" else allergens!!
                            Text(
                                text = allergensText,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    if (currentUserId != null && currentUserId != targetUserId) {
                        Button(
                            onClick = {
                                actionLoading = true
                                val userRef = db.collection("users").document(currentUserId)
                                val task = if (isFriend) {
                                    userRef.update("friends", FieldValue.arrayRemove(targetUserId))
                                } else {
                                    userRef.update("friends", FieldValue.arrayUnion(targetUserId))
                                }
                                
                                task.addOnSuccessListener {
                                    isFriend = !isFriend
                                    actionLoading = false
                                }.addOnFailureListener {
                                    actionLoading = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !actionLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isFriend) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        ) {
                            if (actionLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text(if (isFriend) "Eliminar Amigo" else "Agregar Amigo")
                            }
                        }
                    }
                }
            }
        }
    }
}
