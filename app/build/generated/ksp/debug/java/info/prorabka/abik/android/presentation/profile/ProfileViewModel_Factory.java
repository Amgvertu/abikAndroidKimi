package info.prorabka.abik.android.presentation.profile;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import info.prorabka.abik.android.core.CurrentTeamManager;
import info.prorabka.abik.android.core.prefs.SettingsStore;
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
public final class ProfileViewModel_Factory implements Factory<ProfileViewModel> {
  private final Provider<UserRepository> userRepositoryProvider;

  private final Provider<TeamRepository> teamRepositoryProvider;

  private final Provider<CurrentTeamManager> teamManagerProvider;

  private final Provider<SettingsStore> settingsStoreProvider;

  private final Provider<TokenStore> tokenStoreProvider;

  public ProfileViewModel_Factory(Provider<UserRepository> userRepositoryProvider,
      Provider<TeamRepository> teamRepositoryProvider,
      Provider<CurrentTeamManager> teamManagerProvider,
      Provider<SettingsStore> settingsStoreProvider, Provider<TokenStore> tokenStoreProvider) {
    this.userRepositoryProvider = userRepositoryProvider;
    this.teamRepositoryProvider = teamRepositoryProvider;
    this.teamManagerProvider = teamManagerProvider;
    this.settingsStoreProvider = settingsStoreProvider;
    this.tokenStoreProvider = tokenStoreProvider;
  }

  @Override
  public ProfileViewModel get() {
    return newInstance(userRepositoryProvider.get(), teamRepositoryProvider.get(), teamManagerProvider.get(), settingsStoreProvider.get(), tokenStoreProvider.get());
  }

  public static ProfileViewModel_Factory create(Provider<UserRepository> userRepositoryProvider,
      Provider<TeamRepository> teamRepositoryProvider,
      Provider<CurrentTeamManager> teamManagerProvider,
      Provider<SettingsStore> settingsStoreProvider, Provider<TokenStore> tokenStoreProvider) {
    return new ProfileViewModel_Factory(userRepositoryProvider, teamRepositoryProvider, teamManagerProvider, settingsStoreProvider, tokenStoreProvider);
  }

  public static ProfileViewModel newInstance(UserRepository userRepository,
      TeamRepository teamRepository, CurrentTeamManager teamManager, SettingsStore settingsStore,
      TokenStore tokenStore) {
    return new ProfileViewModel(userRepository, teamRepository, teamManager, settingsStore, tokenStore);
  }
}
