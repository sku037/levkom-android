package com.example.levkomandroid.network

import com.squareup.moshi.Json

data class UserDetails(
    @Json(name = "id") val id: String,
    @Json(name = "displayName") val displayName: String,
    @Json(name = "email") val email: String
)