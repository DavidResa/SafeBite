package com.example.safebite.data.api

import retrofit2.http.GET
import retrofit2.http.Path

interface OpenFoodFactsApi {
    @GET("api/v2/product/{barcode}.json")
    suspend fun getProduct(@Path("barcode") barcode: String): ProductResponse

    companion object {
        const val BASE_URL = "https://world.openfoodfacts.org/"
    }
}
