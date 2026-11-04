package info.prorabka.abik.android.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.core.network.apiCall
import info.prorabka.abik.android.data.api.FileApi
import info.prorabka.abik.android.domain.repository.FileRepository
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.math.max
import kotlin.math.roundToInt
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileRepositoryImpl @Inject constructor(
    private val api: FileApi,
    @ApplicationContext private val context: Context,
) : FileRepository {

    override suspend fun upload(uri: Uri): ApiResult<String> = apiCall {
        val source = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IOException("Не удалось прочитать файл")
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(source, 0, source.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw IOException("Выберите изображение в формате JPEG, PNG или WebP")
        }

        val options = BitmapFactory.Options().apply {
            inSampleSize = max(1, max(bounds.outWidth, bounds.outHeight) / 2048)
        }
        val decoded = BitmapFactory.decodeByteArray(source, 0, source.size, options)
            ?: throw IOException("Не удалось обработать изображение")
        val scale = minOf(1f, 1600f / max(decoded.width, decoded.height))
        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                decoded,
                (decoded.width * scale).roundToInt().coerceAtLeast(1),
                (decoded.height * scale).roundToInt().coerceAtLeast(1),
                true,
            )
        } else {
            decoded
        }

        // JPEG с белой подложкой одинаково поддерживается сервером и любыми
        // галереями Android; размер фотографии телефона заметно уменьшается.
        val flattened = Bitmap.createBitmap(scaled.width, scaled.height, Bitmap.Config.ARGB_8888)
        Canvas(flattened).apply {
            drawColor(Color.WHITE)
            drawBitmap(scaled, 0f, 0f, null)
        }
        val output = ByteArrayOutputStream()
        if (!flattened.compress(Bitmap.CompressFormat.JPEG, 85, output)) {
            throw IOException("Не удалось подготовить изображение")
        }
        val body = output.toByteArray().toRequestBody("image/jpeg".toMediaType())
        val part = MultipartBody.Part.createFormData("file", "upload.jpg", body)
        api.upload(part).url
    }
}
