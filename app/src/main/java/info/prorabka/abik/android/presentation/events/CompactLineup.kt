package info.prorabka.abik.android.presentation.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import info.prorabka.abik.android.core.util.NameFormatter
import info.prorabka.abik.android.domain.model.LineupRole
import info.prorabka.abik.android.domain.model.LineupSlot
import info.prorabka.abik.android.domain.model.SweaterColor

/**
 * Компактная схема звена: три равные колонки нападающих и две колонки
 * защитников шириной 1,5 колонки нападающего. Позиции сохраняются даже
 * при неполном звене, поэтому центральный нападающий остаётся по центру.
 */
@Composable
fun CompactLineup(
    slots: List<LineupSlot>,
    names: Map<Long, String>,
    lineColors: Map<Int, SweaterColor> = emptyMap(),
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        slots.filter { it.role == LineupRole.GOALKEEPER }
            .sortedBy { it.slotNumber }
            .forEach { slot ->
                Text(
                    "Вратарь: ${playerName(slot.userId, names)}",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

        slots.filter { it.role != LineupRole.GOALKEEPER }
            .groupBy { it.lineNumber ?: 0 }
            .toSortedMap()
            .forEach { (lineNumber, lineSlots) ->
                Spacer(Modifier.height(8.dp))
                val colorTitle = lineColors[lineNumber]?.let { " • ${it.display}" }.orEmpty()
                Text(
                    "Звено $lineNumber$colorTitle",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    for (slotNumber in 1..3) {
                        LineupNameCell(
                            slot = lineSlots.firstOrNull {
                                it.role == LineupRole.FORWARD && it.slotNumber == slotNumber
                            },
                            names = names,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    for (slotNumber in 1..2) {
                        LineupNameCell(
                            slot = lineSlots.firstOrNull {
                                it.role == LineupRole.DEFENDER && it.slotNumber == slotNumber
                            },
                            names = names,
                            modifier = Modifier.weight(1.5f),
                        )
                    }
                }
            }
    }
}

@Composable
private fun LineupNameCell(
    slot: LineupSlot?,
    names: Map<Long, String>,
    modifier: Modifier,
) {
    Box(modifier.padding(horizontal = 2.dp), contentAlignment = Alignment.Center) {
        if (slot != null) {
            Text(
                playerName(slot.userId, names),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun playerName(userId: Long, names: Map<Long, String>): String =
    names[userId]?.takeIf { it.isNotBlank() }?.let(NameFormatter::short) ?: "user#$userId"
