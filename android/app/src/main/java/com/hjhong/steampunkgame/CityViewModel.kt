package com.hjhong.steampunkgame

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CityViewModel : ViewModel() {
    private val _cities = MutableStateFlow<List<City>>(emptyList())
    val cities: StateFlow<List<City>> = _cities.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        Firebase.firestore.collection("cities")
            .get()
            .addOnSuccessListener { snap -> _cities.value = snap.toObjects(City::class.java) }
            .addOnFailureListener { e -> _error.value = e.message }
    }
}