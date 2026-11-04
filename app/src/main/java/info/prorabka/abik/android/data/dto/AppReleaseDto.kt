package info.prorabka.abik.android.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class AppReleaseDto(
    val versionCode: Int,
    val versionName: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val mandatory: Boolean = false,
)
