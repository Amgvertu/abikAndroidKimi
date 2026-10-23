package info.prorabka.abik.android.core.prefs;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
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
public final class SessionManager_Factory implements Factory<SessionManager> {
  private final Provider<TokenStore> tokenStoreProvider;

  public SessionManager_Factory(Provider<TokenStore> tokenStoreProvider) {
    this.tokenStoreProvider = tokenStoreProvider;
  }

  @Override
  public SessionManager get() {
    return newInstance(tokenStoreProvider.get());
  }

  public static SessionManager_Factory create(Provider<TokenStore> tokenStoreProvider) {
    return new SessionManager_Factory(tokenStoreProvider);
  }

  public static SessionManager newInstance(TokenStore tokenStore) {
    return new SessionManager(tokenStore);
  }
}
