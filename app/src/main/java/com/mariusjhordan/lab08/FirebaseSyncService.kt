package com.mariusjhordan.lab08

import android.util.Log
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseSyncService(
    private val providedFirestore: FirebaseFirestore? = null
) {

    companion object {
        private const val TAG = "FirebaseSyncService"
    }

    private val firestore: FirebaseFirestore? by lazy {
        providedFirestore ?: try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "Firebase Firestore no pudo ser inicializado", e)
            null
        }
    }

    private val tasksCollection: CollectionReference?
        get() = firestore?.collection("tasks")

    suspend fun uploadTask(task: Task) {
        val collection = tasksCollection
        if (collection == null) {
            Log.w(TAG, "Firebase Firestore no disponible. Omitiendo uploadTask para la tarea ${task.id}")
            return
        }
        try {
            val taskMap = hashMapOf(
                "id" to task.id,
                "description" to task.description,
                "isCompleted" to task.isCompleted
            )
            // Usamos el id de la tarea local como ID del documento en Firestore
            collection.document(task.id.toString()).set(taskMap).await()
            Log.d(TAG, "Tarea subida/actualizada exitosamente: ${task.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Error al subir tarea ${task.id} a Firestore", e)
        }
    }

    suspend fun updateTask(task: Task) {
        uploadTask(task)
    }

    suspend fun deleteTask(task: Task) {
        val collection = tasksCollection
        if (collection == null) {
            Log.w(TAG, "Firebase Firestore no disponible. Omitiendo deleteTask para la tarea ${task.id}")
            return
        }
        try {
            collection.document(task.id.toString()).delete().await()
            Log.d(TAG, "Tarea eliminada exitosamente en Firestore: ${task.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Error al eliminar tarea ${task.id} en Firestore", e)
        }
    }

    suspend fun deleteAllTasks(tasks: List<Task>) {
        val db = firestore
        val collection = tasksCollection
        if (db == null || collection == null) {
            Log.w(TAG, "Firebase Firestore no disponible. Omitiendo deleteAllTasks")
            return
        }
        try {
            val batch = db.batch()
            tasks.forEach { task ->
                val docRef = collection.document(task.id.toString())
                batch.delete(docRef)
            }
            batch.commit().await()
            Log.d(TAG, "Todas las tareas eliminadas de Firestore")
        } catch (e: Exception) {
            Log.e(TAG, "Error al eliminar todas las tareas en Firestore", e)
        }
    }

    suspend fun syncLocalTasksToCloud(tasks: List<Task>) {
        val db = firestore
        val collection = tasksCollection
        if (db == null || collection == null) {
            Log.w(TAG, "Firebase Firestore no disponible. Omitiendo syncLocalTasksToCloud")
            return
        }
        try {
            val batch = db.batch()
            tasks.forEach { task ->
                val docRef = collection.document(task.id.toString())
                val taskMap = hashMapOf(
                    "id" to task.id,
                    "description" to task.description,
                    "isCompleted" to task.isCompleted
                )
                batch.set(docRef, taskMap)
            }
            batch.commit().await()
            Log.d(TAG, "Sincronización masiva con Firestore completada")
        } catch (e: Exception) {
            Log.e(TAG, "Error en la sincronización de tareas con Firestore", e)
        }
    }
}
