package com.mariusjhordan.lab08

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class TaskFilter {
    ALL,
    PENDING,
    COMPLETED
}

enum class TaskSortOrder {
    DATE_DESC,    // Más recientes
    DATE_ASC,     // Más antiguas
    NAME_ASC,     // Nombre A-Z
    NAME_DESC,    // Nombre Z-A
    STATUS_PENDING_FIRST, // Pendientes primero
    STATUS_COMPLETED_FIRST // Completadas primero
}

class TaskViewModel(
    application: Application,
    private val taskDao: TaskDao,
    private val syncService: FirebaseSyncService = FirebaseSyncService()
) : AndroidViewModel(application) {

    // Constructor secundario para compatibilidad directa pasando solo taskDao
    constructor(taskDao: TaskDao) : this(
        application = getApplicationInstance(),
        taskDao = taskDao,
        syncService = FirebaseSyncService()
    )

    // Estado base de la lista de tareas obtenidas de Room
    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks

    // Estado para búsqueda
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    // Estado para filtro actual (Todas, Pendientes, Completadas)
    private val _selectedFilter = MutableStateFlow(TaskFilter.ALL)
    val selectedFilter: StateFlow<TaskFilter> = _selectedFilter

    // Estado para ordenamiento
    private val _sortOrder = MutableStateFlow(TaskSortOrder.DATE_DESC)
    val sortOrder: StateFlow<TaskSortOrder> = _sortOrder

    // Lista procesada (filtrada, buscada y ordenada) expuesta a la UI
    val visibleTasks: StateFlow<List<Task>> = combine(
        _tasks,
        _searchQuery,
        _selectedFilter,
        _sortOrder
    ) { allTasks, query, filter, sort ->
        // 1. Filtrar por estado
        val filteredByStatus = when (filter) {
            TaskFilter.ALL -> allTasks
            TaskFilter.PENDING -> allTasks.filter { !it.isCompleted }
            TaskFilter.COMPLETED -> allTasks.filter { it.isCompleted }
        }

        // 2. Filtrar por término de búsqueda (título o descripción)
        val filteredBySearch = if (query.isBlank()) {
            filteredByStatus
        } else {
            val trimmed = query.trim().lowercase()
            filteredByStatus.filter { task ->
                task.description.lowercase().contains(trimmed)
            }
        }

        // 3. Ordenar tareas
        when (sort) {
            TaskSortOrder.DATE_DESC -> filteredBySearch.sortedByDescending { it.id }
            TaskSortOrder.DATE_ASC -> filteredBySearch.sortedBy { it.id }
            TaskSortOrder.NAME_ASC -> filteredBySearch.sortedBy { it.description.lowercase() }
            TaskSortOrder.NAME_DESC -> filteredBySearch.sortedByDescending { it.description.lowercase() }
            TaskSortOrder.STATUS_PENDING_FIRST -> filteredBySearch.sortedWith(
                compareBy<Task> { it.isCompleted }.thenByDescending { it.id }
            )
            TaskSortOrder.STATUS_COMPLETED_FIRST -> filteredBySearch.sortedWith(
                compareByDescending<Task> { it.isCompleted }.thenByDescending { it.id }
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val workManager = WorkManager.getInstance(application)

    init {
        // Al inicializar, cargamos las tareas de la base de datos local
        viewModelScope.launch {
            _tasks.value = taskDao.getAllTasks()
        }
    }

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onFilterChange(filter: TaskFilter) {
        _selectedFilter.value = filter
    }

    fun onSortOrderChange(sortOrder: TaskSortOrder) {
        _sortOrder.value = sortOrder
    }

    // Función para añadir una nueva tarea
    fun addTask(description: String) {
        val newTask = Task(description = description)
        viewModelScope.launch {
            val generatedId = taskDao.insertTask(newTask)
            val taskWithId = newTask.copy(id = generatedId.toInt())
            _tasks.value = taskDao.getAllTasks() // Recargamos la lista

            // Programar notificación utilizando WorkManager
            scheduleTaskNotification(description)

            // Sincronizar inserción con Firestore
            syncService.uploadTask(taskWithId)
        }
    }

    // Función para editar una tarea existente
    fun editTask(task: Task, newDescription: String) {
        viewModelScope.launch {
            val updatedTask = task.copy(description = newDescription)
            taskDao.updateTask(updatedTask)
            _tasks.value = taskDao.getAllTasks()

            // Sincronizar actualización con Firestore
            syncService.updateTask(updatedTask)
        }
    }

    // Función para alternar el estado de completado de una tarea
    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            val updatedTask = task.copy(isCompleted = !task.isCompleted)
            taskDao.updateTask(updatedTask)
            _tasks.value = taskDao.getAllTasks() // Recargamos la lista

            // Sincronizar actualización con Firestore
            syncService.updateTask(updatedTask)
        }
    }

    // Función para eliminar una tarea individual
    fun deleteTask(task: Task) {
        viewModelScope.launch {
            taskDao.deleteTask(task)
            _tasks.value = taskDao.getAllTasks()

            // Sincronizar eliminación con Firestore
            syncService.deleteTask(task)
        }
    }

    // Función para eliminar todas las tareas
    fun deleteAllTasks() {
        viewModelScope.launch {
            val currentTasks = _tasks.value
            taskDao.deleteAllTasks()
            _tasks.value = emptyList() // Vaciamos la lista en el estado

            // Sincronizar borrado con Firestore
            syncService.deleteAllTasks(currentTasks)
        }
    }

    // Programar notificación con WorkManager
    private fun scheduleTaskNotification(taskTitle: String) {
        val inputData = Data.Builder()
            .putString(TaskNotificationWorker.KEY_TASK_TITLE, taskTitle)
            .build()

        val notificationWorkRequest = OneTimeWorkRequestBuilder<TaskNotificationWorker>()
            .setInputData(inputData)
            .build()

        workManager.enqueue(notificationWorkRequest)
    }

    companion object {
        // Obtenemos de forma reflectiva la instancia de Application si se llama desde el constructor de compatibilidad
        @Suppress("UNCHECKED_CAST")
        private fun getApplicationInstance(): Application {
            return try {
                val activityThreadClass = Class.forName("android.app.ActivityThread")
                val currentApplicationMethod = activityThreadClass.getMethod("currentApplication")
                currentApplicationMethod.invoke(null) as Application
            } catch (e: Exception) {
                try {
                    val appGlobalsClass = Class.forName("android.app.AppGlobals")
                    val getInitialApplicationMethod = appGlobalsClass.getMethod("getInitialApplication")
                    getInitialApplicationMethod.invoke(null) as Application
                } catch (e2: Exception) {
                    throw IllegalStateException("No se pudo obtener la instancia de Application automáticamente", e2)
                }
            }
        }
    }
}
