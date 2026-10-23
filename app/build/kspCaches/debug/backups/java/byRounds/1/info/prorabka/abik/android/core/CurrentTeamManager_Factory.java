package info.prorabka.abik.android.core;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import info.prorabka.abik.android.core.prefs.SettingsStore;
import info.prorabka.abik.android.domain.repository.TeamRepository;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class CurrentTeamManager_Factory implements Factory<CurrentTeamManager> {
  private final Provider<TeamRepository> teamRepositoryProvider;

  private final Provider<SettingsStore> settingsStoreProvider;

  public CurrentTeamManager_Factory(Provider<TeamRepository> teamRepositoryProvider,
      Provider<SettingsStore> settingsStoreProvider) {
    this.teamRepositoryProvider = teamRepositoryProvider;
    this.settingsStoreProvider = settingsStoreProvider;
  }

  @Override
  public CurrentTeamManager get() {
    return newInstance(teamRepositoryProvider.get(), settingsStoreProvider.get());
  }

  public static CurrentTeamManager_Factory create(Provider<TeamRepository> teamRepositoryProvider,
      Provider<SettingsStore> settingsStoreProvider) {
    return new CurrentTeamManager_Factory(teamRepositoryProvider, settingsStoreProvider);
  }

  public static CurrentTeamManager newInstance(TeamRepository teamRepository,
      SettingsStore settingsStore) {
    return new CurrentTeamManager(teamRepository, settingsStore);
  }
}
