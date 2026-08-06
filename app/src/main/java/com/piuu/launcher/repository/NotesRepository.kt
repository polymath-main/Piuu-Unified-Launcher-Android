package com.piuu/launcher/repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class LauncherNote(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val category: String = "General"
)

/**
 * Persistent Notes Repository for Piuu Launcher.
 * Stores quick memos and notes permanently in SharedPreferences using JSON payloads.
 */
class NotesRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("piuu_notes_prefs", Context.MODE_PRIVATE)
    private val _notes = MutableStateFlow<List<LauncherNote>>(emptyList())
    val notes: StateFlow<List<LauncherNote>> = _notes

    init {
        loadNotes()
    }

    private fun loadNotes() {
        val rawJson = prefs.getString("saved_notes_json", null)
        if (!rawJson.isNullOrBlank()) {
            try {
                val array = JSONArray(rawJson)
                val loadedList = mutableListOf<LauncherNote>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    loadedList.add(
                        LauncherNote(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            title = obj.optString("title", "Quick Note"),
                            content = obj.optString("content", ""),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            category = obj.optString("category", "General")
                        )
                    )
                }
                _notes.value = loadedList
                return
            } catch (t: Throwable) {
                // Fallback to initial default note
            }
        }

        val defaultNotes = listOf(
            LauncherNote(
                title = "Welcome to Piuu Notes",
                content = "Use this quick memo space to write tasks, reminders, and thoughts directly from the PiP bar or Homescreen widget.",
                category = "System"
            )
        )
        _notes.value = defaultNotes
        saveToDisk(defaultNotes)
    }

    fun addNote(title: String, content: String, category: String = "General") {
        val newNote = LauncherNote(title = title, content = content, category = category)
        val updated = listOf(newNote) + _notes.value
        _notes.value = updated
        saveToDisk(updated)
    }

    fun updateNote(id: String, newTitle: String, newContent: String) {
        val updated = _notes.value.map { note ->
            if (note.id == id) note.copy(title = newTitle, content = newContent, timestamp = System.currentTimeMillis())
            else note
        }
        _notes.value = updated
        saveToDisk(updated)
    }

    fun deleteNote(id: String) {
        val updated = _notes.value.filterNot { it.id == id }
        _notes.value = updated
        saveToDisk(updated)
    }

    private fun saveToDisk(notesList: List<LauncherNote>) {
        val array = JSONArray()
        for (note in notesList) {
            val obj = JSONObject()
            obj.put("id", note.id)
            obj.put("title", note.title)
            obj.put("content", note.content)
            obj.put("timestamp", note.timestamp)
            obj.put("category", note.category)
            array.put(obj)
        }
        prefs.edit().putString("saved_notes_json", array.toString()).apply()
    }

    companion object {
        @Volatile
        private var instance: NotesRepository? = null

        fun getInstance(context: Context): NotesRepository {
            return instance ?: synchronized(this) {
                instance ?: NotesRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
