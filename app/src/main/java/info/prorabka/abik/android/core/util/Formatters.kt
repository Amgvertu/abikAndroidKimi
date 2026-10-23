package info.prorabka.abik.android.core.util

import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val rubFormatter: java.text.NumberFormat
    get() = java.text.NumberFormat.getIntegerInstance(Locale("ru", "RU"))

/** Копейки → "1 500 ₽" (суммы округлены до 100 ₽ по ТЗ, копейки не показываем). */
fun Long.kopecksToRubles(): String {
    val rubles = this / 100
    val sign = if (rubles < 0) "−" else ""
    return "$sign${rubFormatter.format(kotlin.math.abs(rubles))} ₽"
}

/** "2026-10-22" → "22.10.2026"; неразобранное возвращаем как есть. */
fun String.isoDateToDisplay(): String = runCatching {
    LocalDate.parse(this).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
}.getOrDefault(this)

/** "2026-10-22" → LocalDate или null. */
fun String.isoDateOrNull(): LocalDate? = runCatching { LocalDate.parse(this) }.getOrNull()

/** LocalDate → "2026-10-22" для API. */
fun LocalDate.toApiDate(): String = format(DateTimeFormatter.ISO_LOCAL_DATE)

/** ISO datetime → "22.10.2026 21:30". */
fun String.isoDateTimeToDisplay(): String = runCatching {
    OffsetDateTime.parse(this).format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
}.getOrDefault(this)

/** Телефон "+79001112233" → "+7 900 111-22-33". */
fun String.phoneToDisplay(): String {
    val digits = filter { it.isDigit() }
    if (digits.length == 11 && (digits.startsWith("7") || digits.startsWith("8"))) {
        val d = if (digits.startsWith("8")) "7" + digits.drop(1) else digits
        return "+7 ${d.substring(1, 4)} ${d.substring(4, 7)}-${d.substring(7, 9)}-${d.substring(9, 11)}"
    }
    return this
}
