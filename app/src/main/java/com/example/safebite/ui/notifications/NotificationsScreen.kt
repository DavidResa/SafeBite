package com.example.safebite.ui.notifications
import android.util.Log

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.alpha
import com.example.safebite.data.FriendRequest
import com.example.safebite.data.Notification
import com.example.safebite.ui.profile.AvatarImage
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import com.example.safebite.data.Report
import com.example.safebite.data.Post
import com.example.safebite.utils.ModerationUtils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onNavigateToUser: (String) -> Unit
) {
    val db = Firebase.firestore
    val auth = Firebase.auth
    val currentUserId = auth.currentUser?.uid
    
    var requests by remember { mutableStateOf<List<FriendRequest>>(emptyList()) }
    var notifications by remember { mutableStateOf<List<Notification>>(emptyList()) }
    var reports by remember { mutableStateOf<List<Report>>(emptyList()) }
    var isAdmin by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var postToView by remember { mutableStateOf<Post?>(null) }
    var showPostDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Notificaciones", "Reportes")

    LaunchedEffect(currentUserId) {
        if (currentUserId != null) {
            // Fetch Friend Requests
            db.collection("friend_requests")
                .whereEqualTo("toId", currentUserId)
                .whereEqualTo("status", "pending")
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        requests = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(FriendRequest::class.java)?.copy(id = doc.id)
                        }
                    }
                }

            // Fetch Warning Notifications
            db.collection("notifications")
                .whereEqualTo("toId", currentUserId)
                .addSnapshotListener { snapshot, _ ->
                    Log.d("NotificationsScreen", "Fetched notifications snapshot for $currentUserId. Empty: ${snapshot?.isEmpty}")
                    if (snapshot != null) {
                        notifications = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(Notification::class.java)?.copy(id = doc.id)
                        }.sortedByDescending { it.timestamp }
                        Log.d("NotificationsScreen", "Parsed ${notifications.size} notifications")
                        
                        // Mark unread as read
                        snapshot.documents.filter { doc -> doc.getBoolean("read") == false }.forEach { doc ->
                            db.collection("notifications").document(doc.id).update("read", true)
                        }
                    }
                }

            // Fetch User Info (isAdmin)
            db.collection("users").document(currentUserId).addSnapshotListener { doc, _ ->
                isAdmin = doc?.getBoolean("isAdmin") ?: false
                if (isAdmin) {
                    // Fetch Reports
                    db.collection("reports")
                        .whereEqualTo("status", "pending")
                        .addSnapshotListener { snapshot, _ ->
                            if (snapshot != null) {
                                reports = snapshot.documents.mapNotNull { d ->
                                    d.toObject(Report::class.java)?.copy(id = d.id)
                                }.sortedByDescending { it.timestamp }
                            }
                        }
                }
                isLoading = false
            }
        } else {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = { Text("Notificaciones") }
                )
                if (isAdmin) {
                    TabRow(selectedTabIndex = selectedTabIndex) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = { selectedTabIndex = index },
                                text = { Text(title) }
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                val showNotifications = !isAdmin || selectedTabIndex == 0
                val showReports = isAdmin && selectedTabIndex == 1
                
                val isCurrentTabEmpty = if (showNotifications) {
                    requests.isEmpty() && notifications.isEmpty()
                } else {
                    reports.isEmpty()
                }

                if (isCurrentTabEmpty) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            if (showNotifications) "No tienes notificaciones" else "No hay reportes",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        // Requests & Notifications Section
                        if (showNotifications) {
                            items(requests) { request ->
                                FriendRequestItem(
                                    request = request,
                                    onAccept = {
                                        val batch = db.batch()
                                        batch.update(db.collection("users").document(currentUserId!!), "friends", FieldValue.arrayUnion(request.fromId))
                                        batch.update(db.collection("users").document(request.fromId), "friends", FieldValue.arrayUnion(currentUserId))
                                        batch.delete(db.collection("friend_requests").document(request.id))
                                        batch.commit()
                                    },
                                    onReject = {
                                        db.collection("friend_requests").document(request.id).delete()
                                    },
                                    onClick = { onNavigateToUser(request.fromId) }
                                )
                            }
                            
                            items(notifications) { notification ->
                                WarningNotificationItem(
                                    notification = notification,
                                    onDelete = {
                                        db.collection("notifications").document(notification.id).delete()
                                            .addOnSuccessListener {
                                                Toast.makeText(context, "Notificación eliminada", Toast.LENGTH_SHORT).show()
                                            }
                                    }
                                )
                            }
                        }

                        // Reports Section (Admin only)
                        if (showReports) {
                            items(reports) { report ->
                                ReportItem(
                                    report = report,
                                    onModerate = {
                                        scope.launch {
                                            val postDoc = db.collection("posts").document(report.postId).get().await()
                                            val post = postDoc.toObject(Post::class.java)
                                            if (post != null) {
                                                val success = ModerationUtils.moderatePost(context, db, post, true, currentUserId)
                                                if (success) {
                                                    Toast.makeText(context, "Publicación eliminada y reporte resuelto", Toast.LENGTH_SHORT).show()
                                                }
                                            } else {
                                                // Post already deleted? Just dismiss report
                                                ModerationUtils.dismissReport(db, report.id)
                                                Toast.makeText(context, "La publicación ya no existe. Reporte cerrado.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    onDismiss = {
                                        scope.launch {
                                            ModerationUtils.dismissReport(db, report.id)
                                            Toast.makeText(context, "Reporte desestimado", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onViewPost = {
                                        scope.launch {
                                            try {
                                                val postDoc = db.collection("posts").document(report.postId).get().await()
                                                val post = postDoc.toObject(Post::class.java)
                                                if (post != null) {
                                                    postToView = post
                                                    showPostDialog = true
                                                } else {
                                                    Toast.makeText(context, "La publicación ya no existe", Toast.LENGTH_SHORT).show()
                                                }
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Error al cargar la publicación", Toast.LENGTH_SHORT).show()
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
        
        if (showPostDialog && postToView != null) {
            AlertDialog(
                onDismissRequest = { showPostDialog = false },
                title = { Text("Publicación Reportada") },
                text = {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        com.example.safebite.ui.scanner.PostCard(
                            post = postToView!!,
                            currentUserId = currentUserId,
                            isAdmin = isAdmin
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showPostDialog = false }) {
                        Text("Cerrar")
                    }
                }
            )
        }
    }
}

@Composable
fun FriendRequestItem(
    request: FriendRequest,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarImage(
                    imageUrl = request.fromProfileImageUrl,
                    modifier = Modifier.size(48.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    placeholderTint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = request.fromUsername,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Quiere ser tu amigo",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onReject) {
                    Text("Rechazar")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onAccept) {
                    Text("Aceptar")
                }
            }
        }
    }
}
@Composable
fun WarningNotificationItem(
    notification: Notification,
    onDelete: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val dateString = sdf.format(Date(notification.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.type == "warning") MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f) 
                             else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (notification.type == "warning") "⚠️" else "ℹ️",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Aviso Administrativo",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (notification.type == "warning") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = dateString,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Borrar notificación",
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
@Composable
fun ReportItem(
    report: Report,
    onModerate: () -> Unit,
    onDismiss: () -> Unit,
    onViewPost: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🚩", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Reporte de Contenido",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "Motivo: ${report.reason}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(modifier = Modifier.alpha(0.1f))
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Contenido del post:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "\"${report.postText}\"",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth(),
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Ignorar")
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onViewPost) {
                    Text("Ver Post")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onModerate,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Borrar Post")
                }
            }
        }
    }
}
