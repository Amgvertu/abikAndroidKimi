package info.prorabka.abik.android.presentation.onboarding;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import info.prorabka.abik.android.domain.repository.GeoRepository;
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
public final class ProfileFormViewModel_Factory implements Factory<ProfileFormViewModel> {
  private final Provider<UserRepository> userRepositoryProvider;

  private final Provider<GeoRepository> geoRepositoryProvider;

  public ProfileFormViewModel_Factory(Provider<UserRepository> userRepositoryProvider,
      Provider<GeoRepository> geoRepositoryProvider) {
    this.userRepositoryProvider = userRepositoryProvider;
    this.geoRepositoryProvider = geoRepositoryProvider;
  }

  @Override
  public ProfileFormViewModel get() {
    return newInstance(userRepositoryProvider.get(), geoRepositoryProvider.get());
  }

  public static ProfileFormViewModel_Factory create(Provider<UserRepository> userRepositoryProvider,
      Provider<GeoRepository> geoRepositoryProvider) {
    return new ProfileFormViewModel_Factory(userRepositoryProvider, geoRepositoryProvider);
  }

  public static ProfileFormViewModel newInstance(UserRepository userRepository,
      GeoRepository geoRepository) {
    return new ProfileFormViewModel(userRepository, geoRepository);
  }
}
