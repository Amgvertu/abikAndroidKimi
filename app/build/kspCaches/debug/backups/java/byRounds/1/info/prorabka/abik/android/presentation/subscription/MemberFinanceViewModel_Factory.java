package info.prorabka.abik.android.presentation.subscription;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import info.prorabka.abik.android.core.CurrentTeamManager;
import info.prorabka.abik.android.domain.repository.ChargeRepository;
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
public final class MemberFinanceViewModel_Factory implements Factory<MemberFinanceViewModel> {
  private final Provider<ChargeRepository> chargeRepositoryProvider;

  private final Provider<FinanceRepository> financeRepositoryProvider;

  private final Provider<TeamRepository> teamRepositoryProvider;

  private final Provider<CurrentTeamManager> teamManagerProvider;

  public MemberFinanceViewModel_Factory(Provider<ChargeRepository> chargeRepositoryProvider,
      Provider<FinanceRepository> financeRepositoryProvider,
      Provider<TeamRepository> teamRepositoryProvider,
      Provider<CurrentTeamManager> teamManagerProvider) {
    this.chargeRepositoryProvider = chargeRepositoryProvider;
    this.financeRepositoryProvider = financeRepositoryProvider;
    this.teamRepositoryProvider = teamRepositoryProvider;
    this.teamManagerProvider = teamManagerProvider;
  }

  @Override
  public MemberFinanceViewModel get() {
    return newInstance(chargeRepositoryProvider.get(), financeRepositoryProvider.get(), teamRepositoryProvider.get(), teamManagerProvider.get());
  }

  public static MemberFinanceViewModel_Factory create(
      Provider<ChargeRepository> chargeRepositoryProvider,
      Provider<FinanceRepository> financeRepositoryProvider,
      Provider<TeamRepository> teamRepositoryProvider,
      Provider<CurrentTeamManager> teamManagerProvider) {
    return new MemberFinanceViewModel_Factory(chargeRepositoryProvider, financeRepositoryProvider, teamRepositoryProvider, teamManagerProvider);
  }

  public static MemberFinanceViewModel newInstance(ChargeRepository chargeRepository,
      FinanceRepository financeRepository, TeamRepository teamRepository,
      CurrentTeamManager teamManager) {
    return new MemberFinanceViewModel(chargeRepository, financeRepository, teamRepository, teamManager);
  }
}
