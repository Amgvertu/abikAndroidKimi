package info.prorabka.abik.android.presentation.subscription;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import info.prorabka.abik.android.core.CurrentTeamManager;
import info.prorabka.abik.android.domain.repository.FinanceRepository;
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
public final class SubscriptionViewModel_Factory implements Factory<SubscriptionViewModel> {
  private final Provider<CurrentTeamManager> teamManagerProvider;

  private final Provider<TeamRepository> teamRepositoryProvider;

  private final Provider<FinanceRepository> financeRepositoryProvider;

  public SubscriptionViewModel_Factory(Provider<CurrentTeamManager> teamManagerProvider,
      Provider<TeamRepository> teamRepositoryProvider,
      Provider<FinanceRepository> financeRepositoryProvider) {
    this.teamManagerProvider = teamManagerProvider;
    this.teamRepositoryProvider = teamRepositoryProvider;
    this.financeRepositoryProvider = financeRepositoryProvider;
  }

  @Override
  public SubscriptionViewModel get() {
    return newInstance(teamManagerProvider.get(), teamRepositoryProvider.get(), financeRepositoryProvider.get());
  }

  public static SubscriptionViewModel_Factory create(
      Provider<CurrentTeamManager> teamManagerProvider,
      Provider<TeamRepository> teamRepositoryProvider,
      Provider<FinanceRepository> financeRepositoryProvider) {
    return new SubscriptionViewModel_Factory(teamManagerProvider, teamRepositoryProvider, financeRepositoryProvider);
  }

  public static SubscriptionViewModel newInstance(CurrentTeamManager teamManager,
      TeamRepository teamRepository, FinanceRepository financeRepository) {
    return new SubscriptionViewModel(teamManager, teamRepository, financeRepository);
  }
}
