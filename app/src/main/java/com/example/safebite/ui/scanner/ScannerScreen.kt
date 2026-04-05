package com.example.safebite.ui.scanner

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import com.example.safebite.ui.profile.AvatarImage
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.compose.ui.focus.onFocusChanged
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.filled.Add
import com.example.safebite.data.Post
import com.google.firebase.auth.FirebaseAuth
import java.util.UUID
import com.example.safebite.utils.BarcodeScanner
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    onBarcodeDetected: (String) -> Unit,
    onUserSelected: (String) -> Unit = {}
) {
    UserSearchTab(onUserSelected)
}

@Composable
fun CameraScannerTab(onBarcodeDetected: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )

    LaunchedEffect(key1 = true) {
        launcher.launch(Manifest.permission.CAMERA)
    }

    if (hasCameraPermission) {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { context ->
                    val previewView = PreviewView(context)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build()
                        val selector = CameraSelector.Builder()
                            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                            .build()

                        preview.setSurfaceProvider(previewView.surfaceProvider)

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        imageAnalysis.setAnalyzer(
                            Executors.newSingleThreadExecutor(),
                            BarcodeScanner { barcode ->
                                onBarcodeDetected(barcode)
                            }
                        )

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                selector,
                                preview,
                                imageAnalysis
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(context))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    } else {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Se requiere permiso de cámara para escanear")
        }
    }
}

