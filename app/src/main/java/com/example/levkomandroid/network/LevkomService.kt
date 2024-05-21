package com.example.levkomandroid.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*

private const val BASE_URL = "https://levkomwebapi.azurewebsites.net/"

// Initialize Moshi instance with KotlinJsonAdapterFactory for Kotlin class compatibility
private val moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

// Configure HttpLoggingInterceptor to log the full bodies of both requests and responses
private val loggingInterceptor = HttpLoggingInterceptor().apply {
    level = HttpLoggingInterceptor.Level.BODY
}

// Build the OkHttpClient and inject the logging interceptor and common headers
private val client = OkHttpClient.Builder()
    .addInterceptor(loggingInterceptor)
    .addInterceptor { chain ->
        val original = chain.request()
        // Append common headers to every request for consistency and debugging
        val requestBuilder = original.newBuilder()
            .header("Content-Type", "application/json")
            .header("User-Agent", "android")
            .header("Accept", "application/json")
            // Preserve the original request method and body
            .method(original.method, original.body)
        val request = requestBuilder.build()
        chain.proceed(request)
    }
    .build()

// Setup Retrofit with the Moshi converter and the custom OkHttpClient
private val retrofit = Retrofit.Builder()
    .baseUrl(BASE_URL)
    .client(client)
    .addConverterFactory(MoshiConverterFactory.create(moshi))
    .build()

// Define the interface for the Levkom Web API service
interface LevkomService {

    @POST("api/auth/login")
    fun login(@Body credentials: LoginRequest): Call<JWTokenResponse>

    @POST("auth/logout")
    fun logout(): Call<Void>

    @GET("user/details")
    fun getUserDetails(): Call<UserDetails>

    @GET("api/routeslist/GetRoutes")
    suspend fun getRoutes(): Response<List<RouteDto>>

    @DELETE("api/routeslist/DeleteRoute/{routeId}")
    fun deleteRoute(@Path("routeId") routeId: Int): Call<Void>

    @POST("api/roadgraph/CreateRoute")
    suspend fun createRoute(): Response<Int> // Assuming it returns an Int ID for the route

    @POST("api/RoadGraph/ImportSingleAddress")
    suspend fun importAddress(@Body address: Address, @Query("routeId") routeId: Int): Response<Void>

    @GET("api/roadgraph/GetAddressesByRouteIdWithOrder")
    suspend fun getAddressesByRouteIdWithOrder(@Query("routeId") routeId: Int): Response<List<DeliveryAddr>>
}

// Singleton object to access the retrofit service
object LevkomApi {
    val retrofitService: LevkomService by lazy {
        retrofit.create(LevkomService::class.java)
    }
}
