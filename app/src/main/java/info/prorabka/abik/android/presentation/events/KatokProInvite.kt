package info.prorabka.abik.android.presentation.events

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import info.prorabka.abik.android.R
import info.prorabka.abik.android.core.util.KatokProLauncher

/** Открывает Katok.pro или его страницу в RuStore, если приложение не установлено. */
@Composable
fun KatokProInviteButton(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    TextButton(
        onClick = { KatokProLauncher.open(context) },
        modifier = modifier,
    ) {
        Image(
            painter = painterResource(R.drawable.katok_pro_logo),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White),
        )
        Spacer(Modifier.width(8.dp))
        Text("Пригласить вратаря/игроков", maxLines = 2)
    }
}
