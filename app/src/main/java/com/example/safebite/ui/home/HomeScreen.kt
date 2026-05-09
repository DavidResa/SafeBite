package com.example.safebite.ui.home

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.safebite.R
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Close
import com.example.safebite.utils.HistoryUtils
import com.example.safebite.utils.ScannedProduct
import com.example.safebite.utils.ShoppingListUtils
import com.example.safebite.ui.profile.ZoomOverlayDialog
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.material.icons.filled.Refresh

@Composable
fun HomeScreen(
    userName: String,
    onNavigateToScanner: () -> Unit,
    onSearchBarcode: (String) -> Unit,
    onNavigateToShoppingList: () -> Unit
) {
    var manualBarcode by remember { mutableStateOf("") }
    var showHistoryDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val image: InputImage
            try {
                image = InputImage.fromFilePath(context, it)
                val scanner = BarcodeScanning.getClient()
                scanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        if (barcodes.isNotEmpty()) {
                            val barcodeValue = barcodes[0].rawValue
                            if (barcodeValue != null) {
                                onSearchBarcode(barcodeValue)
                            }
                        } else {
                            Toast.makeText(context, "No se encontró ningún código de barras en la imagen", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Error al procesar la imagen", Toast.LENGTH_SHORT).show()
                    }
            } catch (e: Exception) {
                Toast.makeText(context, "Error al cargar la imagen", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // History Button (Option 1)
            IconButton(
                onClick = { showHistoryDialog = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Historial de escaneos",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

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
                    .height(100.dp)
                    .padding(bottom = 24.dp)
            )
            
            Text(
                text = "Hola, $userName",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.tertiary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Tu asistente inteligente para comer con seguridad",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Scanning Options Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = onNavigateToScanner,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(text = "Cámara")
                }
                
                Button(
                    onClick = { launcher.launch("image/*") },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text(text = "Galería")
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Manual Entry Option
            Text(
                text = "o introduce el código manualmente:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = manualBarcode,
                onValueChange = { if (it.all { char -> char.isDigit() }) manualBarcode = it },
                label = { Text("Código de barras") },
                placeholder = { Text("Ej: 8410010203040") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { if (manualBarcode.isNotEmpty()) onSearchBarcode(manualBarcode) },
                enabled = manualBarcode.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary
                )
            ) {
                Text(text = "Consultar producto")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onNavigateToShoppingList,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text(text = "Lista de la compra")
            }
        }

        // History Dialog
        ZoomOverlayDialog(
            visible = showHistoryDialog,
            onDismiss = { showHistoryDialog = false },
            title = "Historial de Escaneos",
            icon = Icons.Default.Refresh,
            iconTint = MaterialTheme.colorScheme.primary
        ) {
            val history = remember(showHistoryDialog) { HistoryUtils.getHistory(context) }
            if (history.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay productos escaneados recientemente", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                    items(history) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    showHistoryDialog = false
                                    onSearchBarcode(item.barcode)
                                },
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = item.imageUrl,
                                    contentDescription = null,
                                    modifier = Modifier.size(50.dp).clip(MaterialTheme.shapes.small),
                                    contentScale = ContentScale.Fit
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = item.productName, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                                    Text(text = item.barcode, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                }
                                
                                IconButton(onClick = {
                                    scope.launch {
                                        if (ShoppingListUtils.isInShoppingList(item.barcode)) {
                                            Toast.makeText(context, "${item.productName} ya está en la lista", Toast.LENGTH_SHORT).show()
                                        } else {
                                            ShoppingListUtils.addToShoppingList(item)
                                            Toast.makeText(context, "${item.productName} añadido", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingCart,
                                        contentDescription = "Añadir a la lista",
                                        tint = MaterialTheme.colorScheme.primary
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
}
