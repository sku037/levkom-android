package com.example.levkomandroid.repository

import com.example.levkomandroid.network.Address
import com.example.levkomandroid.network.DeliveryAddr
import com.example.levkomandroid.network.ImportAddressesResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import com.example.levkomandroid.network.RouteDto
import okio.IOException
import retrofit2.HttpException

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

    suspend fun importAddresses(addresses: List<Address>, routeId: Int): Flow<ImportAddressesResult> = flow {
        emit(remoteDatasource.importAddresses(addresses, routeId))
    }

    suspend fun deleteAddress(addressId: Int, routeId: Int): Flow<Boolean> = flow {
        emit(remoteDatasource.deleteAddress(addressId, routeId))
    }

    // Calculate Route
    suspend fun calculateRoute(routeId: Int): Flow<Boolean> = flow {
        emit(remoteDatasource.calculateRoute(routeId))
    }

    // Get geometry for the route
    suspend fun getRouteGeometryByRouteId(routeId: Int): String? {
        return remoteDatasource.getRouteGeometryByRouteId(routeId)
    }
}