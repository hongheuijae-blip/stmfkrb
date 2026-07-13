package com.hjhong.steampunkgame

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RobotViewModel(application: Application) : AndroidViewModel(application) {

    private val loadoutManager = RobotLoadoutManager(application)

    private val _parts = MutableStateFlow<List<RobotPart>>(emptyList())
    val parts: StateFlow<List<RobotPart>> = _parts.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // 슬롯 이름 -> 장착된 파츠 id
    private val _equipped = MutableStateFlow<Map<String, String>>(emptyMap())
    val equipped: StateFlow<Map<String, String>> = _equipped.asStateFlow()

    init {
        Firebase.firestore.collection("robotParts")
            .get()
            .addOnSuccessListener { snap ->
                _parts.value = snap.toObjects(RobotPart::class.java)
                loadEquippedForAllSlots()
            }
            .addOnFailureListener { e -> _error.value = e.message }
    }

    private fun loadEquippedForAllSlots() {
        val slots = _parts.value.map { it.slot }.distinct()
        viewModelScope.launch {
            val result = mutableMapOf<String, String>()
            slots.forEach { slot ->
                loadoutManager.equippedPartIdFlow(slot).collect { partId ->
                    if (partId != null) result[slot] = partId
                    _equipped.value = result.toMap()
                    return@collect
                }
            }
        }
    }

    fun equip(slot: String, partId: String) {
        viewModelScope.launch {
            loadoutManager.equip(slot, partId)
            _equipped.value = _equipped.value.toMutableMap().apply { put(slot, partId) }
        }
    }

    fun unequip(slot: String) {
        viewModelScope.launch {
            loadoutManager.unequip(slot)
            _equipped.value = _equipped.value.toMutableMap().apply { remove(slot) }
        }
    }

    fun totalAttack(): Int = equippedParts().sumOf { it.bonusAttack }
    fun totalDefense(): Int = equippedParts().sumOf { it.bonusDefense }

    private fun equippedParts(): List<RobotPart> {
        val equippedIds = _equipped.value.values.toSet()
        return _parts.value.filter { it.id in equippedIds }
    }
}