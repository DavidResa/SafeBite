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
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Close
import com.example.safebite.ui.profile.ProfileSquareCard
import com.example.safebite.ui.profile.ZoomOverlayDialog
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDetailScreen(
    targetUserId: String,
    onBack: () -> Unit
) {
    var username by remember { mutableStateOf<String?>(null) }
    var allergens by remember { mutableStateOf<String?>(null) }
    var profileImageUrl by remember { mutableStateOf<String?>(null) }
    var favoriteFoods by remember { mutableStateOf<String?>(null) }
    var friendsData by remember { mutableStateOf<List<Pair<String, String?>>>(emptyList()) }
    var showFriendsDialog by remember { mutableStateOf(false) }
    var showFavoritesDialog by remember { mutableStateOf(false) }
    var isPrivate by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var isFriend by remember { mutableStateOf(false) }
    var pendingRequestFromMe by remember { mutableStateOf(false) }
    var pendingRequestToMe by remember { mutableStateOf(false) }
    var actionLoading by remember { mutableStateOf(false) }
    var currentUserName by remember { mutableStateOf<String?>(null) }
    var currentUserProfileUrl by remember { mutableStateOf<String?>(null) }
    var isCurrentUserAdmin by remember { mutableStateOf(false) }
    var targetUserBannedUntil by remember { mutableStateOf<Long?>(null) }

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
                favoriteFoods = doc.getString("favoriteFoods")
                isPrivate = doc.getBoolean("isPrivate") ?: false
                targetUserBannedUntil = doc.getLong("bannedUntil")
                
                val fList = doc.get("friends") as? List<String> ?: emptyList()
                val fData = mutableListOf<Pair<String, String?>>()
                for (fid in fList) {
                    try {
                        val fDoc = db.collection("users").document(fid).get().await()
                        val uName = fDoc.getString("username")
                        val pUrl = fDoc.getString("profileImageUrl")
                        if (uName != null) {
                            fData.add(Pair(uName, pUrl))
                        }
                    } catch (e: Exception) {}
                }
                friendsData = fData
            }

            // Check friendship and pending requests
            if (currentUserId != null) {
                val currentUserDoc = db.collection("users").document(currentUserId).get().await()
                currentUserName = currentUserDoc.getString("username")
                currentUserProfileUrl = currentUserDoc.getString("profileImageUrl")
                isCurrentUserAdmin = currentUserDoc.getBoolean("isAdmin") ?: false
                
                val friendsList = currentUserDoc.get("friends") as? List<String>
                isFriend = friendsList?.contains(targetUserId) == true
                
                if (!isFriend) {
                    // Check if I sent a request
                    val sentReq = db.collection("friend_requests")
                        .whereEqualTo("fromId", currentUserId)
                        .whereEqualTo("toId", targetUserId)
                        .whereEqualTo("status", "pending")
                        .get().await()
                    pendingRequestFromMe = !sentReq.isEmpty
                    
                    // Check if they sent a request
                    val receivedReq = db.collection("friend_requests")
                        .whereEqualTo("fromId", targetUserId)
                        .whereEqualTo("toId", currentUserId)
                        .whereEqualTo("status", "pending")
                        .get().await()
                    pendingRequestToMe = !receivedReq.isEmpty
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                        .verticalScroll(rememberScrollState())
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

                    if (isPrivate && !isFriend && currentUserId != targetUserId && !isCurrentUserAdmin) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("🔒", style = MaterialTheme.typography.displayMedium)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "Esta cuenta es privada",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Hazte amigo de este usuario para ver su información.",
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
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

                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
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

                    if (currentUserId != null && currentUserId != targetUserId) {
                        Button(
                            onClick = {
                                actionLoading = true
                                val scope = kotlinx.coroutines.MainScope()
                                scope.launch {
                                    try {
                                        if (isFriend) {
                                            db.collection("users").document(currentUserId)
                                                .update("friends", FieldValue.arrayRemove(targetUserId)).await()
                                            db.collection("users").document(targetUserId)
                                                .update("friends", FieldValue.arrayRemove(currentUserId)).await()
                                            isFriend = false
                                        } else if (pendingRequestToMe) {
                                            // Accept functionality or move to notifications? 
                                            // For simplicity, let's just accept here too
                                            val batch = db.batch()
                                            batch.update(db.collection("users").document(currentUserId), "friends", FieldValue.arrayUnion(targetUserId))
                                            batch.update(db.collection("users").document(targetUserId), "friends", FieldValue.arrayUnion(currentUserId))
                                            val reqSnap = db.collection("friend_requests")
                                                .whereEqualTo("fromId", targetUserId)
                                                .whereEqualTo("toId", currentUserId)
                                                .get().await()
                                            for (d in reqSnap.documents) batch.delete(d.reference)
                                            batch.commit().await()
                                            isFriend = true
                                            pendingRequestToMe = false
                                        } else if (!pendingRequestFromMe) {
                                            val request = com.example.safebite.data.FriendRequest(
                                                fromId = currentUserId,
                                                fromUsername = currentUserName ?: "Usuario",
                                                fromProfileImageUrl = currentUserProfileUrl,
                                                toId = targetUserId,
                                                status = "pending"
                                            )
                                            db.collection("friend_requests").add(request).await()
                                            pendingRequestFromMe = true
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    } finally {
                                        actionLoading = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !actionLoading && !pendingRequestFromMe,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = when {
                                    isFriend -> MaterialTheme.colorScheme.error
                                    pendingRequestFromMe -> MaterialTheme.colorScheme.secondary
                                    else -> MaterialTheme.colorScheme.primary
                                }
                            )
                        ) {
                            if (actionLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                val buttonText = when {
                                    isFriend -> "Eliminar Amigo"
                                    pendingRequestFromMe -> "Solicitud enviada"
                                    pendingRequestToMe -> "Aceptar Solicitud"
                                    else -> "Agregar Amigo"
                                }
                                Text(buttonText)
                            }
                        }
                    }

                    // Admin Actions Section
                    if (isCurrentUserAdmin && currentUserId != targetUserId) {
                        Spacer(modifier = Modifier.height(32.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Acciones de Administrador", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    actionLoading = true
                                    val banUntil = System.currentTimeMillis() + (3 * 24 * 60 * 60 * 1000L)
                                    db.collection("users").document(targetUserId).update("bannedUntil", banUntil)
                                        .addOnSuccessListener { 
                                            targetUserBannedUntil = banUntil
                                            actionLoading = false 
                                        }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                                enabled = !actionLoading && (targetUserBannedUntil == null || targetUserBannedUntil!! < System.currentTimeMillis())
                            ) {
                                Text("Banear 3d", maxLines = 1)
                            }

                            Button(
                                onClick = {
                                    actionLoading = true
                                    db.collection("users").document(targetUserId).update("bannedUntil", null)
                                        .addOnSuccessListener { 
                                            targetUserBannedUntil = null
                                            actionLoading = false 
                                        }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                                enabled = !actionLoading && targetUserBannedUntil != null && targetUserBannedUntil!! > System.currentTimeMillis()
                            ) {
                                Text("Quitar Ban", maxLines = 1)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                actionLoading = true
                                val scope = kotlinx.coroutines.MainScope()
                                scope.launch {
                                    try {
                                        // Delete from usernames first
                                        if (username != null) {
                                            db.collection("usernames").document(username!!).delete().await()
                                        }
                                        // Delete from users
                                        db.collection("users").document(targetUserId).delete().await()
                                        // Delete friend requests
                                        val reqs = db.collection("friend_requests")
                                            .whereEqualTo("fromId", targetUserId).get().await()
                                        for (d in reqs) d.reference.delete().await()
                                        val reqsTo = db.collection("friend_requests")
                                            .whereEqualTo("toId", targetUserId).get().await()
                                        for (d in reqsTo) d.reference.delete().await()
                                        
                                        onBack()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    } finally {
                                        actionLoading = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            enabled = !actionLoading
                        ) {
                            Text("Borrar Usuario Completamente")
                        }
                    }
                }
            }
        }
    }
        
        // After Scaffold
        ZoomOverlayDialog(
            visible = showFriendsDialog,
            onDismiss = { showFriendsDialog = false },
            title = "Amigos (${friendsData.size})",
            icon = Icons.Default.Person,
            iconTint = MaterialTheme.colorScheme.primary
        ) {
            if (friendsData.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Todavía no tiene amigos", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(friendsData) { data ->
                        val (uname, pUrl) = data
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AvatarImage(
                                imageUrl = pUrl,
                                modifier = Modifier.size(48.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop,
                                placeholderTint = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = uname,
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
            val foods = favoriteFoods ?: ""
            if (foods.isBlank()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Ninguna comida favorita registrada", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(foods.split(",").filter { it.isNotBlank() }) { food ->
                        Text(
                            text = "• ${food.trim()}",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}
