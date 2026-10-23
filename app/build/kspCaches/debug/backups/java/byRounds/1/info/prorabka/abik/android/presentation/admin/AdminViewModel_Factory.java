package info.prorabka.abik.android.presentation.admin;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import info.prorabka.abik.android.domain.repository.AdminRepository;
import info.prorabka.abik.android.domain.repository.GeoRepository;
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
public final class AdminViewModel_Factory implements Factory<AdminViewModel> {
  private final Provider<AdminRepository> adminRepositoryProvider;

  private final Provider<GeoRepository> geoRepositoryProvider;

  public AdminViewModel_Factory(Provider<AdminRepository> adminRepositoryProvider,
      Provider<GeoRepository> geoRepositoryProvider) {
    this.adminRepositoryProvider = adminRepositoryProvider;
    this.geoRepositoryProvider = geoRepositoryProvider;
  }

  @Override
  public AdminViewModel get() {
    return newInstance(adminRepositoryProvider.get(), geoRepositoryProvider.get());
  }

  public static AdminViewModel_Factory create(Provider<AdminRepository> adminRepositoryProvider,
      Provider<GeoRepository> geoRepositoryProvider) {
    return new AdminViewModel_Factory(adminRepositoryProvider, geoRepositoryProvider);
  }

  public static AdminViewModel newInstance(AdminRepository adminRepository,
      GeoRepository geoRepository) {
    return new AdminViewModel(adminRepository, geoRepository);
  }
}
