package info.prorabka.abik.android.data.repository;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import info.prorabka.abik.android.data.api.BalanceApi;
import info.prorabka.abik.android.data.api.PaymentApi;
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
public final class FinanceRepositoryImpl_Factory implements Factory<FinanceRepositoryImpl> {
  private final Provider<BalanceApi> balanceApiProvider;

  private final Provider<PaymentApi> paymentApiProvider;

  public FinanceRepositoryImpl_Factory(Provider<BalanceApi> balanceApiProvider,
      Provider<PaymentApi> paymentApiProvider) {
    this.balanceApiProvider = balanceApiProvider;
    this.paymentApiProvider = paymentApiProvider;
  }

  @Override
  public FinanceRepositoryImpl get() {
    return newInstance(balanceApiProvider.get(), paymentApiProvider.get());
  }

  public static FinanceRepositoryImpl_Factory create(Provider<BalanceApi> balanceApiProvider,
      Provider<PaymentApi> paymentApiProvider) {
    return new FinanceRepositoryImpl_Factory(balanceApiProvider, paymentApiProvider);
  }

  public static FinanceRepositoryImpl newInstance(BalanceApi balanceApi, PaymentApi paymentApi) {
    return new FinanceRepositoryImpl(balanceApi, paymentApi);
  }
}
