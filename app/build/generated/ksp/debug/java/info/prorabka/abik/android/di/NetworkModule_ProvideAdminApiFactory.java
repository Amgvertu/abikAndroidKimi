package info.prorabka.abik.android.di;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import info.prorabka.abik.android.data.api.AdminApi;
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
public final class NetworkModule_ProvideAdminApiFactory implements Factory<AdminApi> {
  private final Provider<Retrofit> retrofitProvider;

  public NetworkModule_ProvideAdminApiFactory(Provider<Retrofit> retrofitProvider) {
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public AdminApi get() {
    return provideAdminApi(retrofitProvider.get());
  }

  public static NetworkModule_ProvideAdminApiFactory create(Provider<Retrofit> retrofitProvider) {
    return new NetworkModule_ProvideAdminApiFactory(retrofitProvider);
  }

  public static AdminApi provideAdminApi(Retrofit retrofit) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideAdminApi(retrofit));
  }
}
