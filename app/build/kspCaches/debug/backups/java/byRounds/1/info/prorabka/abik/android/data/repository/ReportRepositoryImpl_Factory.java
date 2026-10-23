package info.prorabka.abik.android.data.repository;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import info.prorabka.abik.android.data.api.ReportApi;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class ReportRepositoryImpl_Factory implements Factory<ReportRepositoryImpl> {
  private final Provider<ReportApi> apiProvider;

  private final Provider<Context> contextProvider;

  public ReportRepositoryImpl_Factory(Provider<ReportApi> apiProvider,
      Provider<Context> contextProvider) {
    this.apiProvider = apiProvider;
    this.contextProvider = contextProvider;
  }

  @Override
  public ReportRepositoryImpl get() {
    return newInstance(apiProvider.get(), contextProvider.get());
  }

  public static ReportRepositoryImpl_Factory create(Provider<ReportApi> apiProvider,
      Provider<Context> contextProvider) {
    return new ReportRepositoryImpl_Factory(apiProvider, contextProvider);
  }

  public static ReportRepositoryImpl newInstance(ReportApi api, Context context) {
    return new ReportRepositoryImpl(api, context);
  }
}
