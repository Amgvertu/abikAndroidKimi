package info.prorabka.abik.android.domain.model

enum class ChargeStatus(val display: String) {    ACTIVE("Активно"),
    PARTIALLY_PAID("Частично оплачено"),
    PAID("Оплачено"),
    CANCELLED("Отменено"),
    ARCHIVED("В архиве"),
}

data class Charge(
    val id: Long,
    val groupName: String,
    val subgroupName: String?,
    val userId: Long,
    val userName: String,
    val amountKopecks: Long,
    val paidKopecks: Long,
    val remainingKopecks: Long,
    val status: ChargeStatus,
    val description: String?,
    val dueDate: String?,
    val createdAt: String?,
) {
    val title: String
        get() = listOfNotNull(groupName.ifBlank { null }, subgroupName).joinToString(" / ")
}

data class ChargeGroup(val id: Long, val name: String)

data class ChargeSubgroup(val id: Long, val groupId: Long, val name: String)

/** Способ распределения суммы шаблона. */
enum class ChargeDistribution(val display: String) {
    EQUAL("Разделить поровну"),
    FIXED("Фиксированная сумма с каждого"),
}

/** Шаблон систематического (ежемесячного) начисления. */
data class ChargeTemplate(
    val id: Long,
    val groupId: Long,
    val subgroupId: Long?,
    val name: String,
    val amountKopecks: Long,
    val type: String,
    val periodicity: String,
    val dayOfMonth: Int?,
    val distribution: ChargeDistribution,
    val active: Boolean,
    val memberIds: Set<Long>,
)
