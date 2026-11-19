package info.prorabka.abik.android.presentation.shell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import info.prorabka.abik.android.core.util.isoDateToDisplay
import info.prorabka.abik.android.domain.model.Invitation

/** Общий диалог входящих приглашений для всех основных разделов. */
@Composable
fun InvitationsDialog(
    invitations: List<Invitation>,
    saving: Boolean,
    error: String?,
    onAccept: (Invitation) -> Unit,
    onDecline: (Invitation) -> Unit,
) {
    if (invitations.isEmpty()) return
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
        title = { Text("Приглашения в команды") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                invitations.forEachIndexed { index, invitation ->
                    if (index > 0) HorizontalDivider()
                    Text(invitation.teamName, style = MaterialTheme.typography.titleMedium)
                    invitation.expiresAt?.take(10)?.let { date ->
                        Text(
                            "Действует до ${date.isoDateToDisplay()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = { onDecline(invitation) }, enabled = !saving) {
                            Text("Отклонить", color = MaterialTheme.colorScheme.error)
                        }
                        TextButton(onClick = { onAccept(invitation) }, enabled = !saving) {
                            Text(if (saving) "Сохраняем…" else "Принять")
                        }
                    }
                }
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {},
    )
}
