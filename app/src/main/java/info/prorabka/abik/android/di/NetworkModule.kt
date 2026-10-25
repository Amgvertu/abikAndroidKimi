package info.prorabka.abik.android.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import info.prorabka.abik.android.BuildConfig
import info.prorabka.abik.android.core.network.AuthInterceptor
import info.prorabka.abik.android.core.network.DynamicBaseUrlInterceptor
import info.prorabka.abik.android.core.network.TokenAuthenticator
import info.prorabka.abik.android.data.api.AdminApi
import info.prorabka.abik.android.data.api.AuthApi
import info.prorabka.abik.android.data.api.BalanceApi
import info.prorabka.abik.android.data.api.ChargeApi
import info.prorabka.abik.android.data.api.FileApi
import info.prorabka.abik.android.data.api.GeoApi
import info.prorabka.abik.android.data.api.InvitationApi
import info.prorabka.abik.android.data.api.PaymentApi
import info.prorabka.abik.android.data.api.ReportApi
import info.prorabka.abik.android.data.api.TeamApi
import info.prorabka.abik.android.data.api.UserApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    @Provides
    @Singleton
    fun provideOkHttp(
        authInterceptor: AuthInterceptor,
        baseUrlInterceptor: DynamicBaseUrlInterceptor,
        tokenAuthenticator: TokenAuthenticator,
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(baseUrlInterceptor)
            .addInterceptor(authInterceptor)
            .authenticator(tokenAuthenticator)
        if (BuildConfig.DEBUG) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY },
            )
        }
        return builder.build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
        // Реальный URL подставляет DynamicBaseUrlInterceptor
        .baseUrl(BuildConfig.DEFAULT_BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideUserApi(retrofit: Retrofit): UserApi = retrofit.create(UserApi::class.java)

    @Provides
    @Singleton
    fun provideGeoApi(retrofit: Retrofit): GeoApi = retrofit.create(GeoApi::class.java)

    @Provides
    @Singleton
    fun provideTeamApi(retrofit: Retrofit): TeamApi = retrofit.create(TeamApi::class.java)

    @Provides
    @Singleton
    fun provideInvitationApi(retrofit: Retrofit): InvitationApi =
        retrofit.create(InvitationApi::class.java)

    @Provides
    @Singleton
    fun provideBalanceApi(retrofit: Retrofit): BalanceApi = retrofit.create(BalanceApi::class.java)

    @Provides
    @Singleton
    fun providePaymentApi(retrofit: Retrofit): PaymentApi = retrofit.create(PaymentApi::class.java)

    @Provides
    @Singleton
    fun provideChargeApi(retrofit: Retrofit): ChargeApi = retrofit.create(ChargeApi::class.java)

    @Provides
    @Singleton
    fun provideReportApi(retrofit: Retrofit): ReportApi = retrofit.create(ReportApi::class.java)

    @Provides
    @Singleton
    fun provideAdminApi(retrofit: Retrofit): AdminApi = retrofit.create(AdminApi::class.java)

    @Provides
    @Singleton
    fun provideFileApi(retrofit: Retrofit): FileApi = retrofit.create(FileApi::class.java)
}
