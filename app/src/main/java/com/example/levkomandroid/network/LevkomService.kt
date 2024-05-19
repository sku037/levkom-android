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


private val moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

// Create a HttpLoggingInterceptor and set its level to BODY
private val loggingInterceptor = HttpLoggingInterceptor().apply {
    level = HttpLoggingInterceptor.Level.BODY
}

// Create an OkHttpClient and add the loggingInterceptor to it
private val client = OkHttpClient.Builder()
    .addInterceptor(loggingInterceptor)
    .addInterceptor { chain ->
        val original = chain.request()
        val request = original.newBuilder()
            .header("Content-Type", "application/json")
            .header("User-Agent", "android")
            .header("Accept", "application/json")
            // Используем значения original.method и original.body вместо методов
            .method(original.method, original.body)
            .build()
        chain.proceed(request)
    }
    .build()

private val retrofit = Retrofit.Builder()
    .addConverterFactory(MoshiConverterFactory.create(moshi))
    .baseUrl(BASE_URL)
    .client(client)
    .build()

interface LevkomService {

    @POST("api/auth/login")
    fun login(@Body credentials: LoginRequest): Call<JWTokenResponse>

    @POST("auth/logout")
    fun logout(): Call<Void>

    @GET("user/details")
    fun getUserDetails(): Call<UserDetails>

    @GET("api/routeslist/GetRoutes")
    suspend fun getRoutes(): List<RouteDto>

    @DELETE("api/routeslist/DeleteRoute/{routeId}")
    fun deleteRoute(@Path("routeId") routeId: Int): Call<Void>

    @POST("api/roadgraph/CreateRoute")
    suspend fun createRoute(): Response<Int> // Assuming it returns an Int ID

    @POST("api/RoadGraph/ImportSingleAddress")
    suspend fun importAddress(@Body address: Address, @Query("routeId") routeId: Int): Response<Void>

    @GET("api/RoadGraph/GetAddressesByRouteIdWithOrder")
    suspend fun getAddressesByRouteIdWithOrder(@Query("routeId") routeId: Int): Response<List<DeliveryAddr>>

}

object LevkomApi {
    val retrofitService : LevkomService by lazy {
        retrofit.create(LevkomService::class.java) }
}