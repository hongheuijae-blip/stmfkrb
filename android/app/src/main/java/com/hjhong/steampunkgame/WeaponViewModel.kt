package com.hjhong.steampunkgame

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WeaponViewModel : ViewModel() {
    private val _weapons = MutableStateFlow<List<Weapon>>(emptyList())
    val weapons: StateFlow<List<Weapon>> = _weapons.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        Firebase.firestore.collection("weapons")
            .get()
            .addOnSuccessListener { snap -> _weapons.value = snap.toObjects(Weapon::class.java) }
            .addOnFailureListener { e -> _error.value = e.message }
    }
}