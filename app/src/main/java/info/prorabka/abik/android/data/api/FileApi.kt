package info.prorabka.abik.android.data.api

import info.prorabka.abik.android.data.dto.FileUploadResponseDto
import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface FileApi {

    @Multipart
    @POST("v1/files")
    suspend fun upload(@Part file: MultipartBody.Part): FileUploadResponseDto
}
