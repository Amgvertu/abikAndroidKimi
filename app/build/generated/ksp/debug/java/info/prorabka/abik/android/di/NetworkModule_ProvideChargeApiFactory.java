package info.prorabka.abik.android.di;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import info.prorabka.abik.android.data.api.ChargeApi;
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
public final class NetworkModule_ProvideChargeApiFactory implements Factory<ChargeApi> {
  private final Provider<Retrofit> retrofitProvider;

  public NetworkModule_ProvideChargeApiFactory(Provider<Retrofit> retrofitProvider) {
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public ChargeApi get() {
    return provideChargeApi(retrofitProvider.get());
  }

  public static NetworkModule_ProvideChargeApiFactory create(Provider<Retrofit> retrofitProvider) {
    return new NetworkModule_ProvideChargeApiFactory(retrofitProvider);
  }

  public static ChargeApi provideChargeApi(Retrofit retrofit) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideChargeApi(retrofit));
  }
}
