package com.example.safebite.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProductResponse(
    val code: String? = null,
    val product: Product? = null,
    val status: Int? = null,
    @SerialName("status_verbose") val statusVerbose: String? = null
)

@Serializable
data class Product(
    @SerialName("product_name") val productName: String? = null,
    @SerialName("ingredients_text") val ingredientsText: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    val brands: String? = null,
    val quantity: String? = null,
    @SerialName("ingredients_analysis_tags") val ingredientsAnalysisTags: List<String>? = null,
    val allergens: String? = null,
    @SerialName("allergens_tags") val allergensTags: List<String>? = null
)
