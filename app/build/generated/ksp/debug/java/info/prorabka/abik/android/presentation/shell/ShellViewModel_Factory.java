package info.prorabka.abik.android.presentation.shell;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import info.prorabka.abik.android.core.CurrentTeamManager;
import info.prorabka.abik.android.core.prefs.TokenStore;
import info.prorabka.abik.android.domain.repository.AuthRepository;
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
public final class ShellViewModel_Factory implements Factory<ShellViewModel> {
  private final Provider<CurrentTeamManager> teamManagerProvider;

  private final Provider<TeamRepository> teamRepositoryProvider;

  private final Provider<FinanceRepository> financeRepositoryProvider;

  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<TokenStore> tokenStoreProvider;

  public ShellViewModel_Factory(Provider<CurrentTeamManager> teamManagerProvider,
      Provider<TeamRepository> teamRepositoryProvider,
      Provider<FinanceRepository> financeRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider, Provider<TokenStore> tokenStoreProvider) {
    this.teamManagerProvider = teamManagerProvider;
    this.teamRepositoryProvider = teamRepositoryProvider;
    this.financeRepositoryProvider = financeRepositoryProvider;
    this.authRepositoryProvider = authRepositoryProvider;
    this.tokenStoreProvider = tokenStoreProvider;
  }

  @Override
  public ShellViewModel get() {
    return newInstance(teamManagerProvider.get(), teamRepositoryProvider.get(), financeRepositoryProvider.get(), authRepositoryProvider.get(), tokenStoreProvider.get());
  }

  public static ShellViewModel_Factory create(Provider<CurrentTeamManager> teamManagerProvider,
      Provider<TeamRepository> teamRepositoryProvider,
      Provider<FinanceRepository> financeRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider, Provider<TokenStore> tokenStoreProvider) {
    return new ShellViewModel_Factory(teamManagerProvider, teamRepositoryProvider, financeRepositoryProvider, authRepositoryProvider, tokenStoreProvider);
  }

  public static ShellViewModel newInstance(CurrentTeamManager teamManager,
      TeamRepository teamRepository, FinanceRepository financeRepository,
      AuthRepository authRepository, TokenStore tokenStore) {
    return new ShellViewModel(teamManager, teamRepository, financeRepository, authRepository, tokenStore);
  }
}
