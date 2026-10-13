package com.mariusjhordan.lab08

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mariusjhordan.lab08.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(viewModel: TaskViewModel) {
    val allTasks by viewModel.tasks.collectAsState()
    val visibleTasks by viewModel.visibleTasks.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }
    var taskToDelete by remember { mutableStateOf<Task?>(null) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    val pendingCount = remember(allTasks) { allTasks.count { !it.isCompleted } }
    val completedCount = remember(allTasks) { allTasks.count { it.isCompleted } }

    val currentDateFormatted = remember {
        val dateFormat = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("es", "ES"))
        dateFormat.format(Date()).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("es", "ES")) else it.toString() }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = TodoistRed,
                contentColor = Color.White,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Agregar Tarea",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Barra de Búsqueda y Orden superior estilo Todoist
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.onSearchQueryChange(it) },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            placeholder = {
                                Text(
                                    "Buscar tareas...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Buscar",
                                    tint = TodoistRed
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Limpiar búsqueda",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TodoistRed,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Botón de menú de ordenamiento
                        Box {
                            IconButton(
                                onClick = { showSortMenu = true }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sort,
                                    contentDescription = "Ordenar tareas",
                                    tint = TodoistRed
                                )
                            }

                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                Text(
                                    "Ordenar por:",
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                DropdownMenuItem(
                                    text = { Text("Más recientes") },
                                    onClick = {
                                        viewModel.onSortOrderChange(TaskSortOrder.DATE_DESC)
                                        showSortMenu = false
                                    },
                                    leadingIcon = {
                                        if (sortOrder == TaskSortOrder.DATE_DESC) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = TodoistRed)
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Más antiguas") },
                                    onClick = {
                                        viewModel.onSortOrderChange(TaskSortOrder.DATE_ASC)
                                        showSortMenu = false
                                    },
                                    leadingIcon = {
                                        if (sortOrder == TaskSortOrder.DATE_ASC) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = TodoistRed)
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Nombre (A - Z)") },
                                    onClick = {
                                        viewModel.onSortOrderChange(TaskSortOrder.NAME_ASC)
                                        showSortMenu = false
                                    },
                                    leadingIcon = {
                                        if (sortOrder == TaskSortOrder.NAME_ASC) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = TodoistRed)
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Nombre (Z - A)") },
                                    onClick = {
                                        viewModel.onSortOrderChange(TaskSortOrder.NAME_DESC)
                                        showSortMenu = false
                                    },
                                    leadingIcon = {
                                        if (sortOrder == TaskSortOrder.NAME_DESC) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = TodoistRed)
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Pendientes primero") },
                                    onClick = {
                                        viewModel.onSortOrderChange(TaskSortOrder.STATUS_PENDING_FIRST)
                                        showSortMenu = false
                                    },
                                    leadingIcon = {
                                        if (sortOrder == TaskSortOrder.STATUS_PENDING_FIRST) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = TodoistRed)
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Completadas primero") },
                                    onClick = {
                                        viewModel.onSortOrderChange(TaskSortOrder.STATUS_COMPLETED_FIRST)
                                        showSortMenu = false
                                    },
                                    leadingIcon = {
                                        if (sortOrder == TaskSortOrder.STATUS_COMPLETED_FIRST) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = TodoistRed)
                                        }
                                    }
                                )
                            }
                        }

                        // Botón para borrar todas las tareas
                        if (allTasks.isNotEmpty()) {
                            IconButton(onClick = { showDeleteAllDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = "Eliminar todas",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 2. Encabezado principal estilo Todoist ("Hoy" + Fecha + Contador)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Hoy",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        )
                        Text(
                            text = currentDateFormatted,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Badge / Contador visual estilo Todoist
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (pendingCount > 0) TodoistRed.copy(alpha = 0.12f) else TodoistCompleted.copy(alpha = 0.12f),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (pendingCount > 0) TodoistRed else TodoistCompleted)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$pendingCount pendientes",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (pendingCount > 0) TodoistRed else TodoistCompleted
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Chips de Filtro interactivo (Todas, Pendientes, Completadas)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == TaskFilter.ALL,
                        onClick = { viewModel.onFilterChange(TaskFilter.ALL) },
                        label = { Text("Todas (${allTasks.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TodoistRed,
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = selectedFilter == TaskFilter.PENDING,
                        onClick = { viewModel.onFilterChange(TaskFilter.PENDING) },
                        label = { Text("Pendientes ($pendingCount)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TodoistRed,
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = selectedFilter == TaskFilter.COMPLETED,
                        onClick = { viewModel.onFilterChange(TaskFilter.COMPLETED) },
                        label = { Text("Completadas ($completedCount)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TodoistCompleted,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // 4. Lista de Tareas estilo Todoist (Cards con bordes suaves, checkbox redondo, acciones)
            if (visibleTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            modifier = Modifier.size(64.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = TodoistCompleted,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) {
                                "No se encontraron tareas para \"$searchQuery\""
                            } else when (selectedFilter) {
                                TaskFilter.PENDING -> "¡Todo listo! No tienes tareas pendientes."
                                TaskFilter.COMPLETED -> "Aún no has completado ninguna tarea."
                                TaskFilter.ALL -> "No hay tareas. ¡Toca '+' para agregar la primera!"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = visibleTasks,
                        key = { it.id }
                    ) { task ->
                        TaskItemCard(
                            task = task,
                            onToggleComplete = { viewModel.toggleTaskCompletion(task) },
                            onEditClick = { taskToEdit = task },
                            onDeleteClick = { taskToDelete = task }
                        )
                    }
                    // Espacio inferior para evitar que el FAB tape el último item
                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }
    }

    // Diálogo para Agregar Tarea
    if (showAddDialog) {
        TaskDialog(
            title = "Nueva tarea",
            confirmButtonText = "Agregar tarea",
            initialText = "",
            onDismiss = { showAddDialog = false },
            onConfirm = { description ->
                if (description.isNotBlank()) {
                    viewModel.addTask(description.trim())
                    showAddDialog = false
                }
            }
        )
    }

    // Diálogo para Editar Tarea
    taskToEdit?.let { task ->
        TaskDialog(
            title = "Editar tarea",
            confirmButtonText = "Guardar cambios",
            initialText = task.description,
            onDismiss = { taskToEdit = null },
            onConfirm = { updatedDescription ->
                if (updatedDescription.isNotBlank()) {
                    viewModel.editTask(task, updatedDescription.trim())
                    taskToEdit = null
                }
            }
        )
    }

    // Diálogo para Confirmar Eliminación Individual
    taskToDelete?.let { task ->
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            title = { Text("¿Eliminar tarea?") },
            text = { Text("\"${task.description}\" se eliminará permanentemente de tu dispositivo y de la nube.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTask(task)
                        taskToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = TodoistRed)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { taskToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo para Confirmar Eliminación de Todas las Tareas
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("¿Eliminar todas las tareas?") },
            text = { Text("Esta acción vaciará toda tu lista tanto localmente como en Firestore.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAllTasks()
                        showDeleteAllDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = TodoistRed)
                ) {
                    Text("Eliminar todas")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

/**
 * Tarjeta individual de tarea estilo Todoist:
 * - Borde suave y elevación limpia.
 * - Botón de completado redondo (circle check) característico de Todoist.
 * - Título con tachado suave al completar.
 * - Etiqueta de estado en color.
 * - Botones de acción rápida: Editar y Eliminar.
 */
@Composable
fun TaskItemCard(
    task: Task,
    onToggleComplete: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox Redondo estilo Todoist
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        if (task.isCompleted) TodoistCompleted else Color.Transparent
                    )
                    .border(
                        width = 2.dp,
                        color = if (task.isCompleted) TodoistCompleted else MaterialTheme.colorScheme.outline,
                        shape = CircleShape
                    )
                    .clickable { onToggleComplete() },
                contentAlignment = Alignment.Center
            ) {
                if (task.isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completada",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Información de la tarea
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onToggleComplete() }
            ) {
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        color = if (task.isCompleted) {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Etiqueta de estado
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (task.isCompleted) {
                        TodoistCompleted.copy(alpha = 0.12f)
                    } else {
                        TodoistPending.copy(alpha = 0.12f)
                    }
                ) {
                    Text(
                        text = if (task.isCompleted) "Completada" else "Pendiente",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = if (task.isCompleted) TodoistCompleted else TodoistPending
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Botón Editar
            IconButton(
                onClick = onEditClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Editar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Botón Eliminar
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Eliminar",
                    tint = TodoistRed.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Diálogo modal para Agregar o Editar Tareas estilo Todoist
 */
@Composable
fun TaskDialog(
    title: String,
    confirmButtonText: String,
    initialText: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialText) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Descripción de la tarea") },
                    placeholder = { Text("Ej. Terminar informe de proyecto") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TodoistRed,
                        focusedLabelColor = TodoistRed
                    ),
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TodoistRed,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(confirmButtonText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
