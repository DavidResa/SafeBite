package com.example.safebite.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.google.firebase.firestore.SetOptions
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
    var showFriendsDialog by remember { mutableStateOf(false) }
    var showFavoritesDialog by remember { mutableStateOf(false) }

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
                    favoriteFoods = snapshot.getString("favoriteFoods") ?: "",
                    friends = friendsList,
                    profileImageUrl = snapshot.getString("profileImageUrl"),
                    isPrivate = snapshot.getBoolean("isPrivate") ?: false
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

    Box(modifier = Modifier.fillMaxSize()) {
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
                    var showChangeUsernameDialog by remember { mutableStateOf(false) }
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
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Cuenta Privada")
                                        Switch(
                                            checked = userProfile?.isPrivate ?: false,
                                            onCheckedChange = { newValue ->
                                                if (userId != null) {
                                                    dbInst.collection("users").document(userId)
                                                        .update("isPrivate", newValue)
                                                }
                                            }
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = { 
                                            showSettingsDialog = false
                                            showChangeUsernameDialog = true
                                        }, 
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                    ) {
                                        Text("Cambiar Nombre de Perfil")
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
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

                    if (showChangeUsernameDialog) {
                        var newName by remember { mutableStateOf(userProfile?.username ?: "") }
                        var isChecking by remember { mutableStateOf(false) }
                        var errorMsg by remember { mutableStateOf<String?>(null) }
                        val coroutineScope = rememberCoroutineScope()

                        AlertDialog(
                            onDismissRequest = { if (!isChecking) showChangeUsernameDialog = false },
                            title = { Text("Cambiar Nombre de Perfil") },
                            text = {
                                Column {
                                    Text("Introduce tu nuevo nombre de usuario:")
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = newName,
                                        onValueChange = { newName = it; errorMsg = null },
                                        modifier = Modifier.fillMaxWidth(),
                                        placeholder = { Text("Nuevo nombre") },
                                        singleLine = true,
                                        isError = errorMsg != null
                                    )
                                    if (errorMsg != null) {
                                        Text(
                                            text = errorMsg!!,
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                    if (isChecking) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                    }
                                }
                            },
                            confirmButton = {
                                Button(
                                    enabled = !isChecking && newName.isNotBlank() && newName != userProfile?.username,
                                    onClick = {
                                        isChecking = true
                                        coroutineScope.launch {
                                            try {
                                                val oldName = userProfile?.username ?: ""
                                                val email = userProfile?.email ?: ""
                                                
                                                // 1. Check uniqueness
                                                val checkDoc = dbInst.collection("usernames").document(newName).get().await()
                                                if (checkDoc.exists()) {
                                                    errorMsg = "El nombre de usuario ya está en uso"
                                                    isChecking = false
                                                } else {
                                                    // 2. Perform updates
                                                    // Note: Ideally use a transaction, but let's follow the established pattern
                                                    if (oldName.isNotEmpty()) {
                                                        dbInst.collection("usernames").document(oldName).delete().await()
                                                    }
                                                    dbInst.collection("usernames").document(newName).set(hashMapOf("email" to email)).await()
                                                    dbInst.collection("users").document(userId!!).update("username", newName).await()
                                                    
                                                    withContext(Dispatchers.Main) {
                                                        android.widget.Toast.makeText(currentContext, "Nombre actualizado con éxito", android.widget.Toast.LENGTH_SHORT).show()
                                                        showChangeUsernameDialog = false
                                                        isChecking = false
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                errorMsg = "Error: ${e.localizedMessage}"
                                                isChecking = false
                                            }
                                        }
                                    }
                                ) {
                                    Text("Guardar")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showChangeUsernameDialog = false }, enabled = !isChecking) {
                                    Text("Cancelar")
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
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ProfileSquareCard(
                            modifier = Modifier.weight(1f),
                            title = "Comidas Favoritas",
                            icon = Icons.Default.Favorite,
                            iconTint = MaterialTheme.colorScheme.tertiary,
                            onClick = { showFavoritesDialog = true }
                        )
                        ProfileSquareCard(
                            modifier = Modifier.weight(1f),
                            title = "Mis Amigos",
                            icon = Icons.Default.Person,
                            iconTint = MaterialTheme.colorScheme.primary,
                            onClick = { showFriendsDialog = true }
                        )
                    }
                }
            }
        }
        }
        
        // Overlays
        ZoomOverlayDialog(
            visible = showFriendsDialog,
            onDismiss = { showFriendsDialog = false },
            title = "Mis Amigos",
            icon = Icons.Default.Person,
            iconTint = MaterialTheme.colorScheme.primary
        ) {
            if (friendsMap.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No tienes amigos agregados", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(friendsMap.toList()) { (_, data) ->
                        val (username, profileImageUrl) = data
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AvatarImage(
                                imageUrl = profileImageUrl,
                                modifier = Modifier.size(48.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop,
                                placeholderTint = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = username,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
        }

        ZoomOverlayDialog(
            visible = showFavoritesDialog,
            onDismiss = { showFavoritesDialog = false },
            title = "Comidas Favoritas",
            icon = Icons.Default.Favorite,
            iconTint = MaterialTheme.colorScheme.tertiary
        ) {
            val foods = userProfile?.favoriteFoods ?: ""
            Column(modifier = Modifier.fillMaxSize()) {
                if (foods.isBlank()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No tienes comidas favoritas", style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        items(foods.split(",").filter { it.isNotBlank() }) { food ->
                            val currentFood = food.trim()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "• $currentFood",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                IconButton(
                                    onClick = {
                                        if (userId != null) {
                                            val foodList = foods.split(",").filter { it.isNotBlank() }.map { it.trim() }
                                            val newFoodList = foodList.filter { it != currentFood }
                                            val updatedFoods = newFoodList.joinToString(", ")
                                            db.collection("users").document(userId)
                                                .set(hashMapOf("favoriteFoods" to updatedFoods), SetOptions.merge())
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Borrar",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
                
                HorizontalDivider()
                var newFood by remember { mutableStateOf("") }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newFood,
                        onValueChange = { newFood = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Añadir comida...") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (newFood.isNotBlank() && userId != null) {
                                val updatedFoods = if (foods.isBlank()) newFood.trim() else "$foods, ${newFood.trim()}"
                                db.collection("users").document(userId)
                                    .set(hashMapOf("favoriteFoods" to updatedFoods), SetOptions.merge())
                                    .addOnSuccessListener {
                                        newFood = ""
                                    }
                                    .addOnFailureListener { e ->
                                        // Ignore silently for now, or print, but since context isn't fetched, let's keep it simple
                                    }
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Añadir", tint = MaterialTheme.colorScheme.onPrimaryContainer)
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
fun ProfileSquareCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ZoomOverlayDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    title: String,
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300)),
        exit = fadeOut(tween(300))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f))
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = scaleIn(tween(300), initialScale = 0.8f) + fadeIn(tween(300)),
                exit = scaleOut(tween(300), targetScale = 0.8f) + fadeOut(tween(300))
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .fillMaxHeight(0.6f)
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null,
                            onClick = {} // Consume click
                        ),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = icon, contentDescription = null, tint = iconTint)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(onClick = onDismiss) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar")
                            }
                        }
                        HorizontalDivider()
                        Box(modifier = Modifier.weight(1f)) {
                            content()
                        }
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
