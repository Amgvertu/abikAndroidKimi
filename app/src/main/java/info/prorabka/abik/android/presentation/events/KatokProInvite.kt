package info.prorabka.abik.android.presentation.events

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import info.prorabka.abik.android.R
import info.prorabka.abik.android.core.util.KatokProLauncher

/** Открывает Katok.pro или его страницу в RuStore, если приложение не установлено. */
@Composable
fun KatokProInviteButton(modifier: Modifier = Modifier, compact: Boolean = false) {
    val context = LocalContext.current
    TextButton(
        onClick = { KatokProLauncher.open(context) },
        modifier = modifier,
    ) {
        if (compact) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End,
            ) {
                KatokProLogo()
                Spacer(Modifier.height(4.dp))
                Text(
                    "Пригласить\nвратаря/полевого",
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                    fontSize = 12.sp,
                    lineHeight = 14.sp,
                    textAlign = TextAlign.End,
                )
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                KatokProLogo()
                Spacer(Modifier.width(8.dp))
                Text("Пригласить вратаря/полевого", maxLines = 2)
            }
        }
    }
}

@Composable
private fun KatokProLogo() {
    Image(
        painter = painterResource(R.drawable.katok_pro_logo),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White),
    )
}
