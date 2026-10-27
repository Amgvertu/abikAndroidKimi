package info.prorabka.abik.android.presentation.team

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import info.prorabka.abik.android.domain.model.TeamMember

/** Таблица свободных и занятых номеров (1–99) по ТЗ v3. */
@Composable
fun JerseyNumberDialog(
    member: TeamMember,
    taken: Map<Int, String>,
    onDismiss: () -> Unit,
    onSelect: (Int?) -> Unit,
) {
    var confirmFree by remember { mutableStateOf<Int?>(null) }
    var busyInfo by remember { mutableStateOf<Pair<Int, String>?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Номер: ${member.displayName}") },
        text = {
            Column {
                Text(
                    "Текущий: ${member.jerseyNumber ?: "не назначен"}",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.height(280.dp)) {
                    items((1..99).toList().chunked(6)) { rowNumbers ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            rowNumbers.forEach { number ->
                                val isTaken = number in taken
                                Text(
                                    text = number.toString(),
                                    modifier = Modifier
                                        .padding(6.dp)
                                        .clickable {
                                            if (isTaken) {
                                                busyInfo = number to taken.getValue(number)
                                            } else {
                                                confirmFree = number
                                            }
                                        },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = when {
                                        isTaken -> MaterialTheme.colorScheme.onSurfaceVariant
                                        else -> MaterialTheme.colorScheme.primary
                                    },
                                    fontWeight = if (isTaken) FontWeight.Normal else FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
                if (member.jerseyNumber != null) {
                    TextButton(onClick = { onSelect(null) }) {
                        Text("Снять номер", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Закрыть") }
        },
    )

    confirmFree?.let { number ->
        AlertDialog(
            onDismissRequest = { confirmFree = null },
            title = { Text("Назначить номер $number?") },
            text = { Text(member.displayName) },
            confirmButton = {
                TextButton(onClick = { onSelect(number) }) { Text("Назначить") }
            },
            dismissButton = {
                TextButton(onClick = { confirmFree = null }) { Text("Отмена") }
            },
        )
    }

    busyInfo?.let { (number, owner) ->
        AlertDialog(
            onDismissRequest = { busyInfo = null },
            title = { Text("Номер $number занят") },
            text = { Text("Сейчас под ним: $owner") },
            confirmButton = {
                TextButton(onClick = { busyInfo = null }) { Text("Понятно") }
            },
        )
    }
}
