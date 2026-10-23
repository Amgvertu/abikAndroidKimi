package info.prorabka.abik.android.data.repository;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import info.prorabka.abik.android.data.api.AdminApi;
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
public final class AdminRepositoryImpl_Factory implements Factory<AdminRepositoryImpl> {
  private final Provider<AdminApi> apiProvider;

  public AdminRepositoryImpl_Factory(Provider<AdminApi> apiProvider) {
    this.apiProvider = apiProvider;
  }

  @Override
  public AdminRepositoryImpl get() {
    return newInstance(apiProvider.get());
  }

  public static AdminRepositoryImpl_Factory create(Provider<AdminApi> apiProvider) {
    return new AdminRepositoryImpl_Factory(apiProvider);
  }

  public static AdminRepositoryImpl newInstance(AdminApi api) {
    return new AdminRepositoryImpl(api);
  }
}
