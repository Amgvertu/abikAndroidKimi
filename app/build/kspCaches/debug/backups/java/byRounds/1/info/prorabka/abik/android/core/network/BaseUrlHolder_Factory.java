package info.prorabka.abik.android.core.network;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import info.prorabka.abik.android.core.prefs.SettingsStore;
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
public final class BaseUrlHolder_Factory implements Factory<BaseUrlHolder> {
  private final Provider<SettingsStore> settingsStoreProvider;

  public BaseUrlHolder_Factory(Provider<SettingsStore> settingsStoreProvider) {
    this.settingsStoreProvider = settingsStoreProvider;
  }

  @Override
  public BaseUrlHolder get() {
    return newInstance(settingsStoreProvider.get());
  }

  public static BaseUrlHolder_Factory create(Provider<SettingsStore> settingsStoreProvider) {
    return new BaseUrlHolder_Factory(settingsStoreProvider);
  }

  public static BaseUrlHolder newInstance(SettingsStore settingsStore) {
    return new BaseUrlHolder(settingsStore);
  }
}
