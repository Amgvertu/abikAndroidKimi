package info.prorabka.abik.android.domain.repository

import android.net.Uri
import info.prorabka.abik.android.core.network.ApiResult

enum class FilePurpose {
    GENERAL,
    AVATAR,
}

/** Загрузка файлов (аватар, логотип команды). */
interface FileRepository {
    /** Загружает файл, возвращает относительный URL вида /api/v1/files/xxx.jpg. */
    suspend fun upload(uri: Uri, purpose: FilePurpose = FilePurpose.GENERAL): ApiResult<String>
}
