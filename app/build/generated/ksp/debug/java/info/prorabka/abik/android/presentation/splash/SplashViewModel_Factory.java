package info.prorabka.abik.android.presentation.splash;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import info.prorabka.abik.android.core.prefs.TokenStore;
import info.prorabka.abik.android.domain.repository.TeamRepository;
import info.prorabka.abik.android.domain.repository.UserRepository;
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
public final class SplashViewModel_Factory implements Factory<SplashViewModel> {
  private final Provider<TokenStore> tokenStoreProvider;

  private final Provider<UserRepository> userRepositoryProvider;

  private final Provider<TeamRepository> teamRepositoryProvider;

  public SplashViewModel_Factory(Provider<TokenStore> tokenStoreProvider,
      Provider<UserRepository> userRepositoryProvider,
      Provider<TeamRepository> teamRepositoryProvider) {
    this.tokenStoreProvider = tokenStoreProvider;
    this.userRepositoryProvider = userRepositoryProvider;
    this.teamRepositoryProvider = teamRepositoryProvider;
  }

  @Override
  public SplashViewModel get() {
    return newInstance(tokenStoreProvider.get(), userRepositoryProvider.get(), teamRepositoryProvider.get());
  }

  public static SplashViewModel_Factory create(Provider<TokenStore> tokenStoreProvider,
      Provider<UserRepository> userRepositoryProvider,
      Provider<TeamRepository> teamRepositoryProvider) {
    return new SplashViewModel_Factory(tokenStoreProvider, userRepositoryProvider, teamRepositoryProvider);
  }

  public static SplashViewModel newInstance(TokenStore tokenStore, UserRepository userRepository,
      TeamRepository teamRepository) {
    return new SplashViewModel(tokenStore, userRepository, teamRepository);
  }
}
