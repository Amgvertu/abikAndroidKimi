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
}
