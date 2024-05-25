package com.example.levkomandroid.repository

import android.util.Log
import com.example.levkomandroid.network.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import okio.IOException
import retrofit2.HttpException
import retrofit2.Response

class RemoteDatasource(private val levkomService: LevkomService) {
    @Throws(Throwable::class)
    suspend fun getRoutes(): List<RouteDto> {
        val response = levkomService.getRoutes()  // This returns a Response object
        if (response.isSuccessful) {
            // Successfully received the list of routes
            return response.body() ?: throw Exception("No routes found")  // Return the list or throw if null
        } else {
            // Handle cases where the response is not successful
            throw Exception("Failed to fetch routes: ${response.errorBody()?.string()}")
        }
    }

    @Throws(Throwable::class)
    suspend fun deleteRoute(routeId: Int): Boolean {
        try {
            val response = levkomService.deleteRoute(routeId)
            return response.isSuccessful
        } catch (e: IOException) {
            // Error handling
            throw Exception("Network error: ${e.message}", e)
        } catch (e: HttpException) {
            // HTTP error handling (egs. 404 or 500)
            throw Exception("HTTP error: ${e.response()?.errorBody()?.string()}", e)
        }
    }
    @Throws(Throwable::class)
    suspend fun createRoute(): Int {
        val response = levkomService.createRoute()
        return response.body() ?: throw Exception("No route ID returned")
    }


    @Throws(Throwable::class)
    suspend fun importAddress(address: Address, routeId: Int): Void {
        val response = levkomService.importAddress(address, routeId)
        if (!response.isSuccessful) throw Exception("Failed to import address: ${response.errorBody()?.string()}")
        return response.body() ?: throw Exception("No response body")
    }

    @Throws(Throwable::class)
    suspend fun getAddressesByRouteIdWithOrder(routeId: Int): List<DeliveryAddr> {
        if (routeId <= 0) {
            throw Exception("Invalid route ID: $routeId")
        }

        val response = levkomService.getAddressesByRouteIdWithOrder(routeId)
        if (response.isSuccessful) {
            return response.body() ?: throw Exception("No data received")
        } else {
            throw Exception("Failed to fetch addresses: ${response.errorBody()?.string()}")
        }
    }

}
