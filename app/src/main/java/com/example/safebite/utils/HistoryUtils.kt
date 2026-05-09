package com.example.safebite.utils

import android.content.Context
import com.example.safebite.data.api.Product
import com.google.firebase.auth.FirebaseAuth

data class ScannedProduct(
    val barcode: String,
    val productName: String,
    val imageUrl: String?
)

object HistoryUtils {
    private const val PREFS_NAME = "safebite_product_history"
    private const val HISTORY_KEY_PREFIX = "recent_products_"

    private fun getPrefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    private fun getHistoryKey(): String {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "unknown"
        return "$HISTORY_KEY_PREFIX$userId"
    }

    fun addToHistory(context: Context, barcode: String, product: Product) {
        val prefs = getPrefs(context)
        val historyKey = getHistoryKey()
        val currentHistory = getHistory(context).toMutableList()
        
        // Remove if existing to put it first
        currentHistory.removeAll { it.barcode == barcode }
        
        val newItem = ScannedProduct(
            barcode = barcode,
            productName = product.productName ?: "Producto desconocido",
            imageUrl = product.imageUrl
        )
        
        currentHistory.add(0, newItem)
        val limitedHistory = currentHistory.take(20)
        
        val serialized = limitedHistory.joinToString(";;") { 
            "${it.barcode}||${it.productName}||${it.imageUrl ?: ""}"
        }
        
        prefs.edit().putString(historyKey, serialized).apply()
    }

    fun getHistory(context: Context): List<ScannedProduct> {
        val prefs = getPrefs(context)
        val historyKey = getHistoryKey()
        val serialized = prefs.getString(historyKey, "") ?: ""
        
        if (serialized.isEmpty()) return emptyList()
        
        return serialized.split(";;").mapNotNull { entry ->
            val parts = entry.split("||")
            if (parts.size >= 2) {
                ScannedProduct(
                    barcode = parts[0],
                    productName = parts[1],
                    imageUrl = if (parts.size > 2 && parts[2].isNotEmpty()) parts[2] else null
                )
            } else null
        }
    }
    
    fun clearHistory(context: Context) {
        getPrefs(context).edit().remove(getHistoryKey()).apply()
    }
}
