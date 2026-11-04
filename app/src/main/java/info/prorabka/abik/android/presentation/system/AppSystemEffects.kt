package info.prorabka.abik.android.presentation.system

import android.content.Intent
import android.net.Uri
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun AppSystemEffects(viewModel: AppSystemViewModel = hiltViewModel()) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val update by viewModel.update.collectAsState()
    val context = LocalContext.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.onStart()
                Lifecycle.Event.ON_STOP -> viewModel.onStop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    update?.let { release ->
        AlertDialog(
            onDismissRequest = viewModel::dismissUpdate,
            title = { Text("Доступна версия ${release.versionName}") },
            text = { Text(release.releaseNotes) },
            confirmButton = {
                TextButton(onClick = {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(release.downloadUrl)))
                    }
                    viewModel.dismissUpdate()
                }) { Text("Скачать") }
            },
            dismissButton = if (release.mandatory) null else {
                { TextButton(onClick = viewModel::dismissUpdate) { Text("Позже") } }
            },
        )
    }
}
