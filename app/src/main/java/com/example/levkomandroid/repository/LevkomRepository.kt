package com.example.levkomandroid.repository

import com.example.levkomandroid.network.Address
import com.example.levkomandroid.network.DeliveryAddr
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import com.example.levkomandroid.network.RouteDto

class LevkomRepository constructor(
    private val remoteDatasource: RemoteDatasource,
    private val settingsDataStore: SettingsDataStore
) {
    val appPreferences: Flow<MyAppPreferences> = settingsDataStore.appSettingsFlow

    suspend fun updateUserId(newUserId: Int) {
        settingsDataStore.updateUserId(newUserId)
    }

    suspend fun getRoutes(): Flow<List<RouteDto>> = flow {
        emit(remoteDatasource.getRoutes())
    }

    suspend fun deleteRoute(routeId: Int): Flow<Boolean> = flow {
        emit(remoteDatasource.deleteRoute(routeId))
    }

    suspend fun createRoute(): Flow<Int> = flow {
        val response = remoteDatasource.createRoute()
        emit(response)
    }


    suspend fun importAddress(address: Address, routeId: Int): Flow<Void> = flow {
        emit(remoteDatasource.importAddress(address, routeId))
    }

    suspend fun getAddressesByRouteIdWithOrder(routeId: Int): Flow<List<DeliveryAddr>> = flow {
        emit(remoteDatasource.getAddressesByRouteIdWithOrder(routeId))
    }
}