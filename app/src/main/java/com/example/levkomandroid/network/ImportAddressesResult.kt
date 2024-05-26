package com.example.levkomandroid.network

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

@Parcelize
@JsonClass(generateAdapter = true)
data class ImportAddressesResult(
    @Json(name = "success") val success: String?,
    @Json(name = "failed") val failed: String?,
    @Json(name = "failedAddresses") val failedAddresses: List<Address>?
): Parcelable