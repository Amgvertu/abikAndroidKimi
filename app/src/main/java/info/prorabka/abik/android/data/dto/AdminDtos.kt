package info.prorabka.abik.android.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class AdminUserDto(
    val id: Long,
    val phone: String,
    val status: String,
    val isSuperAdmin: Boolean = false,
    val createdAt: String? = null,
)

@Serializable
data class CreateRegionRequestDto(
    val name: String,
    val code: String,
    val timezone: String,
)

@Serializable
data class CreateCityRequestDto(
    val regionId: Long,
    val name: String,
)
