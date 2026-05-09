package com.example.safebite.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

object ShoppingListUtils {
    private val db = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()

    private fun getShoppingListCollection() = auth.currentUser?.uid?.let { uid ->
        db.collection("users").document(uid).collection("shopping_list")
    }

    suspend fun addToShoppingList(product: ScannedProduct) {
        val collection = getShoppingListCollection() ?: return
        collection.document(product.barcode).set(product).await()
    }

    suspend fun removeFromShoppingList(barcode: String) {
        val collection = getShoppingListCollection() ?: return
        collection.document(barcode).delete().await()
    }

    suspend fun isInShoppingList(barcode: String): Boolean {
        val collection = getShoppingListCollection() ?: return false
        val doc = collection.document(barcode).get().await()
        return doc.exists()
    }
}
