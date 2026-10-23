package info.prorabka.abik.android.di;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import info.prorabka.abik.android.data.api.GeoApi;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import retrofit2.Retrofit;

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
public final class NetworkModule_ProvideGeoApiFactory implements Factory<GeoApi> {
  private final Provider<Retrofit> retrofitProvider;

  public NetworkModule_ProvideGeoApiFactory(Provider<Retrofit> retrofitProvider) {
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public GeoApi get() {
    return provideGeoApi(retrofitProvider.get());
  }

  public static NetworkModule_ProvideGeoApiFactory create(Provider<Retrofit> retrofitProvider) {
    return new NetworkModule_ProvideGeoApiFactory(retrofitProvider);
  }

  public static GeoApi provideGeoApi(Retrofit retrofit) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideGeoApi(retrofit));
  }
}
