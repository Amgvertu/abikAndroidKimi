package info.prorabka.abik.android.di;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import info.prorabka.abik.android.core.network.AuthInterceptor;
import info.prorabka.abik.android.core.network.DynamicBaseUrlInterceptor;
import info.prorabka.abik.android.core.network.TokenAuthenticator;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import okhttp3.OkHttpClient;

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
public final class NetworkModule_ProvideOkHttpFactory implements Factory<OkHttpClient> {
  private final Provider<AuthInterceptor> authInterceptorProvider;

  private final Provider<DynamicBaseUrlInterceptor> baseUrlInterceptorProvider;

  private final Provider<TokenAuthenticator> tokenAuthenticatorProvider;

  public NetworkModule_ProvideOkHttpFactory(Provider<AuthInterceptor> authInterceptorProvider,
      Provider<DynamicBaseUrlInterceptor> baseUrlInterceptorProvider,
      Provider<TokenAuthenticator> tokenAuthenticatorProvider) {
    this.authInterceptorProvider = authInterceptorProvider;
    this.baseUrlInterceptorProvider = baseUrlInterceptorProvider;
    this.tokenAuthenticatorProvider = tokenAuthenticatorProvider;
  }

  @Override
  public OkHttpClient get() {
    return provideOkHttp(authInterceptorProvider.get(), baseUrlInterceptorProvider.get(), tokenAuthenticatorProvider.get());
  }

  public static NetworkModule_ProvideOkHttpFactory create(
      Provider<AuthInterceptor> authInterceptorProvider,
      Provider<DynamicBaseUrlInterceptor> baseUrlInterceptorProvider,
      Provider<TokenAuthenticator> tokenAuthenticatorProvider) {
    return new NetworkModule_ProvideOkHttpFactory(authInterceptorProvider, baseUrlInterceptorProvider, tokenAuthenticatorProvider);
  }

  public static OkHttpClient provideOkHttp(AuthInterceptor authInterceptor,
      DynamicBaseUrlInterceptor baseUrlInterceptor, TokenAuthenticator tokenAuthenticator) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideOkHttp(authInterceptor, baseUrlInterceptor, tokenAuthenticator));
  }
}
