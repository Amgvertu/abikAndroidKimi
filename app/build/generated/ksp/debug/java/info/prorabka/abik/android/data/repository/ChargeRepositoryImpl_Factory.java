package info.prorabka.abik.android.data.repository;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import info.prorabka.abik.android.data.api.ChargeApi;
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
public final class ChargeRepositoryImpl_Factory implements Factory<ChargeRepositoryImpl> {
  private final Provider<ChargeApi> apiProvider;

  public ChargeRepositoryImpl_Factory(Provider<ChargeApi> apiProvider) {
    this.apiProvider = apiProvider;
  }

  @Override
  public ChargeRepositoryImpl get() {
    return newInstance(apiProvider.get());
  }

  public static ChargeRepositoryImpl_Factory create(Provider<ChargeApi> apiProvider) {
    return new ChargeRepositoryImpl_Factory(apiProvider);
  }

  public static ChargeRepositoryImpl newInstance(ChargeApi api) {
    return new ChargeRepositoryImpl(api);
  }
}
