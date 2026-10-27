package info.prorabka.abik.android.core.util

import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val rubFormatter: java.text.NumberFormat
    get() = java.text.NumberFormat.getIntegerInstance(Locale("ru", "RU"))

/** Копейки → "1 500 ₽" (компактно, без копеек — для плашек в тулбаре). */
fun Long.kopecksToRubles(): String {
    val rubles = this / 100
    val sign = if (rubles < 0) "−" else ""
    return "$sign${rubFormatter.format(kotlin.math.abs(rubles))} ₽"
}

private val kopecksFormatter: java.text.DecimalFormat
    get() = java.text.DecimalFormat("#,##0.00", java.text.DecimalFormatSymbols(Locale("ru", "RU")))

/** Копейки → точная сумма: "1 000,50 ₽"; если копеек нет — "1 000 ₽". */
fun Long.kopecksToRublesExact(): String {
    val sign = if (this < 0) "−" else ""
    val abs = kotlin.math.abs(this)
    val rubles = abs / 100
    val kopecks = abs % 100
    return if (kopecks == 0L) {
        "$sign${rubFormatter.format(rubles)} ₽"
    } else {
        "$sign${rubFormatter.format(rubles)},${kopecks.toString().padStart(2, '0')} ₽"
    }
}

/** Копейки → строка для поля ввода: "1000" или "1000.50". */
fun Long.kopecksToInput(): String =
    if (this % 100 == 0L) (this / 100).toString()
    else "${this / 100}.${(this % 100).toString().padStart(2, '0')}"

/** Фильтр ввода денежной суммы: цифры и один разделитель (. или ,), до 2 знаков после. */
fun String.filterMoneyInput(): String {
    var sepSeen = false
    var decimals = 0
    val out = StringBuilder()
    for (ch in this) {
        when {
            ch.isDigit() -> {
                if (sepSeen) {
                    if (decimals >= 2) continue
                    decimals++
                }
                out.append(ch)
            }
            (ch == '.' || ch == ',') && !sepSeen && out.isNotEmpty() -> {
                sepSeen = true
                out.append('.')
            }
        }
    }
    return out.toString().take(12)
}

/** "1000.50" / "1000,5" → копейки; null при мусоре. */
fun String.parseMoneyToKopecks(): Long? = runCatching {
    val normalized = trim().replace(',', '.')
    if (normalized.isEmpty()) return null
    java.math.BigDecimal(normalized)
        .setScale(2, java.math.RoundingMode.DOWN)
        .movePointRight(2)
        .longValueExact()
}.getOrNull()

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
