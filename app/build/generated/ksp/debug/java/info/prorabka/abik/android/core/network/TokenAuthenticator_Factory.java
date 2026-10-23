package info.prorabka.abik.android.core.network;

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
public final class TokenAuthenticator_Factory implements Factory<TokenAuthenticator> {
  private final Provider<TokenStore> tokenStoreProvider;

  private final Provider<SessionManager> sessionManagerProvider;

  private final Provider<AuthApi> authApiProvider;

  public TokenAuthenticator_Factory(Provider<TokenStore> tokenStoreProvider,
      Provider<SessionManager> sessionManagerProvider, Provider<AuthApi> authApiProvider) {
    this.tokenStoreProvider = tokenStoreProvider;
    this.sessionManagerProvider = sessionManagerProvider;
    this.authApiProvider = authApiProvider;
  }

  @Override
  public TokenAuthenticator get() {
    return newInstance(tokenStoreProvider.get(), sessionManagerProvider.get(), authApiProvider);
  }

  public static TokenAuthenticator_Factory create(Provider<TokenStore> tokenStoreProvider,
      Provider<SessionManager> sessionManagerProvider, Provider<AuthApi> authApiProvider) {
    return new TokenAuthenticator_Factory(tokenStoreProvider, sessionManagerProvider, authApiProvider);
  }

  public static TokenAuthenticator newInstance(TokenStore tokenStore, SessionManager sessionManager,
      Provider<AuthApi> authApi) {
    return new TokenAuthenticator(tokenStore, sessionManager, authApi);
  }
}
