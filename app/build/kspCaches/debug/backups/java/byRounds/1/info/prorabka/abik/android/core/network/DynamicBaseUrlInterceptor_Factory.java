package info.prorabka.abik.android.core.network;

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
public final class DynamicBaseUrlInterceptor_Factory implements Factory<DynamicBaseUrlInterceptor> {
  private final Provider<BaseUrlHolder> holderProvider;

  public DynamicBaseUrlInterceptor_Factory(Provider<BaseUrlHolder> holderProvider) {
    this.holderProvider = holderProvider;
  }

  @Override
  public DynamicBaseUrlInterceptor get() {
    return newInstance(holderProvider.get());
  }

  public static DynamicBaseUrlInterceptor_Factory create(Provider<BaseUrlHolder> holderProvider) {
    return new DynamicBaseUrlInterceptor_Factory(holderProvider);
  }

  public static DynamicBaseUrlInterceptor newInstance(BaseUrlHolder holder) {
    return new DynamicBaseUrlInterceptor(holder);
  }
}
