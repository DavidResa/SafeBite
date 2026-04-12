package com.example.safebite.utils

import android.content.Context
import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import com.example.safebite.ui.profile.uriToBase64String

object MediaUtils {
    
    // Para imágenes reutilizamos la lógica de compresión base64 original
    fun processImageUriToBase64(context: Context, uri: Uri): String? {
        return uriToBase64String(context, uri)
    }

    // Para videos fragmentamos los bytes y los subimos en trozos menores a 1MB a colecciones ocultas
    suspend fun processAndUploadVideoChunks(context: Context, uri: Uri, postId: String): Int = withContext(Dispatchers.IO) {
        val db = FirebaseFirestore.getInstance()
        val inputStream = context.contentResolver.openInputStream(uri) 
            ?: throw Exception("No se pudo leer el archivo")
        
        val bufferSize = 800 * 1024 // 800 KB (debajo del límite rígido de 1MB de firestore)
        val buffer = ByteArray(bufferSize)
        var chunkIndex = 0
        var bytesRead = inputStream.read(buffer)
        
        while (bytesRead != -1) {
            val chunkData = if (bytesRead == bufferSize) buffer else buffer.copyOf(bytesRead)
            
            val chunk = hashMapOf(
                "index" to chunkIndex,
                "data" to com.google.firebase.firestore.Blob.fromBytes(chunkData)
            )
            
            try {
                db.collection("posts")
                    .document(postId)
                    .collection("chunks")
                    .document("chunk_$chunkIndex")
                    .set(chunk)
                    .await()
            } catch (e: Exception) {
                inputStream.close()
                throw Exception("Error subiendo el fragmento del video: ${e.message}")
            }
            
            chunkIndex++
            bytesRead = inputStream.read(buffer)
        }
        
        inputStream.close()
        return@withContext chunkIndex
    }
}
