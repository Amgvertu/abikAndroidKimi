package info.prorabka.abik.android.domain.model

data class AppRelease(
    val versionCode: Int,
    val versionName: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val mandatory: Boolean,
)
