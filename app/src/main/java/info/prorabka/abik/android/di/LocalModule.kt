package info.prorabka.abik.android.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import info.prorabka.abik.android.data.local.*
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LocalModule {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): AbikDatabase =
        Room.databaseBuilder(context, AbikDatabase::class.java, "abik-offline.db").build()

    @Provides
    fun offlineOperations(database: AbikDatabase): OfflineOperationDao = database.offlineOperations()

    @Provides
    fun cacheEntries(database: AbikDatabase): CacheEntryDao = database.cacheEntries()
}
