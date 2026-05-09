package com.example.safebite.utils

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.example.safebite.data.Notification
import com.example.safebite.data.Post
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

object ModerationUtils {

    suspend fun moderatePost(
        context: Context,
        db: FirebaseFirestore,
        post: Post,
        isAdmin: Boolean,
        currentUserId: String?
    ): Boolean {
        return try {
            val authorId = post.authorId
            Log.d("Moderation", "Initiating deletion. isAdmin=$isAdmin, authorId=$authorId")

            if (isAdmin && authorId != currentUserId && authorId.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Moderando publicación de $authorId...", Toast.LENGTH_SHORT).show()
                }

                try {
                    val authorDoc = db.collection("users").document(authorId).get().await()
                    if (authorDoc.exists()) {
                        val warnings = (authorDoc.get("warnings") as? List<Long>)?.toMutableList() ?: mutableListOf()
                        val now = System.currentTimeMillis()
                        warnings.add(now)

                        val weekAgo = now - (7 * 24 * 60 * 60 * 1000L)
                        val recentWarnings = warnings.filter { it >= weekAgo }

                        val updates = mutableMapOf<String, Any>("warnings" to warnings)
                        if (recentWarnings.size >= 3) {
                            updates["bannedUntil"] = now + (3 * 24 * 60 * 60 * 1000L)
                        }

                        db.collection("users").document(authorId).update(updates).await()

                        // Create notification
                        val notifId = UUID.randomUUID().toString()
                        val notification = Notification(
                            id = notifId,
                            toId = authorId,
                            message = "El admin te ha borrado una publicación y tienes una advertencia",
                            type = "warning",
                            timestamp = now,
                            read = false
                        )
                        db.collection("notifications").document(notifId).set(notification).await()

                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Advertencia enviada con éxito", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("Moderation", "Error in warning flow", e)
                }
            }

            // Proceed with deletion
            withContext(Dispatchers.IO) {
                if (post.mediaType == "video" && post.mediaUrl?.startsWith("firestore_video_") == true) {
                    val chunks = db.collection("posts").document(post.id).collection("chunks").get().await()
                    for (doc in chunks) doc.reference.delete().await()
                }
                db.collection("posts").document(post.id).delete().await()
                
                // Also resolve any reports linked to this post
                val reports = db.collection("reports").whereEqualTo("postId", post.id).get().await()
                for (doc in reports) {
                    doc.reference.update("status", "resolved").await()
                }
            }
            true
        } catch (e: Exception) {
            Log.e("Moderation", "Global deletion error", e)
            false
        }
    }

    suspend fun dismissReport(db: FirebaseFirestore, reportId: String): Boolean {
        return try {
            db.collection("reports").document(reportId).update("status", "resolved").await()
            true
        } catch (e: Exception) {
            Log.e("Moderation", "Error dismissing report", e)
            false
        }
    }
}
