package info.prorabka.abik.android.push

import android.content.Context
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import info.prorabka.abik.android.core.prefs.TokenStore
import info.prorabka.abik.android.domain.repository.DeviceRepository

/**
 * Ручная точка входа Hilt для push-сервисов.
 * @AndroidEntryPoint не используется: Hilt KSP не разрешает базовые классы
 * из RuStore/Huawei AAR, поэтому зависимости достаём вручную.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface PushEntryPoint {
    fun deviceRepository(): DeviceRepository
    fun ruStoreTokenProvider(): RuStoreTokenProvider
    fun huaweiTokenProvider(): HuaweiTokenProvider
    fun tokenStore(): TokenStore

    companion object {
        fun get(context: Context): PushEntryPoint =
            EntryPointAccessors.fromApplication(context, PushEntryPoint::class.java)
    }
}
