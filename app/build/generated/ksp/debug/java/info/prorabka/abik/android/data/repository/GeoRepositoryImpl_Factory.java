package info.prorabka.abik.android.data.repository;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import info.prorabka.abik.android.data.api.GeoApi;
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
public final class GeoRepositoryImpl_Factory implements Factory<GeoRepositoryImpl> {
  private final Provider<GeoApi> apiProvider;

  public GeoRepositoryImpl_Factory(Provider<GeoApi> apiProvider) {
    this.apiProvider = apiProvider;
  }

  @Override
  public GeoRepositoryImpl get() {
    return newInstance(apiProvider.get());
  }

  public static GeoRepositoryImpl_Factory create(Provider<GeoApi> apiProvider) {
    return new GeoRepositoryImpl_Factory(apiProvider);
  }

  public static GeoRepositoryImpl newInstance(GeoApi api) {
    return new GeoRepositoryImpl(api);
  }
}
