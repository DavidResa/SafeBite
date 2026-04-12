package com.example.safebite.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.safebite.data.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onLogout: () -> Unit = {}) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val userId = auth.currentUser?.uid
    
    var userProfile by remember { mutableStateOf<User?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    var friendsMap by remember { mutableStateOf<Map<String, Pair<String, String?>>>(emptyMap()) }

    DisposableEffect(userId) {
        if (userId == null) {
            isLoading = false
            return@DisposableEffect onDispose {}
        }
        val listener = db.collection("users").document(userId).addSnapshotListener { snapshot, e ->
            if (e != null) {
                error = e.localizedMessage ?: "Error al cargar perfil"
                isLoading = false
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val friendsList = snapshot.get("friends") as? List<String> ?: emptyList()
                userProfile = User(
                    id = userId,
                    username = snapshot.getString("username") ?: "",
                    email = snapshot.getString("email") ?: "",
                    password = snapshot.getString("password") ?: "",
                    allergens = snapshot.getString("allergens") ?: "",
                    friends = friendsList,
                    profileImageUrl = snapshot.getString("profileImageUrl")
                )
                
                if (friendsList.isNotEmpty()) {
                    db.collection("users").get().addOnSuccessListener { usersSnap ->
                        val newFriendsMap = mutableMapOf<String, Pair<String, String?>>()
                        for (doc in usersSnap.documents) {
                            if (friendsList.contains(doc.id)) {
                                val username = doc.getString("username") ?: "Desconocido"
                                val profileImageUrl = doc.getString("profileImageUrl")
                                newFriendsMap[doc.id] = Pair(username, profileImageUrl)
                            }
                        }
                        friendsMap = newFriendsMap
                        isLoading = false
                    }.addOnFailureListener {
                        isLoading = false
                    }
                } else {
                    friendsMap = emptyMap()
                    isLoading = false
                }
            } else {
                isLoading = false
            }
        }
        
        onDispose {
            listener.remove()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Mi Cuenta") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                actions = {
                    var showSettingsDialog by remember { mutableStateOf(false) }
                    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
                    var isProcessing by remember { mutableStateOf(false) }
                    val currentContext = androidx.compose.ui.platform.LocalContext.current
                    val authInst = FirebaseAuth.getInstance()
                    val dbInst = FirebaseFirestore.getInstance()

                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Ajustes")
                    }

                    if (showSettingsDialog) {
                        AlertDialog(
                            onDismissRequest = { if (!isProcessing) showSettingsDialog = false },
                            title = { Text("Ajustes de Cuenta") },
                            text = {
                                Column {
                                    Button(
                                        onClick = { 
                                            authInst.signOut() 
                                            showSettingsDialog = false
                                            onLogout()
                                        }, 
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Cerrar Sesión")
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = { 
                                            showSettingsDialog = false
                                            showDeleteConfirmDialog = true
                                        }, 
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Eliminar Cuenta")
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { showSettingsDialog = false }) {
                                    Text("Cerrar")
                                }
                            }
                        )
                    }

                    if (showDeleteConfirmDialog) {
                        AlertDialog(
                            onDismissRequest = { if (!isProcessing) showDeleteConfirmDialog = false },
                            title = { Text("Eliminar Cuenta", color = MaterialTheme.colorScheme.error) },
                            text = { 
                                if (isProcessing) {
                                    CircularProgressIndicator()
                                } else {
                                    Text("¿Estás completamente seguro de que quieres eliminar tu cuenta? Esta acción no se puede deshacer y perderás todos tus datos.") 
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        isProcessing = true
                                        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                                            try {
                                                val currentUser = authInst.currentUser
                                                val uid = currentUser?.uid
                                                if (uid != null) {
                                                    // Optionally delete username binding
                                                    val usernameDoc = dbInst.collection("users").document(uid).get().await()
                                                    val uName = usernameDoc.getString("username")
                                                    if (uName != null) {
                                                        dbInst.collection("usernames").document(uName).delete().await()
                                                    }
                                                    dbInst.collection("users").document(uid).delete().await()
                                                    currentUser.delete().await()
                                                }
                                                withContext(Dispatchers.Main) {
                                                    isProcessing = false
                                                    showDeleteConfirmDialog = false
                                                    android.widget.Toast.makeText(currentContext, "Cuenta eliminada correctamente", android.widget.Toast.LENGTH_LONG).show()
                                                    onLogout()
                                                }
                                            } catch (e: Exception) {
                                                withContext(Dispatchers.Main) {
                                                    isProcessing = false
                                                    android.widget.Toast.makeText(currentContext, "Por seguridad requieres iniciar sesión nuevamente para borrar la cuenta.", android.widget.Toast.LENGTH_LONG).show()
                                                    authInst.signOut()
                                                    onLogout()
                                                }
                                            }
                                        }
                                    },
                                    enabled = !isProcessing,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Eliminar definitivamente")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteConfirmDialog = false }, enabled = !isProcessing) {
                                    Text("Cancelar")
                                }
                            }
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(32.dp))
            } else if (error != null) {
                Text(text = error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(32.dp))
            } else if (userProfile != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val currentContext = androidx.compose.ui.platform.LocalContext.current
                    var isUploadingAvatar by remember { mutableStateOf(false) }
                    val coroutineScope = rememberCoroutineScope()
                    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
                        contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
                    ) { uri ->
                        if (uri != null && userId != null) {
                            isUploadingAvatar = true
                            coroutineScope.launch(Dispatchers.IO) {
                                val base64Str = uriToBase64String(currentContext, uri)
                                withContext(Dispatchers.Main) {
                                    if (base64Str != null) {
                                        val oldImageUrl = userProfile?.profileImageUrl
                                        db.collection("users").document(userId)
                                            .update("profileImageUrl", base64Str)
                                            .addOnSuccessListener {
                                                isUploadingAvatar = false
                                                android.widget.Toast.makeText(currentContext, "Foto guardada correctamente", android.widget.Toast.LENGTH_SHORT).show()
                                                if (oldImageUrl != null && oldImageUrl.contains("firebasestorage")) {
                                                    try {
                                                        com.google.firebase.storage.FirebaseStorage.getInstance().getReferenceFromUrl(oldImageUrl).delete()
                                                    } catch (e: Exception) { }
                                                }
                                            }
                                            .addOnFailureListener { e ->
                                                isUploadingAvatar = false
                                                android.widget.Toast.makeText(currentContext, "Error BD: ${e.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                    } else {
                                        isUploadingAvatar = false
                                        android.widget.Toast.makeText(currentContext, "Error al convertir imagen a guardado local", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clickable { 
                                if (!isUploadingAvatar) {
                                    launcher.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly))
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        AvatarImage(
                            imageUrl = userProfile!!.profileImageUrl,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop,
                            placeholderTint = MaterialTheme.colorScheme.primary
                        )
                        if (isUploadingAvatar) {
                            CircularProgressIndicator()
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = userProfile!!.username,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(32.dp))

                    ProfileDetailRow(
                        icon = Icons.Default.Email,
                        label = "Correo electrónico",
                        value = userProfile!!.email
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                    ProfileAllergiesSection(userProfile!!.allergens)
                    
                    if (userProfile!!.friends.isNotEmpty()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                        ProfileFriendsSection(friendsMap)
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileDetailRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
            Text(text = value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
fun ProfileAllergiesSection(allergens: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = "Tus Alérgenos", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
        }
        Spacer(modifier = Modifier.height(12.dp))
        if (allergens.isEmpty()) {
            Text("No tienes alérgenos registrados", style = MaterialTheme.typography.bodyMedium)
        } else {
            allergens.split(",").forEach { allergen ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        text = allergen.trim(),
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileFriendsSection(friends: Map<String, Pair<String, String?>>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = "Mis Amigos", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(modifier = Modifier.height(12.dp))
        
        if (friends.isEmpty()) {
            Text("No tienes amigos agregados", style = MaterialTheme.typography.bodyMedium)
        } else {
            friends.forEach { (_, data) ->
                val (username, profileImageUrl) = data
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AvatarImage(
                            imageUrl = profileImageUrl,
                            modifier = Modifier.size(40.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop,
                            placeholderTint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = username,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

fun uriToBase64String(context: android.content.Context, uri: android.net.Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val originalBitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
        inputStream?.close()
        if (originalBitmap == null) return null

        val maxDim = 300f
        val scale = Math.min(maxDim / originalBitmap.width, maxDim / originalBitmap.height)
        val scaledBitmap = if (scale < 1f) {
            android.graphics.Bitmap.createScaledBitmap(
                originalBitmap,
                (originalBitmap.width * scale).toInt(),
                (originalBitmap.height * scale).toInt(),
                true
            )
        } else {
            originalBitmap
        }

        val outputStream = java.io.ByteArrayOutputStream()
        scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, outputStream)
        val byteArray = outputStream.toByteArray()
        "data:image/jpeg;base64," + android.util.Base64.encodeToString(byteArray, android.util.Base64.DEFAULT)
    } catch (e: Exception) {
        null
    }
}

@Composable
fun AvatarImage(imageUrl: String?, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Crop, placeholderTint: androidx.compose.ui.graphics.Color) {
    if (imageUrl != null && imageUrl.startsWith("data:image")) {
        val base64String = imageUrl.substringAfter("base64,")
        val imageBitmap = remember(imageUrl) {
            try {
                val imageBytes = android.util.Base64.decode(base64String, android.util.Base64.DEFAULT)
                android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)?.asImageBitmap()
            } catch (e: Exception) { null }
        }
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap,
                contentDescription = "Avatar",
                modifier = modifier,
                contentScale = contentScale
            )
        } else {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Avatar Error",
                modifier = modifier,
                tint = placeholderTint
            )
        }
    } else if (imageUrl != null) {
        coil.compose.AsyncImage(
            model = imageUrl,
            contentDescription = "Avatar",
            modifier = modifier,
            contentScale = contentScale
        )
    } else {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = "Avatar Placeholder",
            modifier = modifier,
            tint = placeholderTint
        )
    }
}
