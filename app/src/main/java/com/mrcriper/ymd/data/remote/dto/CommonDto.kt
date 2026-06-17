package com.mrcriper.ymd.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class InvitationDto(
    val operationId: String? = null,
    val error: String? = null,
    val errorDescription: String? = null,
    val status: String? = null,
)

@Serializable
data class ApiErrorDto(
    val name: String? = null,
    val message: String? = null,
    val error: String? = null,
)

@Serializable
data class CoverSizeDto(
    val width: Int? = null,
    val height: Int? = null,
    val uri: String? = null,
)
