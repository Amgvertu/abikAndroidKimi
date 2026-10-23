package info.prorabka.abik.android.di;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import info.prorabka.abik.android.data.api.InvitationApi;
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
public final class NetworkModule_ProvideInvitationApiFactory implements Factory<InvitationApi> {
  private final Provider<Retrofit> retrofitProvider;

  public NetworkModule_ProvideInvitationApiFactory(Provider<Retrofit> retrofitProvider) {
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public InvitationApi get() {
    return provideInvitationApi(retrofitProvider.get());
  }

  public static NetworkModule_ProvideInvitationApiFactory create(
      Provider<Retrofit> retrofitProvider) {
    return new NetworkModule_ProvideInvitationApiFactory(retrofitProvider);
  }

  public static InvitationApi provideInvitationApi(Retrofit retrofit) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideInvitationApi(retrofit));
  }
}
