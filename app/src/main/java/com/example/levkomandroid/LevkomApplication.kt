package com.example.levkomandroid

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.example.levkomandroid.repository.LevkomRepository


class LevkomApplication: Application() {
    val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
        name = "app_settings"
    )

    val levkomRepository: LevkomRepository
        get() = ServiceLocator.provideLevkomRepository(this, dataStore)
}