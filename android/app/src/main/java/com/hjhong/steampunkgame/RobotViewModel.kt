package com.hjhong.steampunkgame

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class RobotViewModel(application: Application) : AndroidViewModel(application) {

    private val loadoutManager = RobotLoadoutManager(application)

    private val _parts = MutableStateFlow<List<RobotPart>>(emptyList())
    val parts: StateFlow<List<RobotPart>> = _parts.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _equipped = MutableStateFlow<Map<String, String>>(emptyMap())
    val equipped: StateFlow<Map<String, String>> = _equipped.asStateFlow()

    init {
        Firebase.firestore.collection("robotParts")
            .get()
            .addOnSuccessListener { snap ->
                _parts.value = snap.toObjects(RobotPart::class.java)
                observeAllSlots()
            }
            .addOnFailureListener { e -> _error.value = e.message }
    }

    private fun observeAllSlots() {
        val slots = _parts.value.map { it.slot }.distinct()
        if (slots.isEmpty()) return

        val flows = slots.map { slot ->
            loadoutManager.equippedPartIdFlow(slot)
        }

        viewModelScope.launch {
            combine(flows) { values ->
                val map = mutableMapOf<String, String>()
                slots.forEachIndexed { index, slot ->
                    val partId = values[index]
                    if (partId != null) map[slot] = partId
                }
                map.toMap()
            }.collect { map ->
                _equipped.value = map
            }
        }
    }

    fun equip(slot: String, partId: String) {
        viewModelScope.launch {
            loadoutManager.equip(slot, partId)
        }
    }

    fun unequip(slot: String) {
        viewModelScope.launch {
            loadoutManager.unequip(slot)
        }
    }

    fun totalAttack(): Int = equippedParts().sumOf { it.bonusAttack }
    fun totalDefense(): Int = equippedParts().sumOf { it.bonusDefense }

    fun representativeImagePath(): String? {
        val eq = equippedParts()
        val bodyPart = eq.find { it.slot.contains("body", true) || it.slot.contains("몸통") }
        return (bodyPart ?: eq.firstOrNull { it.imagePath.isNotBlank() })?.imagePath?.takeIf { it.isNotBlank() }
    }

    private fun equippedParts(): List<RobotPart> {
        val equippedIds = _equipped.value.values.toSet()
        return _parts.value.filter { it.id in equippedIds }
    }
}