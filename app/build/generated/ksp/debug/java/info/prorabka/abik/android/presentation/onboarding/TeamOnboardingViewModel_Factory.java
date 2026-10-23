package info.prorabka.abik.android.presentation.onboarding;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import info.prorabka.abik.android.domain.repository.GeoRepository;
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
public final class TeamOnboardingViewModel_Factory implements Factory<TeamOnboardingViewModel> {
  private final Provider<TeamRepository> teamRepositoryProvider;

  private final Provider<GeoRepository> geoRepositoryProvider;

  public TeamOnboardingViewModel_Factory(Provider<TeamRepository> teamRepositoryProvider,
      Provider<GeoRepository> geoRepositoryProvider) {
    this.teamRepositoryProvider = teamRepositoryProvider;
    this.geoRepositoryProvider = geoRepositoryProvider;
  }

  @Override
  public TeamOnboardingViewModel get() {
    return newInstance(teamRepositoryProvider.get(), geoRepositoryProvider.get());
  }

  public static TeamOnboardingViewModel_Factory create(
      Provider<TeamRepository> teamRepositoryProvider,
      Provider<GeoRepository> geoRepositoryProvider) {
    return new TeamOnboardingViewModel_Factory(teamRepositoryProvider, geoRepositoryProvider);
  }

  public static TeamOnboardingViewModel newInstance(TeamRepository teamRepository,
      GeoRepository geoRepository) {
    return new TeamOnboardingViewModel(teamRepository, geoRepository);
  }
}
