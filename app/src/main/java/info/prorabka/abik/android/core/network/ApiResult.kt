package info.prorabka.abik.android.core.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

@Serializable
data class ApiErrorDto(
    val status: Int? = null,
    val code: String? = null,
    val message: String? = null,
    val errors: List<FieldErrorDto>? = null,
)

@Serializable
data class FieldErrorDto(
    val field: String? = null,
    val message: String? = null,
)

sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>

    /** Ошибка сервера с разобранным телом ApiError. */
    data class HttpError(
        val httpCode: Int,
        val code: String?,
        val message: String,
        val fieldErrors: Map<String, String> = emptyMap(),
    ) : ApiResult<Nothing>

    /** Нет сети / таймаут / недоступен сервер. */
    data class NetworkError(val cause: String?) : ApiResult<Nothing>
}

val apiErrorJson: Json = Json { ignoreUnknownKeys = true }

suspend fun <T> apiCall(block: suspend () -> T): ApiResult<T> = try {
    ApiResult.Success(block())
} catch (e: HttpException) {
    val body = e.response()?.errorBody()?.string()
    val parsed = body?.let {
        runCatching { apiErrorJson.decodeFromString(ApiErrorDto.serializer(), it) }.getOrNull()
    }
    ApiResult.HttpError(
        httpCode = e.code(),
        code = parsed?.code,
        message = parsed?.message ?: "Ошибка сервера (${e.code()})",
        fieldErrors = parsed?.errors
            ?.mapNotNull { fe -> fe.field?.let { it to (fe.message ?: "") } }
            ?.toMap()
            .orEmpty(),
    )
} catch (e: IOException) {
    ApiResult.NetworkError(e.message)
}
