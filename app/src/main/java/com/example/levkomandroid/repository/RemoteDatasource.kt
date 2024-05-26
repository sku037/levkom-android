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
        try {
            val response = levkomService.getAddressesByRouteIdWithOrder(routeId)  // Retrieve the Response object
            if (response.isSuccessful) {
                Log.d("RemoteDatasource", "Addresses fetched successfully for route ID $routeId")
                return response.body() ?: throw Exception("No data received")  // Return the response body if the request was successful
            } else {
                Log.e("RemoteDatasource", "Failed to fetch addresses for route ID $routeId: ${response.errorBody()?.string()}")
                throw Exception("Failed to fetch addresses: ${response.errorBody()?.string()}")  // Throw an exception in case of error
            }
        } catch (e: HttpException) {
            Log.e("RemoteDatasource", "HTTP Exception while fetching addresses: ${e.response()?.errorBody()?.string()}", e)
            throw Exception("Failed to fetch addresses: ${e.response()?.errorBody()?.string()}")
        } catch (e: IOException) {
            Log.e("RemoteDatasource", "Network error while fetching addresses: ${e.message}", e)
            throw Exception("Network error: ${e.message}")
        }
    }

    @Throws(Throwable::class)
    suspend fun importAddresses(jsonContent: String, routeId: Int): ImportAddressesResult {
        val response = levkomService.importAddresses(jsonContent, routeId)
        if (response.isSuccessful) {
            return response.body() ?: throw Exception("No response body")
        } else {
            throw Exception("Failed to import addresses: ${response.errorBody()?.string()}")
        }
    }

    @Throws(Throwable::class)
    suspend fun deleteAddress(addressId: Int, routeId: Int): Boolean {
        try {
            val response = levkomService.deleteAddress(addressId, routeId)
            return response.isSuccessful
        } catch (e: IOException) {
            // Обработка ошибок связи
            throw Exception("Network error: ${e.message}", e)
        } catch (e: HttpException) {
            // Обработка ошибок HTTP
            throw Exception("HTTP error: ${e.response()?.errorBody()?.string()}", e)
        }
    }

    // Calculate route calculation
    suspend fun calculateRoute(routeId: Int): Boolean {
        return try {
            val response = levkomService.calculateRoute(routeId)
            response.isSuccessful
        } catch (e: Exception) {
            throw Exception("Error in calculating route: ${e.localizedMessage}", e)
        }
    }
    // Get geometry of the route
    suspend fun getRouteGeometryByRouteId(routeId: Int): String? {
        try {
            val response = levkomService.getRouteGeometryByRouteId(routeId)
            if (response.isSuccessful && response.body() != null) {
                return response.body()
            } else {
                System.out.println("Server responded with error code: ${response.code()}")
            }
        } catch (e: Exception) {
            System.out.println("Error getting route geometry: ${e.message}")
        }
        return null
    }

}
