package info.prorabka.abik.android.data.repository;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import info.prorabka.abik.android.core.prefs.SessionManager;
import info.prorabka.abik.android.core.prefs.TokenStore;
import info.prorabka.abik.android.data.api.AuthApi;
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
public final class AuthRepositoryImpl_Factory implements Factory<AuthRepositoryImpl> {
  private final Provider<AuthApi> apiProvider;

  private final Provider<TokenStore> tokenStoreProvider;

  private final Provider<SessionManager> sessionManagerProvider;

  public AuthRepositoryImpl_Factory(Provider<AuthApi> apiProvider,
      Provider<TokenStore> tokenStoreProvider, Provider<SessionManager> sessionManagerProvider) {
    this.apiProvider = apiProvider;
    this.tokenStoreProvider = tokenStoreProvider;
    this.sessionManagerProvider = sessionManagerProvider;
  }

  @Override
  public AuthRepositoryImpl get() {
    return newInstance(apiProvider.get(), tokenStoreProvider.get(), sessionManagerProvider.get());
  }

  public static AuthRepositoryImpl_Factory create(Provider<AuthApi> apiProvider,
      Provider<TokenStore> tokenStoreProvider, Provider<SessionManager> sessionManagerProvider) {
    return new AuthRepositoryImpl_Factory(apiProvider, tokenStoreProvider, sessionManagerProvider);
  }

  public static AuthRepositoryImpl newInstance(AuthApi api, TokenStore tokenStore,
      SessionManager sessionManager) {
    return new AuthRepositoryImpl(api, tokenStore, sessionManager);
  }
}
