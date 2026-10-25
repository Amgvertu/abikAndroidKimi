package info.prorabka.abik.android.core.network

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Превращает относительные URL файлов (/api/v1/files/xxx.webp)
 * в абсолютные на текущем сервере — для Coil.
 *
 * Сервер всегда возвращает канонический префикс /api/ (так ходит nginx на проде).
 * Текущий base URL уже содержит свой префикс (/api/ на проде, /api/abik/ локально),
 * поэтому канонический префикс отрезаем и подставляем базу целиком.
 */
@Singleton
class UrlResolver @Inject constructor(
    private val baseUrlHolder: BaseUrlHolder,
) {
    fun resolve(url: String?): String? {
        if (url.isNullOrBlank()) return null
        if (url.startsWith("http://") || url.startsWith("https://")) return url
        val base = baseUrlHolder.baseUrl.trimEnd('/')
        val path = url.removePrefix("/").removePrefix("api/")
        return "$base/$path"
    }
}
