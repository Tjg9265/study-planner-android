package com.example.studyplanner

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.text.ParsePosition
import java.util.Locale
import java.util.UUID

// An immutable task. A unique ID keeps tasks with identical names separate.
data class StudyTask(
    val id: String,
    val name: String,
    val course: String,
    val dueDate: String,
    val completed: Boolean = false
)

// Encapsulates conversion between task objects and private on-device storage.
class TaskStorage(context: Context) {
    private val preferences = context.getSharedPreferences("study_planner", Context.MODE_PRIVATE)

    // Rebuilds the saved tasks; malformed data is reported rather than erased.
    fun load(): List<StudyTask> {
        val array = JSONArray(preferences.getString("tasks", "[]") ?: "[]")
        return List(array.length()) { index ->
            val item = array.getJSONObject(index)
            StudyTask(
                id = item.getString("id"),
                name = item.getString("name"),
                course = item.getString("course"),
                dueDate = item.getString("dueDate"),
                completed = item.getBoolean("completed")
            )
        }
    }

    // Saves the complete small list and confirms that the write succeeded.
    // For a larger app, move disk writes off the UI thread and use Room/DataStore.
    fun save(tasks: List<StudyTask>): Boolean {
        val array = JSONArray()
        tasks.forEach { task ->
            array.put(JSONObject().apply {
                put("id", task.id)
                put("name", task.name)
                put("course", task.course)
                put("dueDate", task.dueDate)
                put("completed", task.completed)
            })
        }
        return preferences.edit().putString("tasks", array.toString()).commit()
    }
}

class MainActivity : ComponentActivity() {
    // Creates storage and opens the Compose interface when the activity starts.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val storage = TaskStorage(applicationContext)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    StudyPlannerApp(storage)
                }
            }
        }
    }
}

// Checks both the exact YYYY-MM-DD shape and the calendar date (including leap years).
fun isValidDate(value: String): Boolean {
    if (!Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}").matches(value)) return false
    val format = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
    val position = ParsePosition(0)
    return format.parse(value, position) != null && position.index == value.length
}

// Owns the task list and switches between the list screen and the entry screen.
@Composable
fun StudyPlannerApp(storage: TaskStorage) {
    val initialLoad = remember(storage) { runCatching { storage.load() } }
    var tasks by remember(storage) { mutableStateOf(initialLoad.getOrDefault(emptyList())) }
    var addingTask by rememberSaveable { mutableStateOf(false) }
    var error by remember { mutableStateOf(
        if (initialLoad.isFailure) "Saved tasks could not be read. Changes are disabled to protect them."
        else ""
    ) }
    val canEdit = initialLoad.isSuccess

    // Updates the visible list only after storage confirms a successful save.
    fun persist(updated: List<StudyTask>): Boolean {
        if (!canEdit) return false
        val saved = runCatching { storage.save(updated) }.getOrDefault(false)
        if (saved) {
            tasks = updated
            error = ""
        } else {
            error = "Could not save this change. Please try again."
        }
        return saved
    }

    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Study Planner", style = MaterialTheme.typography.headlineMedium)
        if (error.isNotEmpty()) {
            Text(error, color = MaterialTheme.colorScheme.error)
        }
        if (addingTask) {
            AddTaskScreen(
                onSave = { name, course, date ->
                    val task = StudyTask(UUID.randomUUID().toString(), name, course, date)
                    if (persist(tasks + task)) addingTask = false
                },
                onCancel = { addingTask = false }
            )
        } else {
            Text("${tasks.count { !it.completed }} remaining • ${tasks.count { it.completed }} completed")
            Button(onClick = { addingTask = true }, enabled = canEdit) {
                Text("Add assignment")
            }
            if (tasks.isEmpty()) {
                Text("No assignments yet. Add your first task!")
            }
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(tasks.sortedWith(compareBy<StudyTask> { it.completed }.thenBy { it.dueDate }),
                    key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        enabled = canEdit,
                        onToggle = {
                            persist(tasks.map {
                                if (it.id == task.id) it.copy(completed = !it.completed) else it
                            })
                        },
                        onDelete = { persist(tasks.filterNot { it.id == task.id }) }
                    )
                }
            }
        }
    }
}

// Collects a new assignment and prevents blank fields or invalid dates.
@Composable
fun AddTaskScreen(onSave: (String, String, String) -> Unit, onCancel: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var course by rememberSaveable { mutableStateOf("") }
    var dueDate by rememberSaveable { mutableStateOf("") }
    var validationError by rememberSaveable { mutableStateOf("") }

    // A lazy container keeps the form scrollable on smaller screens and with the keyboard open.
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("New assignment", style = MaterialTheme.typography.titleLarge) }
        item {
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Task name") }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            OutlinedTextField(
                value = course, onValueChange = { course = it },
                label = { Text("Course") }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            OutlinedTextField(
                value = dueDate, onValueChange = { dueDate = it },
                label = { Text("Due date (YYYY-MM-DD)") },
                placeholder = { Text("2026-10-15") }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            if (validationError.isNotEmpty()) {
                Text(validationError, color = MaterialTheme.colorScheme.error)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    validationError = when {
                        name.isBlank() || course.isBlank() -> "Enter a task name and course."
                        !isValidDate(dueDate.trim()) -> "Enter a real date using YYYY-MM-DD."
                        else -> ""
                    }
                    if (validationError.isEmpty()) onSave(name.trim(), course.trim(), dueDate.trim())
                }) { Text("Save task") }
                OutlinedButton(onClick = onCancel) { Text("Cancel") }
            }
        }
    }
}

// Displays one task with completion controls and a confirmation before deletion.
@Composable
fun TaskCard(task: StudyTask, enabled: Boolean, onToggle: () -> Unit, onDelete: () -> Unit) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = task.completed, onCheckedChange = { onToggle() }, enabled = enabled)
                Text(
                    text = task.name,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None,
                    modifier = Modifier.weight(1f)
                )
            }
            Text("Course: ${task.course}")
            Text("Due: ${task.dueDate}")
            TextButton(onClick = { confirmDelete = true }, enabled = enabled) { Text("Delete") }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete assignment?") },
            text = { Text("Delete '${task.name}'?") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            }
        )
    }
}
