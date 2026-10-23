package info.prorabka.abik.android.di;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import info.prorabka.abik.android.data.api.BalanceApi;
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
public final class NetworkModule_ProvideBalanceApiFactory implements Factory<BalanceApi> {
  private final Provider<Retrofit> retrofitProvider;

  public NetworkModule_ProvideBalanceApiFactory(Provider<Retrofit> retrofitProvider) {
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public BalanceApi get() {
    return provideBalanceApi(retrofitProvider.get());
  }

  public static NetworkModule_ProvideBalanceApiFactory create(Provider<Retrofit> retrofitProvider) {
    return new NetworkModule_ProvideBalanceApiFactory(retrofitProvider);
  }

  public static BalanceApi provideBalanceApi(Retrofit retrofit) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideBalanceApi(retrofit));
  }
}