data class SearchUserResult(val id: String, val username: String, val profileImageUrl: String? = null)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSearchTab(onUserSelected: (String) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<SearchUserResult>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("safebite_search_history", Context.MODE_PRIVATE) }
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "unknown"
    val historyKey = "recent_users_$userId"
    
    var searchHistory by remember(userId) { 
        mutableStateOf<List<SearchUserResult>>(
            sharedPrefs.getString(historyKey, "")?.split(",,")?.mapNotNull {
                val parts = it.split("::")
                if (parts.size >= 2) {
                    val url = if (parts.size > 2 && parts[2].isNotEmpty()) parts[2] else null
                    SearchUserResult(parts[0], parts[1], url)
                } else null
            } ?: emptyList()
        )
    }

    fun addToHistory(user: SearchUserResult) {
        val updatedHistory = listOf(user) + searchHistory.filter { it.id != user.id }
        val limitedHistory = updatedHistory.take(10)
        searchHistory = limitedHistory
        sharedPrefs.edit().putString(historyKey, limitedHistory.joinToString(",,") { "${it.id}::${it.username}::${it.profileImageUrl ?: ""}" }).apply()
    }

    val db = Firebase.firestore

    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotEmpty()) {
            isLoading = true
            try {
                val snapshot = db.collection("users")
                    .whereGreaterThanOrEqualTo("username", searchQuery)
                    .whereLessThanOrEqualTo("username", searchQuery + "\uf8ff")
                    .get()
                    .await()
                    
                val results = snapshot.documents.mapNotNull { doc ->
                    val username = doc.getString("username")
                    val profileImageUrl = doc.getString("profileImageUrl")
                    if (username != null) {
                        SearchUserResult(doc.id, username, profileImageUrl)
                    } else null
                }
                searchResults = results
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        } else {
            searchResults = emptyList()
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isFocused = it.isFocused },
            placeholder = { Text("Buscar usuarios...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (searchResults.isEmpty() && searchQuery.isNotEmpty()) {
            Text("No se encontraron usuarios", style = MaterialTheme.typography.bodyMedium)
        } else if (searchQuery.isNotEmpty()) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(searchResults) { user ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { 
                                addToHistory(user)
                                onUserSelected(user.id) 
                            },
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AvatarImage(
                                imageUrl = user.profileImageUrl,
                                modifier = Modifier.size(40.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop,
                                placeholderTint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(text = user.username, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        } else if (isFocused && searchHistory.isNotEmpty()) {
            Text("Búsquedas recientes", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(searchHistory) { user ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { 
                                addToHistory(user)
                                onUserSelected(user.id) 
                            },
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AvatarImage(
                                imageUrl = user.profileImageUrl,
                                modifier = Modifier.size(24.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop,
                                placeholderTint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = user.username, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        } else if (!isFocused && searchQuery.isEmpty()) {
            Text("Explorar", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), 
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Restaurantes", "Recetas", "Experiencias").forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                        label = { Text(cat) }
                    )
                }
            }
            if (selectedCategory != null) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Box(modifier = Modifier.fillMaxSize().weight(1f, fill=false)) {
                    CategoryFeed(category = selectedCategory!!)
                }
            }
        }
    }
}

@Composable
fun CategoryFeed(category: String) {
    val db = Firebase.firestore
    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showUploadDialog by remember { mutableStateOf(false) }

    DisposableEffect(category) {
        isLoading = true
        posts = emptyList()
        val listener = db.collection("posts")
            .whereEqualTo("category", category)
            .addSnapshotListener { snapshot, e ->
                if (snapshot != null) {
                    posts = snapshot.documents.mapNotNull { it.toObject(Post::class.java) }
                        .sortedByDescending { it.timestamp }
                }
                isLoading = false
            }
        onDispose { listener.remove() }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showUploadDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Subir Contenido")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (posts.isEmpty()) {
                Text("Aún no hay publicaciones en $category", modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(posts) { post ->
                        PostCard(post)
                    }
                }
            }
        }
    }

    if (showUploadDialog) {
        UploadPostDialog(category = category, onDismiss = { showUploadDialog = false })
    }
}

@Composable
fun PostCard(post: Post) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(post.authorUsername, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            if (post.textContent.isNotEmpty()) {
                Text(post.textContent, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (post.mediaUrl != null) {
                if (post.mediaType == "image") {
                    coil.compose.AsyncImage(
                        model = post.mediaUrl,
                        contentDescription = "Post image",
                        modifier = Modifier.fillMaxWidth().height(200.dp)
                    )
                } else if (post.mediaType == "video") {
                    Text("[Video subido - Haz click aquí para verlo en el navegador]", color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadPostDialog(category: String, onDismiss: () -> Unit) {
    var textContent by remember { mutableStateOf("") }
    var selectedMediaUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var isVideo by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val db = Firebase.firestore
    val auth = FirebaseAuth.getInstance()
    
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedMediaUri = uri
            val mimeType = context.contentResolver.getType(uri)
            isVideo = mimeType?.startsWith("video/") == true
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isUploading) onDismiss() },
        title = { Text("Publicar en $category") },
        text = {
            Column {
                OutlinedTextField(
                    value = textContent,
                    onValueChange = { textContent = it },
                    placeholder = { Text("Escribe algo...") },
                    modifier = Modifier.fillMaxWidth().height(100.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { 
                    mediaPickerLauncher.launch(
                        androidx.activity.result.PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageAndVideo
                        )
                    )
                }) {
                    Text(if (selectedMediaUri == null) "Añadir Imagen/Video" else "Medio Seleccionado")
                }
                if (isUploading) {
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text("Subiendo...", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isUploading && (textContent.isNotEmpty() || selectedMediaUri != null),
                onClick = {
                    isUploading = true
                    val userId = auth.currentUser?.uid ?: return@Button
                    
                    db.collection("users").document(userId).get().addOnSuccessListener { doc ->
                        val username = doc.getString("username") ?: "Usuario"
                        val postId = UUID.randomUUID().toString()
                        
                        val uploadPost = { mediaUrl: String?, mediaType: String? ->
                            val post = Post(
                                id = postId,
                                category = category,
                                authorId = userId,
                                authorUsername = username,
                                textContent = textContent,
                                mediaUrl = mediaUrl,
                                mediaType = mediaType,
                                timestamp = System.currentTimeMillis()
                            )
                            db.collection("posts").document(postId).set(post)
                                .addOnSuccessListener {
                                    isUploading = false
                                    Toast.makeText(context, "Publicado correctamente", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                }
                                .addOnFailureListener {
                                    isUploading = false
                                    Toast.makeText(context, "Error al guardar", Toast.LENGTH_SHORT).show()
                                }
                        }
                        
                        if (selectedMediaUri != null) {
                            val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance().reference
                            val fileName = UUID.randomUUID().toString() + if (isVideo) ".mp4" else ".jpg"
                            val fileRef = storageRef.child("posts_media/$fileName")
                            
                            fileRef.putFile(selectedMediaUri!!)
                                .addOnSuccessListener {
                                    fileRef.downloadUrl.addOnSuccessListener { url ->
                                        uploadPost(url.toString(), if (isVideo) "video" else "image")
                                    }.addOnFailureListener {
                                        isUploading = false
                                        Toast.makeText(context, "Error al obtener URL", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .addOnFailureListener { e ->
                                    isUploading = false
                                    Toast.makeText(context, "Error subiendo archivo. Revise Firebase Storage.", Toast.LENGTH_LONG).show()
                                }
                        } else {
                            uploadPost(null, null)
                        }
                    }
                }
            ) {
                Text("Publicar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isUploading) {
                Text("Cancelar")
            }
        }
    )
}
