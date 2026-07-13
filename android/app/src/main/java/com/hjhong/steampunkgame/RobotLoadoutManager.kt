package com.hjhong.steampunkgame

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.robotDataStore by preferencesDataStore(name = "robot_loadout")

class RobotLoadoutManager(private val context: Context) {

    private fun keyFor(slot: String) = stringPreferencesKey("equipped_$slot")

    fun equippedPartIdFlow(slot: String): Flow<String?> =
        context.robotDataStore.data.map { prefs -> prefs[keyFor(slot)] }

    suspend fun equip(slot: String, partId: String) {
        context.robotDataStore.edit { prefs -> prefs[keyFor(slot)] = partId }
    }

    suspend fun unequip(slot: String) {
        context.robotDataStore.edit { prefs -> prefs.remove(keyFor(slot)) }
    }
}