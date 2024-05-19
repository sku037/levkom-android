package com.example.levkomandroid.network

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

@Parcelize
@JsonClass(generateAdapter = true)
data class DeliveryAddr(
    @Json(name = "id") val id: Int,
    @Json(name = "latitude") val latitude: Double,
    @Json(name = "longitude") val longitude: Double,
    @Json(name = "osmId") val osmId: Long?,
    @Json(name = "post") val post: String,
    @Json(name = "city") val city: String,
    @Json(name = "street") val street: String,
    @Json(name = "hnr") val hnr: String
): Parcelable