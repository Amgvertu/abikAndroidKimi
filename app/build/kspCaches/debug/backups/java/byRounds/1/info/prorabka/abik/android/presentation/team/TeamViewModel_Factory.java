package info.prorabka.abik.android.presentation.team;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import info.prorabka.abik.android.core.CurrentTeamManager;
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
public final class TeamViewModel_Factory implements Factory<TeamViewModel> {
  private final Provider<TeamRepository> teamRepositoryProvider;

  private final Provider<CurrentTeamManager> teamManagerProvider;

  public TeamViewModel_Factory(Provider<TeamRepository> teamRepositoryProvider,
      Provider<CurrentTeamManager> teamManagerProvider) {
    this.teamRepositoryProvider = teamRepositoryProvider;
    this.teamManagerProvider = teamManagerProvider;
  }

  @Override
  public TeamViewModel get() {
    return newInstance(teamRepositoryProvider.get(), teamManagerProvider.get());
  }

  public static TeamViewModel_Factory create(Provider<TeamRepository> teamRepositoryProvider,
      Provider<CurrentTeamManager> teamManagerProvider) {
    return new TeamViewModel_Factory(teamRepositoryProvider, teamManagerProvider);
  }

  public static TeamViewModel newInstance(TeamRepository teamRepository,
      CurrentTeamManager teamManager) {
    return new TeamViewModel(teamRepository, teamManager);
  }
}
