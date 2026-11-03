package info.prorabka.abik.android.core.util

/**
 * Нормализация телефона под серверный формат: "+7XXXXXXXXXX".
 * Сервер принимает E.164 или 10-15 цифр; приводим ввод пользователя к "+7...".
 */
object PhoneUtils {

    /** Возвращает нормализованный телефон или null, если ввод не похож на номер. */
    fun normalize(input: String): String? {
        var digits = input.filter { it.isDigit() }
        if (digits.isEmpty()) return null
        if (digits.length == 10) digits = "7$digits"            // 9001112233
        if (digits.length == 11 && digits.startsWith("8")) {    // 89001112233
            digits = "7" + digits.drop(1)
        }
        return if (digits.length in 11..15) "+$digits" else null
    }

    fun isValid(input: String): Boolean = normalize(input) != null

    /** Цифры национального номера для редактирования; код страны добавляет маска. */
    fun inputDigits(input: String): String {
        val digits = input.filter(Char::isDigit)
        return if (digits.length > 10 && (digits.startsWith("7") || digits.startsWith("8"))) {
            digits.drop(1).take(10)
        } else {
            digits.take(10)
        }
    }

    /** Маска отображения: +7 (xxx) xxx xxxx. На сервер по-прежнему уходит normalize(). */
    fun formatInput(input: String): String {
        if (input.isEmpty()) return ""
        var digits = input.filter(Char::isDigit)
        if (digits.startsWith("8")) digits = "7" + digits.drop(1)
        if (!digits.startsWith("7")) digits = "7$digits"
        val national = digits.drop(1).take(10)
        return buildString {
            append("+7")
            if (national.isNotEmpty()) {
                append(" (")
                append(national.take(3))
                if (national.length >= 3) {
                    append(") ")
                    append(national.drop(3).take(3))
                    if (national.length > 6) {
                        append(' ')
                        append(national.drop(6).take(4))
                    }
                }
            }
        }
    }
}
