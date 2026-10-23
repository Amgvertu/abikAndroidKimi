package info.prorabka.abik.android;

import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import info.prorabka.abik.android.core.prefs.SessionManager;
import info.prorabka.abik.android.core.prefs.SettingsStore;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class MainActivity_MembersInjector implements MembersInjector<MainActivity> {
  private final Provider<SettingsStore> settingsStoreProvider;

  private final Provider<SessionManager> sessionManagerProvider;

  public MainActivity_MembersInjector(Provider<SettingsStore> settingsStoreProvider,
      Provider<SessionManager> sessionManagerProvider) {
    this.settingsStoreProvider = settingsStoreProvider;
    this.sessionManagerProvider = sessionManagerProvider;
  }

  public static MembersInjector<MainActivity> create(Provider<SettingsStore> settingsStoreProvider,
      Provider<SessionManager> sessionManagerProvider) {
    return new MainActivity_MembersInjector(settingsStoreProvider, sessionManagerProvider);
  }

  @Override
  public void injectMembers(MainActivity instance) {
    injectSettingsStore(instance, settingsStoreProvider.get());
    injectSessionManager(instance, sessionManagerProvider.get());
  }

  @InjectedFieldSignature("info.prorabka.abik.android.MainActivity.settingsStore")
  public static void injectSettingsStore(MainActivity instance, SettingsStore settingsStore) {
    instance.settingsStore = settingsStore;
  }

  @InjectedFieldSignature("info.prorabka.abik.android.MainActivity.sessionManager")
  public static void injectSessionManager(MainActivity instance, SessionManager sessionManager) {
    instance.sessionManager = sessionManager;
  }
}
