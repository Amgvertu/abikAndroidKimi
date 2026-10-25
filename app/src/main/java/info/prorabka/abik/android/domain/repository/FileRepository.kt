package info.prorabka.abik.android.domain.repository

import android.net.Uri
import info.prorabka.abik.android.core.network.ApiResult

/** Загрузка файлов (аватар, логотип команды). */
interface FileRepository {
    /** Загружает файл, возвращает относительный URL вида /api/v1/files/xxx.webp. */
    suspend fun upload(uri: Uri): ApiResult<String>
}
