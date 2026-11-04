package info.prorabka.abik.android.core.util

/** Форматирование ФИО только для отображения; исходные данные профиля не изменяются. */
object NameFormatter {
    /** Возвращает «Фамилия И.». Телефон, user#id и другие однословные значения сохраняет. */
    fun short(fullName: String): String {
        val parts = fullName.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (parts.isEmpty()) return fullName.trim()
        val initial = parts.getOrNull(1)?.firstOrNull()?.uppercaseChar() ?: return parts[0]
        return "${parts[0]} $initial."
    }
}
