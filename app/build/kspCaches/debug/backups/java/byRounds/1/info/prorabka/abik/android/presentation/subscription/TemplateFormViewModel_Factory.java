package info.prorabka.abik.android.presentation.subscription;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import info.prorabka.abik.android.core.CurrentTeamManager;
import info.prorabka.abik.android.domain.repository.ChargeRepository;
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
public final class TemplateFormViewModel_Factory implements Factory<TemplateFormViewModel> {
  private final Provider<ChargeRepository> chargeRepositoryProvider;

  private final Provider<TeamRepository> teamRepositoryProvider;

  private final Provider<CurrentTeamManager> teamManagerProvider;

  public TemplateFormViewModel_Factory(Provider<ChargeRepository> chargeRepositoryProvider,
      Provider<TeamRepository> teamRepositoryProvider,
      Provider<CurrentTeamManager> teamManagerProvider) {
    this.chargeRepositoryProvider = chargeRepositoryProvider;
    this.teamRepositoryProvider = teamRepositoryProvider;
    this.teamManagerProvider = teamManagerProvider;
  }

  @Override
  public TemplateFormViewModel get() {
    return newInstance(chargeRepositoryProvider.get(), teamRepositoryProvider.get(), teamManagerProvider.get());
  }

  public static TemplateFormViewModel_Factory create(
      Provider<ChargeRepository> chargeRepositoryProvider,
      Provider<TeamRepository> teamRepositoryProvider,
      Provider<CurrentTeamManager> teamManagerProvider) {
    return new TemplateFormViewModel_Factory(chargeRepositoryProvider, teamRepositoryProvider, teamManagerProvider);
  }

  public static TemplateFormViewModel newInstance(ChargeRepository chargeRepository,
      TeamRepository teamRepository, CurrentTeamManager teamManager) {
    return new TemplateFormViewModel(chargeRepository, teamRepository, teamManager);
  }
}
