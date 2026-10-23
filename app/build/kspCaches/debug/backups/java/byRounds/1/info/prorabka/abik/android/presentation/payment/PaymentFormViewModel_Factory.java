package info.prorabka.abik.android.presentation.payment;

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
public final class PaymentFormViewModel_Factory implements Factory<PaymentFormViewModel> {
  private final Provider<FinanceRepository> financeRepositoryProvider;

  private final Provider<TeamRepository> teamRepositoryProvider;

  private final Provider<CurrentTeamManager> teamManagerProvider;

  public PaymentFormViewModel_Factory(Provider<FinanceRepository> financeRepositoryProvider,
      Provider<TeamRepository> teamRepositoryProvider,
      Provider<CurrentTeamManager> teamManagerProvider) {
    this.financeRepositoryProvider = financeRepositoryProvider;
    this.teamRepositoryProvider = teamRepositoryProvider;
    this.teamManagerProvider = teamManagerProvider;
  }

  @Override
  public PaymentFormViewModel get() {
    return newInstance(financeRepositoryProvider.get(), teamRepositoryProvider.get(), teamManagerProvider.get());
  }

  public static PaymentFormViewModel_Factory create(
      Provider<FinanceRepository> financeRepositoryProvider,
      Provider<TeamRepository> teamRepositoryProvider,
      Provider<CurrentTeamManager> teamManagerProvider) {
    return new PaymentFormViewModel_Factory(financeRepositoryProvider, teamRepositoryProvider, teamManagerProvider);
  }

  public static PaymentFormViewModel newInstance(FinanceRepository financeRepository,
      TeamRepository teamRepository, CurrentTeamManager teamManager) {
    return new PaymentFormViewModel(financeRepository, teamRepository, teamManager);
  }
}
