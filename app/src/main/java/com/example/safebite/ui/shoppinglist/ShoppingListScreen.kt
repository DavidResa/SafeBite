package com.example.safebite.ui.shoppinglist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.safebite.utils.ScannedProduct
import com.example.safebite.utils.ShoppingListUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(
    onBack: () -> Unit,
    onProductClick: (String) -> Unit
) {
    val db = Firebase.firestore
    val auth = FirebaseAuth.getInstance()
    val scope = rememberCoroutineScope()
    
    var items by remember { mutableStateOf<List<ScannedProduct>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            db.collection("users").document(uid).collection("shopping_list")
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        items = snapshot.documents.mapNotNull { doc ->
                            val barcode = doc.getString("barcode") ?: ""
                            val name = doc.getString("productName") ?: ""
                            val imageUrl = doc.getString("imageUrl")
                            if (barcode.isNotEmpty()) {
                                ScannedProduct(barcode, name, imageUrl)
                            } else null
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
            TopAppBar(
                title = { Text("Lista de la Compra") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (items.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.ShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Tu lista está vacía",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    items(items) { product ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable { onProductClick(product.barcode) },
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = product.imageUrl,
                                    contentDescription = null,
                                    modifier = Modifier.size(60.dp).clip(MaterialTheme.shapes.small),
                                    contentScale = ContentScale.Fit
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = product.productName,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 2
                                    )
                                    Text(
                                        text = product.barcode,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                IconButton(onClick = {
                                    scope.launch {
                                        ShoppingListUtils.removeFromShoppingList(product.barcode)
                                    }
                                }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Eliminar",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
