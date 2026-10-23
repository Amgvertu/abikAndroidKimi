package info.prorabka.abik.android.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import info.prorabka.abik.android.data.repository.AdminRepositoryImpl
import info.prorabka.abik.android.data.repository.AuthRepositoryImpl
import info.prorabka.abik.android.data.repository.ChargeRepositoryImpl
import info.prorabka.abik.android.data.repository.FinanceRepositoryImpl
import info.prorabka.abik.android.data.repository.GeoRepositoryImpl
import info.prorabka.abik.android.data.repository.ReportRepositoryImpl
import info.prorabka.abik.android.data.repository.TeamRepositoryImpl
import info.prorabka.abik.android.data.repository.UserRepositoryImpl
import info.prorabka.abik.android.domain.repository.AdminRepository
import info.prorabka.abik.android.domain.repository.AuthRepository
import info.prorabka.abik.android.domain.repository.ChargeRepository
import info.prorabka.abik.android.domain.repository.FinanceRepository
import info.prorabka.abik.android.domain.repository.GeoRepository
import info.prorabka.abik.android.domain.repository.ReportRepository
import info.prorabka.abik.android.domain.repository.TeamRepository
import info.prorabka.abik.android.domain.repository.UserRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    @Singleton
    abstract fun bindGeoRepository(impl: GeoRepositoryImpl): GeoRepository

    @Binds
    @Singleton
    abstract fun bindTeamRepository(impl: TeamRepositoryImpl): TeamRepository

    @Binds
    @Singleton
    abstract fun bindFinanceRepository(impl: FinanceRepositoryImpl): FinanceRepository

    @Binds
    @Singleton
    abstract fun bindChargeRepository(impl: ChargeRepositoryImpl): ChargeRepository

    @Binds
    @Singleton
    abstract fun bindReportRepository(impl: ReportRepositoryImpl): ReportRepository

    @Binds
    @Singleton
    abstract fun bindAdminRepository(impl: AdminRepositoryImpl): AdminRepository
}
