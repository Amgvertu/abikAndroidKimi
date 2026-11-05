package info.prorabka.abik.android.data.repository

import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.core.network.apiCall
import info.prorabka.abik.android.data.api.UserApi
import info.prorabka.abik.android.data.dto.PhoneChangeConfirmRequest
import info.prorabka.abik.android.data.dto.PhoneChangeSendNewRequest
import info.prorabka.abik.android.data.dto.SmsCodeRequest
import info.prorabka.abik.android.data.dto.UserProfileRequest
import info.prorabka.abik.android.data.dto.UserProfileResponse
import info.prorabka.abik.android.domain.model.PlayerStatus
import info.prorabka.abik.android.domain.model.Position
import info.prorabka.abik.android.domain.model.Profile
import info.prorabka.abik.android.domain.model.StickHand
import info.prorabka.abik.android.domain.model.User
import info.prorabka.abik.android.domain.repository.UserRepository
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val api: UserApi,
    private val cache: info.prorabka.abik.android.data.local.CacheStore,
    private val tokenStore: info.prorabka.abik.android.core.prefs.TokenStore,
) : UserRepository {

    override suspend fun me(): ApiResult<User> {
        val key = "${tokenStore.userId}:me"
        val serializer = info.prorabka.abik.android.data.dto.UserDto.serializer()
        return when (val result = apiCall { api.me() }) {
            is ApiResult.Success -> {
                cache.write(key, serializer, result.data)
                ApiResult.Success(result.data.toDomain())
            }
            is ApiResult.NetworkError -> {
                val cached = cache.read(key, serializer)
                if (cached != null) ApiResult.Success(cached.toDomain()) else result
            }
            is ApiResult.HttpError -> result
        }
    }

    override suspend fun myProfile(): ApiResult<Profile> {
        val key = "${tokenStore.userId}:profile:me"
        val serializer = UserProfileResponse.serializer()
        return when (val result = apiCall { api.myProfile() }) {
            is ApiResult.Success -> {
                cache.write(key, serializer, result.data)
                ApiResult.Success(result.data.toDomain())
            }
            is ApiResult.NetworkError -> {
                val cached = cache.read(key, serializer)
                if (cached != null) ApiResult.Success(cached.toDomain()) else result
            }
            is ApiResult.HttpError -> result
        }
    }

    override suspend fun profileOf(userId: Long): ApiResult<Profile> {
        val key = "${tokenStore.userId}:profile:$userId"
        val serializer = UserProfileResponse.serializer()
        return when (val result = apiCall { api.profileOfUser(userId) }) {
            is ApiResult.Success -> {
                cache.write(key, serializer, result.data)
                ApiResult.Success(result.data.toDomain())
            }
            is ApiResult.NetworkError -> {
                val cached = cache.read(key, serializer)
                if (cached != null) ApiResult.Success(cached.toDomain()) else result
            }
            is ApiResult.HttpError -> result
        }
    }

    override suspend fun updateMyProfile(
        cityId: Long,
        lastName: String,
        firstName: String,
        middleName: String,
        birthDateIso: String,
        heightCm: Int,
        weightKg: Int,
        stickHand: StickHand,
        positions: Set<Position>,
        playerStatus: PlayerStatus,
        jerseySize: String?,
        coversSize: String?,
        gaitersSize: String?,
        avatarUrl: String?,
    ): ApiResult<Profile> = apiCall {
        api.updateMyProfile(
            UserProfileRequest(
                cityId = cityId,
                lastName = lastName,
                firstName = firstName,
                middleName = middleName,
                birthDate = birthDateIso,
                heightCm = heightCm,
                weightKg = weightKg,
                stickHand = stickHand.name,
                positions = positions.map { it.name }.toSet(),
                playerStatus = playerStatus.name,
                jerseySize = jerseySize,
                coversSize = coversSize,
                gaitersSize = gaitersSize,
                avatarUrl = avatarUrl,
            ),
        ).toDomain()
    }

    override suspend fun phoneChangeStart(): ApiResult<Unit> = apiCall {
        api.phoneChangeStart()
        Unit
    }

    override suspend fun phoneChangeVerifyOld(code: String): ApiResult<String> = apiCall {
        api.phoneChangeVerifyOld(SmsCodeRequest(code)).changeToken
    }

    override suspend fun phoneChangeSendNew(changeToken: String, newPhone: String): ApiResult<Unit> =
        apiCall {
            api.phoneChangeSendNew(PhoneChangeSendNewRequest(changeToken, newPhone))
            Unit
        }

    override suspend fun phoneChangeConfirm(changeToken: String, code: String): ApiResult<Unit> =
        apiCall {
            api.phoneChangeConfirm(PhoneChangeConfirmRequest(changeToken, code))
            Unit
        }
}

private fun info.prorabka.abik.android.data.dto.UserDto.toDomain(): User =
    User(id, phone, status, isSuperAdmin)

fun UserProfileResponse.toDomain(): Profile = Profile(
    userId = userId,
    cityId = cityId,
    cityName = cityName ?: "",
    regionId = regionId,
    regionName = regionName,
    lastName = lastName,
    firstName = firstName,
    middleName = middleName,
    birthDate = runCatching { LocalDate.parse(birthDate) }.getOrNull(),
    heightCm = heightCm,
    weightKg = weightKg,
    stickHand = runCatching { StickHand.valueOf(stickHand) }.getOrDefault(StickHand.RIGHT),
    positions = positions.mapNotNull { runCatching { Position.valueOf(it) }.getOrNull() }.toSet(),
    playerStatus = runCatching { PlayerStatus.valueOf(playerStatus) }.getOrDefault(PlayerStatus.AMATEUR),
    jerseySize = jerseySize,
    coversSize = coversSize,
    gaitersSize = gaitersSize,
    avatarUrl = avatarUrl,
)
