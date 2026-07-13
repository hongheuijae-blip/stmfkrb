package com.hjhong.steampunkgame

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class QuestViewModel : ViewModel() {
    private val _quests = MutableStateFlow<List<Quest>>(emptyList())
    val quests: StateFlow<List<Quest>> = _quests.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        Firebase.firestore.collection("quests")
            .get()
            .addOnSuccessListener { snap -> _quests.value = snap.toObjects(Quest::class.java) }
            .addOnFailureListener { e -> _error.value = e.message }
    }
}