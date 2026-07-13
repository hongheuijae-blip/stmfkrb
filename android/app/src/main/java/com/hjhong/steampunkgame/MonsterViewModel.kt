package com.hjhong.steampunkgame

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MonsterViewModel : ViewModel() {
    private val _monsters = MutableStateFlow<List<Monster>>(emptyList())
    val monsters: StateFlow<List<Monster>> = _monsters.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        Firebase.firestore.collection("monsters")
            .get()
            .addOnSuccessListener { snap ->
                _monsters.value = snap.toObjects(Monster::class.java)
            }
            .addOnFailureListener { e ->
                _error.value = e.message
            }
    }
}