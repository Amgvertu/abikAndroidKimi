package info.prorabka.abik.android.presentation.reports;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import info.prorabka.abik.android.core.CurrentTeamManager;
import info.prorabka.abik.android.domain.repository.ChargeRepository;
import info.prorabka.abik.android.domain.repository.ReportRepository;
import info.prorabka.abik.android.domain.repository.TeamRepository;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast"
})
public final class ReportsViewModel_Factory implements Factory<ReportsViewModel> {
  private final Provider<ReportRepository> reportRepositoryProvider;

  private final Provider<ChargeRepository> chargeRepositoryProvider;

  private final Provider<TeamRepository> teamRepositoryProvider;

  private final Provider<CurrentTeamManager> teamManagerProvider;

  public ReportsViewModel_Factory(Provider<ReportRepository> reportRepositoryProvider,
      Provider<ChargeRepository> chargeRepositoryProvider,
      Provider<TeamRepository> teamRepositoryProvider,
      Provider<CurrentTeamManager> teamManagerProvider) {
    this.reportRepositoryProvider = reportRepositoryProvider;
    this.chargeRepositoryProvider = chargeRepositoryProvider;
    this.teamRepositoryProvider = teamRepositoryProvider;
    this.teamManagerProvider = teamManagerProvider;
  }

  @Override
  public ReportsViewModel get() {
    return newInstance(reportRepositoryProvider.get(), chargeRepositoryProvider.get(), teamRepositoryProvider.get(), teamManagerProvider.get());
  }

  public static ReportsViewModel_Factory create(Provider<ReportRepository> reportRepositoryProvider,
      Provider<ChargeRepository> chargeRepositoryProvider,
      Provider<TeamRepository> teamRepositoryProvider,
      Provider<CurrentTeamManager> teamManagerProvider) {
    return new ReportsViewModel_Factory(reportRepositoryProvider, chargeRepositoryProvider, teamRepositoryProvider, teamManagerProvider);
  }

  public static ReportsViewModel newInstance(ReportRepository reportRepository,
      ChargeRepository chargeRepository, TeamRepository teamRepository,
      CurrentTeamManager teamManager) {
    return new ReportsViewModel(reportRepository, chargeRepository, teamRepository, teamManager);
  }
}
