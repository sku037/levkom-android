package com.example.levkomandroid.network

import com.squareup.moshi.Json

data class LoginRequest(
    @Json(name = "Email") val username: String,
    @Json(name = "Password") val password: String
)