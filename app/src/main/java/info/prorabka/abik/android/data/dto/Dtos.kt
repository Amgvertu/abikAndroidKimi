package info.prorabka.abik.android.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegisterStartRequest(
    val phone: String,
    val password: String,
    val acceptedPd: Boolean,
    val policyVersion: String,
)

@Serializable
data class RegisterStartResponse(
    val phone: String,
    val message: String? = null,
)

@Serializable
data class RegisterConfirmRequest(
    val phone: String,
    val code: String,
)

@Serializable
data class LoginRequest(
    val phone: String,
    val password: String,
)

@Serializable
data class RefreshRequest(val refreshToken: String)

@Serializable
data class LogoutRequest(val refreshToken: String)

@Serializable
data class TokenPairDto(
    val accessToken: String,
    val refreshToken: String,
    val accessExpiresAt: String? = null,
    val refreshExpiresAt: String? = null,
)

@Serializable
data class AuthResponse(
    val tokens: TokenPairDto,
    val user: UserDto,
)

@Serializable
data class UserDto(
    val id: Long,
    val phone: String,
    val status: String,
    val isSuperAdmin: Boolean = false,
    val createdAt: String? = null,
)

@Serializable
data class MessageResponse(val message: String? = null)

// --- Восстановление пароля / смена номера (backend-api-extension.md, раздел 2а) ---

@Serializable
data class PhoneRequest(val phone: String)

@Serializable
data class PasswordResetConfirmRequest(
    val phone: String,
    val code: String,
    val newPassword: String,
)

@Serializable
data class SmsCodeRequest(val code: String)

@Serializable
data class PhoneChangeTokenResponse(val changeToken: String)

@Serializable
data class PhoneChangeSendNewRequest(
    val changeToken: String,
    val newPhone: String,
)

@Serializable
data class PhoneChangeConfirmRequest(
    val changeToken: String,
    val code: String,
)

// --- Профиль ---

@Serializable
data class UserProfileRequest(
    val cityId: Long,
    val lastName: String,
    val firstName: String,
    val middleName: String,
    val birthDate: String,
    val heightCm: Int,
    val weightKg: Int,
    val stickHand: String,
    val positions: Set<String>,
    val jerseySize: String? = null,
    val coversSize: String? = null,
    val gaitersSize: String? = null,
    val avatarUrl: String? = null,
)

@Serializable
data class UserProfileResponse(
    val userId: Long,
    val cityId: Long,
    val cityName: String? = null,
    val regionId: Long? = null,
    val regionName: String? = null,
    val lastName: String,
    val firstName: String,
    val middleName: String,
    val birthDate: String,
    val heightCm: Int,
    val weightKg: Int,
    val stickHand: String,
    val positions: Set<String> = emptySet(),
    val jerseySize: String? = null,
    val coversSize: String? = null,
    val gaitersSize: String? = null,
    val avatarUrl: String? = null,
    val updatedAt: String? = null,
)

// --- Гео ---

@Serializable
data class RegionDto(val id: Long, val name: String, val code: String? = null)

@Serializable
data class CityDto(val id: Long, val regionId: Long, val name: String)

// --- Команды ---

@Serializable
data class TeamDto(
    val id: Long,
    val name: String,
    val cityId: Long? = null,
    val cityName: String? = null,
    val homeRegionId: Long? = null,
    val homeRegionName: String? = null,
    val logoUrl: String? = null,
    val status: String,
    val createdAt: String? = null,
)
