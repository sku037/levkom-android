package com.example.levkomandroid.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import okio.IOException

data class MyAppPreferences(
    val userId: Int
)

object MyPreferencesKeys {
    val USER_ID = intPreferencesKey("user_id")
}

class SettingsDataStore(private val dataStore: DataStore<Preferences>){
    val appSettingsFlow: Flow<MyAppPreferences> = dataStore.data
        .catch {
            if (it is IOException){
                it.printStackTrace()
                emit(emptyPreferences())
            } else {
                throw it
            }
        }
        .map { preferences ->
            val userId = preferences[MyPreferencesKeys.USER_ID] ?: -1
            MyAppPreferences(userId)
        }

    suspend fun updateUserId(newUserId: Int) {
        dataStore.edit { preferences ->
            preferences[MyPreferencesKeys.USER_ID] = newUserId
        }
    }
}