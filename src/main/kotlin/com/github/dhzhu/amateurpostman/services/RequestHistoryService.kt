package com.github.dhzhu.amateurpostman.services

import com.github.dhzhu.amateurpostman.models.HttpRequest
import com.github.dhzhu.amateurpostman.models.HttpResponse
import com.github.dhzhu.amateurpostman.models.RequestHistoryEntry
import com.github.dhzhu.amateurpostman.models.RequestHistoryState
import com.github.dhzhu.amateurpostman.models.SerializableHistoryEntry
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project

/** Service for managing request history with persistent storage */
@Service(Service.Level.PROJECT)
@State(name = "RequestHistoryService", storages = [Storage("amateur-postman-history.xml")])
class RequestHistoryService(private val project: Project) :
        PersistentStateComponent<RequestHistoryState> {

    private var state = RequestHistoryState()
    private val listeners = mutableListOf<() -> Unit>()

    override fun getState(): RequestHistoryState = state

    override fun loadState(state: RequestHistoryState) {
        this.state = state
    }

    /** Add a new history entry */
    fun addEntry(request: HttpRequest, response: HttpResponse? = null, name: String? = null) {
        val entry = RequestHistoryEntry(request = request, response = response, name = name)

        val serializable = SerializableHistoryEntry.fromHistoryEntry(entry)
        state.entries.add(0, serializable) // Add to beginning

        // Trim to max size
        while (state.entries.size > RequestHistoryState.MAX_ENTRIES) {
            state.entries.removeAt(state.entries.size - 1)
        }

        notifyListeners()
    }

    /** Get all history entries */
    fun getHistory(): List<RequestHistoryEntry> {
        return state.entries.map { it.toHistoryEntry() }
    }

    /** Get history entries filtered by search query */
    fun searchHistory(query: String): List<RequestHistoryEntry> {
        if (query.isBlank()) return getHistory()

        val lowerQuery = query.lowercase()
        return getHistory().filter { entry ->
            entry.request.url.lowercase().contains(lowerQuery) ||
                    entry.request.method.name.lowercase().contains(lowerQuery) ||
                    entry.name?.lowercase()?.contains(lowerQuery) == true
        }
    }

    /** Delete a history entry by ID */
    fun deleteEntry(id: String) {
        state.entries.removeIf { it.id == id }
        notifyListeners()
    }

    /** Clear all history */
    fun clearHistory() {
        state.entries.clear()
        notifyListeners()
    }

    /** Get a single entry by ID */
    fun getEntry(id: String): RequestHistoryEntry? {
        return state.entries.find { it.id == id }?.toHistoryEntry()
    }

    /** Update an entry's name */
    fun renameEntry(id: String, newName: String) {
        val index = state.entries.indexOfFirst { it.id == id }
        if (index >= 0) {
            state.entries[index] = state.entries[index].copy(name = newName)
            notifyListeners()
        }
    }

    /** Add a listener for history changes */
    fun addChangeListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    /** Remove a change listener */
    fun removeChangeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }

    private fun notifyListeners() {
        listeners.forEach { it.invoke() }
    }

    /** Get the count of history entries */
    fun getHistoryCount(): Int = state.entries.size
}
