package info.prorabka.abik.android.data.api

import info.prorabka.abik.android.data.dto.NotificationDto
import info.prorabka.abik.android.data.dto.UnreadCountResponse
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface NotificationApi {

    @GET("v1/notifications")
    suspend fun list(@Query("onlyUnread") onlyUnread: Boolean = false): List<NotificationDto>

    @GET("v1/notifications/unread-count")
    suspend fun unreadCount(): UnreadCountResponse

    @POST("v1/notifications/{id}/read")
    suspend fun markRead(@Path("id") id: Long): NotificationDto

    @POST("v1/notifications/read-all")
    suspend fun markAllRead(): Map<String, Int>
}
