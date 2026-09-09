package com.orderflow.autoresponder.data.remote.dto

import com.google.gson.annotations.SerializedName

// ── /me ─────────────────────────────────────────────────────────────────────

data class MetaMeResponse(
    @SerializedName("id")   val id: String = "",
    @SerializedName("name") val name: String = ""
)

// ── /me/phone_numbers ────────────────────────────────────────────────────────

data class MetaPhoneNumbersResponse(
    @SerializedName("data") val data: List<MetaPhoneNumberDto> = emptyList()
)

data class MetaPhoneNumberDto(
    @SerializedName("id")                        val id: String = "",
    @SerializedName("display_phone_number")      val displayPhoneNumber: String = "",
    @SerializedName("verified_name")             val verifiedName: String = "",
    @SerializedName("code_verification_status")  val codeVerificationStatus: String = ""
)

// ── /me/businesses ───────────────────────────────────────────────────────────

data class MetaBusinessesResponse(
    @SerializedName("data") val data: List<MetaBusinessDto> = emptyList()
)

data class MetaBusinessDto(
    @SerializedName("id")   val id: String = "",
    @SerializedName("name") val name: String = ""
)

// ── Error response ───────────────────────────────────────────────────────────

data class MetaApiErrorResponse(
    @SerializedName("error") val error: MetaApiError? = null
)

data class MetaApiError(
    @SerializedName("message")    val message: String = "",
    @SerializedName("type")       val type: String = "",
    @SerializedName("code")       val code: Int = 0
)
