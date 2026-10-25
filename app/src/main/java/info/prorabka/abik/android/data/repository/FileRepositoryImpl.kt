package info.prorabka.abik.android.data.repository

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.core.network.apiCall
import info.prorabka.abik.android.data.api.FileApi
import info.prorabka.abik.android.domain.repository.FileRepository
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileRepositoryImpl @Inject constructor(
    private val api: FileApi,
    @ApplicationContext private val context: Context,
) : FileRepository {

    override suspend fun upload(uri: Uri): ApiResult<String> = apiCall {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw java.io.IOException("Не удалось прочитать файл")
        val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
        val ext = when (mime) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
        val body = bytes.toRequestBody(mime.toMediaType())
        val part = MultipartBody.Part.createFormData("file", "upload.$ext", body)
        api.upload(part).url
    }
}
