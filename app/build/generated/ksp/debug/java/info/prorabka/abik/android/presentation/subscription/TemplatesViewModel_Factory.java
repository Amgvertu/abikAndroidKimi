package info.prorabka.abik.android.presentation.subscription;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import info.prorabka.abik.android.core.CurrentTeamManager;
import info.prorabka.abik.android.domain.repository.ChargeRepository;
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
public final class TemplatesViewModel_Factory implements Factory<TemplatesViewModel> {
  private final Provider<ChargeRepository> chargeRepositoryProvider;

  private final Provider<CurrentTeamManager> teamManagerProvider;

  public TemplatesViewModel_Factory(Provider<ChargeRepository> chargeRepositoryProvider,
      Provider<CurrentTeamManager> teamManagerProvider) {
    this.chargeRepositoryProvider = chargeRepositoryProvider;
    this.teamManagerProvider = teamManagerProvider;
  }

  @Override
  public TemplatesViewModel get() {
    return newInstance(chargeRepositoryProvider.get(), teamManagerProvider.get());
  }

  public static TemplatesViewModel_Factory create(
      Provider<ChargeRepository> chargeRepositoryProvider,
      Provider<CurrentTeamManager> teamManagerProvider) {
    return new TemplatesViewModel_Factory(chargeRepositoryProvider, teamManagerProvider);
  }

  public static TemplatesViewModel newInstance(ChargeRepository chargeRepository,
      CurrentTeamManager teamManager) {
    return new TemplatesViewModel(chargeRepository, teamManager);
  }
}
