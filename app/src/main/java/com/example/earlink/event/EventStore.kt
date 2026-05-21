package com.example.earlink.event

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object EventStore {
    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> = _events.asStateFlow()

    fun addEvent(event: Event) {
        val currentList = _events.value.toMutableList()
        currentList.add(0, event) // Insert at the top (newest first)
        if (currentList.size > 100) {
            currentList.removeAt(currentList.size - 1)
        }
        _events.value = currentList
    }

    fun clear() {
        _events.value = emptyList()
    }
}
