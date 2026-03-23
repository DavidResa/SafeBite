package com.example.safebite.ui.scanner

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.safebite.data.api.Product
import com.example.safebite.data.api.RetrofitClient
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    barcode: String,
    userAllergens: String,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var product by remember { mutableStateOf<Product?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(barcode) {
        scope.launch {
            try {
                val response = RetrofitClient.api.getProduct(barcode)
                if (response.status == 1) {
                    product = response.product
                } else {
                    error = "Producto no encontrado"
                }
            } catch (e: Exception) {
                error = "Error al conectar con el servidor: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Resultado del Análisis") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("<")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator()
            } else if (error != null) {
                Text(text = error!!)
            } else if (product != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    AsyncImage(
                        model = product!!.imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val verdict = analyzeIngredients(product!!.ingredientsText ?: "", userAllergens)
                    VerdictCard(verdict)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = product!!.productName ?: "Sin nombre", style = MaterialTheme.typography.headlineMedium)
                    Text(text = "Marca: ${product!!.brands ?: "N/A"}", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Ingredientes:", style = MaterialTheme.typography.titleLarge)
                    Text(text = product!!.ingredientsText ?: "No especificados", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

enum class VerdictColor { GREEN, YELLOW, RED }
data class AnalysisVerdict(val color: VerdictColor, val message: String)

@Composable
fun VerdictCard(verdict: AnalysisVerdict) {
    val backgroundColor = when (verdict.color) {
        VerdictColor.GREEN -> Color(0xFFC8E6C9)
        VerdictColor.YELLOW -> Color(0xFFFFF9C4)
        VerdictColor.RED -> Color(0xFFFFCDD2)
    }
    val textColor = when (verdict.color) {
        VerdictColor.GREEN -> Color(0xFF2E7D32)
        VerdictColor.YELLOW -> Color(0xFFFBC02D)
        VerdictColor.RED -> Color(0xFFC62828)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (verdict.color == VerdictColor.GREEN) "¡SEGURO!" else if (verdict.color == VerdictColor.RED) "¡PELIGRO!" else "PRECAUCIÓN",
                style = MaterialTheme.typography.headlineSmall,
                color = textColor
            )
            Text(text = verdict.message, color = textColor)
        }
    }
}

fun analyzeIngredients(ingredientsText: String, userAllergens: String): AnalysisVerdict {
    if (ingredientsText.isEmpty()) return AnalysisVerdict(VerdictColor.YELLOW, "No hay lista de ingredientes disponible para analizar.")
    if (userAllergens.isEmpty()) return AnalysisVerdict(VerdictColor.GREEN, "No tienes alérgenos configurados. ¡Parece seguro!")

    val allergens = userAllergens.split(",").map { it.trim().lowercase() }
    val ingredients = ingredientsText.lowercase()

    val detected = mutableListOf<String>()
    for (allergen in allergens) {
        if (ingredients.contains(allergen)) {
            detected.add(allergen)
        }
    }

    return if (detected.isNotEmpty()) {
        AnalysisVerdict(VerdictColor.RED, "Se han detectado los siguientes alérgenos: ${detected.joinToString(", ")}")
    } else {
        if (ingredients.contains("puede contener") || ingredients.contains("trazas")) {
            AnalysisVerdict(VerdictColor.YELLOW, "Aunque no se detectan tus alérgenos principales, el producto indica que puede contener trazas.")
        } else {
            AnalysisVerdict(VerdictColor.GREEN, "No se han detectado tus alérgenos en la lista de ingredientes.")
        }
    }
}
