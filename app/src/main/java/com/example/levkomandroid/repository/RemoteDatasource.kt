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
        val call = levkomService.deleteRoute(routeId)
        val response = call.execute()
        return response.isSuccessful
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
}
