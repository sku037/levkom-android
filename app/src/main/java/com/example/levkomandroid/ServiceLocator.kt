package com.example.levkomandroid

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.example.levkomandroid.network.LevkomApi
import com.example.levkomandroid.repository.LevkomRepository
import com.example.levkomandroid.repository.RemoteDatasource
import com.example.levkomandroid.repository.SettingsDataStore


object ServiceLocator {
    var levkomRepository: LevkomRepository? = null
    fun provideLevkomRepository(
        context: Context,
        dataStore: DataStore<Preferences>
    ): LevkomRepository {
        synchronized(this) {
            return  levkomRepository ?: createLevkomRepository(context, dataStore)
        }
    }

    private fun createLevkomRepository(
        context: Context,
        dataStore: DataStore<Preferences>
    ): LevkomRepository {
        val remoteDatasource = createRemoteDataSource(context)
        val settingsDataStore = createSettingsDataSource(dataStore)
        val newRepo = LevkomRepository(remoteDatasource, settingsDataStore)
        levkomRepository = newRepo
        return newRepo
    }

    private fun createRemoteDataSource(context: Context): RemoteDatasource {
        val retrofitService = LevkomApi.retrofitService
        return RemoteDatasource(retrofitService)
    }

    private fun createSettingsDataSource(dataStore: DataStore<Preferences>): SettingsDataStore {
        val settingsDataStore = SettingsDataStore(dataStore)
        return settingsDataStore
    }
}