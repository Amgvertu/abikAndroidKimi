package info.prorabka.abik.android.data.repository;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import info.prorabka.abik.android.data.api.UserApi;
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
public final class UserRepositoryImpl_Factory implements Factory<UserRepositoryImpl> {
  private final Provider<UserApi> apiProvider;

  public UserRepositoryImpl_Factory(Provider<UserApi> apiProvider) {
    this.apiProvider = apiProvider;
  }

  @Override
  public UserRepositoryImpl get() {
    return newInstance(apiProvider.get());
  }

  public static UserRepositoryImpl_Factory create(Provider<UserApi> apiProvider) {
    return new UserRepositoryImpl_Factory(apiProvider);
  }

  public static UserRepositoryImpl newInstance(UserApi api) {
    return new UserRepositoryImpl(api);
  }
}
