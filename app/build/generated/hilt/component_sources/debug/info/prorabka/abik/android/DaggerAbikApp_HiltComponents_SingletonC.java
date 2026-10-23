package info.prorabka.abik.android;

import android.app.Activity;
import android.app.Service;
import android.view.View;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import dagger.hilt.android.ActivityRetainedLifecycle;
import dagger.hilt.android.ViewModelLifecycle;
import dagger.hilt.android.internal.builders.ActivityComponentBuilder;
import dagger.hilt.android.internal.builders.ActivityRetainedComponentBuilder;
import dagger.hilt.android.internal.builders.FragmentComponentBuilder;
import dagger.hilt.android.internal.builders.ServiceComponentBuilder;
import dagger.hilt.android.internal.builders.ViewComponentBuilder;
import dagger.hilt.android.internal.builders.ViewModelComponentBuilder;
import dagger.hilt.android.internal.builders.ViewWithFragmentComponentBuilder;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories_InternalFactoryFactory_Factory;
import dagger.hilt.android.internal.managers.ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory;
import dagger.hilt.android.internal.managers.SavedStateHandleHolder;
import dagger.hilt.android.internal.modules.ApplicationContextModule;
import dagger.hilt.android.internal.modules.ApplicationContextModule_ProvideContextFactory;
import dagger.internal.DaggerGenerated;
import dagger.internal.DelegateFactory;
import dagger.internal.DoubleCheck;
import dagger.internal.IdentifierNameString;
import dagger.internal.KeepFieldType;
import dagger.internal.LazyClassKeyMap;
import dagger.internal.MapBuilder;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import info.prorabka.abik.android.core.CurrentTeamManager;
import info.prorabka.abik.android.core.network.AuthInterceptor;
import info.prorabka.abik.android.core.network.BaseUrlHolder;
import info.prorabka.abik.android.core.network.DynamicBaseUrlInterceptor;
import info.prorabka.abik.android.core.network.TokenAuthenticator;
import info.prorabka.abik.android.core.prefs.SessionManager;
import info.prorabka.abik.android.core.prefs.SettingsStore;
import info.prorabka.abik.android.core.prefs.TokenStore;
import info.prorabka.abik.android.data.api.AdminApi;
import info.prorabka.abik.android.data.api.AuthApi;
import info.prorabka.abik.android.data.api.BalanceApi;
import info.prorabka.abik.android.data.api.ChargeApi;
import info.prorabka.abik.android.data.api.GeoApi;
import info.prorabka.abik.android.data.api.InvitationApi;
import info.prorabka.abik.android.data.api.PaymentApi;
import info.prorabka.abik.android.data.api.ReportApi;
import info.prorabka.abik.android.data.api.TeamApi;
import info.prorabka.abik.android.data.api.UserApi;
import info.prorabka.abik.android.data.repository.AdminRepositoryImpl;
import info.prorabka.abik.android.data.repository.AuthRepositoryImpl;
import info.prorabka.abik.android.data.repository.ChargeRepositoryImpl;
import info.prorabka.abik.android.data.repository.FinanceRepositoryImpl;
import info.prorabka.abik.android.data.repository.GeoRepositoryImpl;
import info.prorabka.abik.android.data.repository.ReportRepositoryImpl;
import info.prorabka.abik.android.data.repository.TeamRepositoryImpl;
import info.prorabka.abik.android.data.repository.UserRepositoryImpl;
import info.prorabka.abik.android.di.NetworkModule_ProvideAdminApiFactory;
import info.prorabka.abik.android.di.NetworkModule_ProvideAuthApiFactory;
import info.prorabka.abik.android.di.NetworkModule_ProvideBalanceApiFactory;
import info.prorabka.abik.android.di.NetworkModule_ProvideChargeApiFactory;
import info.prorabka.abik.android.di.NetworkModule_ProvideGeoApiFactory;
import info.prorabka.abik.android.di.NetworkModule_ProvideInvitationApiFactory;
import info.prorabka.abik.android.di.NetworkModule_ProvideJsonFactory;
import info.prorabka.abik.android.di.NetworkModule_ProvideOkHttpFactory;
import info.prorabka.abik.android.di.NetworkModule_ProvidePaymentApiFactory;
import info.prorabka.abik.android.di.NetworkModule_ProvideReportApiFactory;
import info.prorabka.abik.android.di.NetworkModule_ProvideRetrofitFactory;
import info.prorabka.abik.android.di.NetworkModule_ProvideTeamApiFactory;
import info.prorabka.abik.android.di.NetworkModule_ProvideUserApiFactory;
import info.prorabka.abik.android.presentation.admin.AdminViewModel;
import info.prorabka.abik.android.presentation.admin.AdminViewModel_HiltModules;
import info.prorabka.abik.android.presentation.auth.ForgotPasswordViewModel;
import info.prorabka.abik.android.presentation.auth.ForgotPasswordViewModel_HiltModules;
import info.prorabka.abik.android.presentation.auth.LoginViewModel;
import info.prorabka.abik.android.presentation.auth.LoginViewModel_HiltModules;
import info.prorabka.abik.android.presentation.auth.RegisterViewModel;
import info.prorabka.abik.android.presentation.auth.RegisterViewModel_HiltModules;
import info.prorabka.abik.android.presentation.onboarding.ProfileFormViewModel;
import info.prorabka.abik.android.presentation.onboarding.ProfileFormViewModel_HiltModules;
import info.prorabka.abik.android.presentation.onboarding.TeamOnboardingViewModel;
import info.prorabka.abik.android.presentation.onboarding.TeamOnboardingViewModel_HiltModules;
import info.prorabka.abik.android.presentation.payment.PaymentFormViewModel;
import info.prorabka.abik.android.presentation.payment.PaymentFormViewModel_HiltModules;
import info.prorabka.abik.android.presentation.profile.ChangePhoneViewModel;
import info.prorabka.abik.android.presentation.profile.ChangePhoneViewModel_HiltModules;
import info.prorabka.abik.android.presentation.profile.ProfileViewModel;
import info.prorabka.abik.android.presentation.profile.ProfileViewModel_HiltModules;
import info.prorabka.abik.android.presentation.reports.ReportsViewModel;
import info.prorabka.abik.android.presentation.reports.ReportsViewModel_HiltModules;
import info.prorabka.abik.android.presentation.shell.ShellViewModel;
import info.prorabka.abik.android.presentation.shell.ShellViewModel_HiltModules;
import info.prorabka.abik.android.presentation.splash.SplashViewModel;
import info.prorabka.abik.android.presentation.splash.SplashViewModel_HiltModules;
import info.prorabka.abik.android.presentation.subscription.ChargeCreateViewModel;
import info.prorabka.abik.android.presentation.subscription.ChargeCreateViewModel_HiltModules;
import info.prorabka.abik.android.presentation.subscription.MemberFinanceViewModel;
import info.prorabka.abik.android.presentation.subscription.MemberFinanceViewModel_HiltModules;
import info.prorabka.abik.android.presentation.subscription.SubscriptionViewModel;
import info.prorabka.abik.android.presentation.subscription.SubscriptionViewModel_HiltModules;
import info.prorabka.abik.android.presentation.subscription.TemplateFormViewModel;
import info.prorabka.abik.android.presentation.subscription.TemplateFormViewModel_HiltModules;
import info.prorabka.abik.android.presentation.subscription.TemplatesViewModel;
import info.prorabka.abik.android.presentation.subscription.TemplatesViewModel_HiltModules;
import info.prorabka.abik.android.presentation.team.MemberDetailsViewModel;
import info.prorabka.abik.android.presentation.team.MemberDetailsViewModel_HiltModules;
import info.prorabka.abik.android.presentation.team.TeamViewModel;
import info.prorabka.abik.android.presentation.team.TeamViewModel_HiltModules;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;
import kotlinx.serialization.json.Json;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;

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
public final class DaggerAbikApp_HiltComponents_SingletonC {
  private DaggerAbikApp_HiltComponents_SingletonC() {
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private ApplicationContextModule applicationContextModule;

    private Builder() {
    }

    public Builder applicationContextModule(ApplicationContextModule applicationContextModule) {
      this.applicationContextModule = Preconditions.checkNotNull(applicationContextModule);
      return this;
    }

    public AbikApp_HiltComponents.SingletonC build() {
      Preconditions.checkBuilderRequirement(applicationContextModule, ApplicationContextModule.class);
      return new SingletonCImpl(applicationContextModule);
    }
  }

  private static final class ActivityRetainedCBuilder implements AbikApp_HiltComponents.ActivityRetainedC.Builder {
    private final SingletonCImpl singletonCImpl;

    private SavedStateHandleHolder savedStateHandleHolder;

    private ActivityRetainedCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ActivityRetainedCBuilder savedStateHandleHolder(
        SavedStateHandleHolder savedStateHandleHolder) {
      this.savedStateHandleHolder = Preconditions.checkNotNull(savedStateHandleHolder);
      return this;
    }

    @Override
    public AbikApp_HiltComponents.ActivityRetainedC build() {
      Preconditions.checkBuilderRequirement(savedStateHandleHolder, SavedStateHandleHolder.class);
      return new ActivityRetainedCImpl(singletonCImpl, savedStateHandleHolder);
    }
  }

  private static final class ActivityCBuilder implements AbikApp_HiltComponents.ActivityC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private Activity activity;

    private ActivityCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ActivityCBuilder activity(Activity activity) {
      this.activity = Preconditions.checkNotNull(activity);
      return this;
    }

    @Override
    public AbikApp_HiltComponents.ActivityC build() {
      Preconditions.checkBuilderRequirement(activity, Activity.class);
      return new ActivityCImpl(singletonCImpl, activityRetainedCImpl, activity);
    }
  }

  private static final class FragmentCBuilder implements AbikApp_HiltComponents.FragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private Fragment fragment;

    private FragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public FragmentCBuilder fragment(Fragment fragment) {
      this.fragment = Preconditions.checkNotNull(fragment);
      return this;
    }

    @Override
    public AbikApp_HiltComponents.FragmentC build() {
      Preconditions.checkBuilderRequirement(fragment, Fragment.class);
      return new FragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragment);
    }
  }

  private static final class ViewWithFragmentCBuilder implements AbikApp_HiltComponents.ViewWithFragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private View view;

    private ViewWithFragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;
    }

    @Override
    public ViewWithFragmentCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public AbikApp_HiltComponents.ViewWithFragmentC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewWithFragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl, view);
    }
  }

  private static final class ViewCBuilder implements AbikApp_HiltComponents.ViewC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private View view;

    private ViewCBuilder(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public ViewCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public AbikApp_HiltComponents.ViewC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, view);
    }
  }

  private static final class ViewModelCBuilder implements AbikApp_HiltComponents.ViewModelC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private SavedStateHandle savedStateHandle;

    private ViewModelLifecycle viewModelLifecycle;

    private ViewModelCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ViewModelCBuilder savedStateHandle(SavedStateHandle handle) {
      this.savedStateHandle = Preconditions.checkNotNull(handle);
      return this;
    }

    @Override
    public ViewModelCBuilder viewModelLifecycle(ViewModelLifecycle viewModelLifecycle) {
      this.viewModelLifecycle = Preconditions.checkNotNull(viewModelLifecycle);
      return this;
    }

    @Override
    public AbikApp_HiltComponents.ViewModelC build() {
      Preconditions.checkBuilderRequirement(savedStateHandle, SavedStateHandle.class);
      Preconditions.checkBuilderRequirement(viewModelLifecycle, ViewModelLifecycle.class);
      return new ViewModelCImpl(singletonCImpl, activityRetainedCImpl, savedStateHandle, viewModelLifecycle);
    }
  }

  private static final class ServiceCBuilder implements AbikApp_HiltComponents.ServiceC.Builder {
    private final SingletonCImpl singletonCImpl;

    private Service service;

    private ServiceCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ServiceCBuilder service(Service service) {
      this.service = Preconditions.checkNotNull(service);
      return this;
    }

    @Override
    public AbikApp_HiltComponents.ServiceC build() {
      Preconditions.checkBuilderRequirement(service, Service.class);
      return new ServiceCImpl(singletonCImpl, service);
    }
  }

  private static final class ViewWithFragmentCImpl extends AbikApp_HiltComponents.ViewWithFragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private final ViewWithFragmentCImpl viewWithFragmentCImpl = this;

    private ViewWithFragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;


    }
  }

  private static final class FragmentCImpl extends AbikApp_HiltComponents.FragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl = this;

    private FragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        Fragment fragmentParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return activityCImpl.getHiltInternalFactoryFactory();
    }

    @Override
    public ViewWithFragmentComponentBuilder viewWithFragmentComponentBuilder() {
      return new ViewWithFragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl);
    }
  }

  private static final class ViewCImpl extends AbikApp_HiltComponents.ViewC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final ViewCImpl viewCImpl = this;

    private ViewCImpl(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }
  }

  private static final class ActivityCImpl extends AbikApp_HiltComponents.ActivityC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl = this;

    private ActivityCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, Activity activityParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;


    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return DefaultViewModelFactories_InternalFactoryFactory_Factory.newInstance(getViewModelKeys(), new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl));
    }

    @Override
    public Map<Class<?>, Boolean> getViewModelKeys() {
      return LazyClassKeyMap.<Boolean>of(MapBuilder.<String, Boolean>newMapBuilder(19).put(LazyClassKeyProvider.info_prorabka_abik_android_presentation_admin_AdminViewModel, AdminViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.info_prorabka_abik_android_presentation_profile_ChangePhoneViewModel, ChangePhoneViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.info_prorabka_abik_android_presentation_subscription_ChargeCreateViewModel, ChargeCreateViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.info_prorabka_abik_android_presentation_auth_ForgotPasswordViewModel, ForgotPasswordViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.info_prorabka_abik_android_presentation_auth_LoginViewModel, LoginViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.info_prorabka_abik_android_presentation_team_MemberDetailsViewModel, MemberDetailsViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.info_prorabka_abik_android_presentation_subscription_MemberFinanceViewModel, MemberFinanceViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.info_prorabka_abik_android_presentation_payment_PaymentFormViewModel, PaymentFormViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.info_prorabka_abik_android_presentation_onboarding_ProfileFormViewModel, ProfileFormViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.info_prorabka_abik_android_presentation_profile_ProfileViewModel, ProfileViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.info_prorabka_abik_android_presentation_auth_RegisterViewModel, RegisterViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.info_prorabka_abik_android_presentation_reports_ReportsViewModel, ReportsViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.info_prorabka_abik_android_presentation_shell_ShellViewModel, ShellViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.info_prorabka_abik_android_presentation_splash_SplashViewModel, SplashViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.info_prorabka_abik_android_presentation_subscription_SubscriptionViewModel, SubscriptionViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.info_prorabka_abik_android_presentation_onboarding_TeamOnboardingViewModel, TeamOnboardingViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.info_prorabka_abik_android_presentation_team_TeamViewModel, TeamViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.info_prorabka_abik_android_presentation_subscription_TemplateFormViewModel, TemplateFormViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.info_prorabka_abik_android_presentation_subscription_TemplatesViewModel, TemplatesViewModel_HiltModules.KeyModule.provide()).build());
    }

    @Override
    public ViewModelComponentBuilder getViewModelComponentBuilder() {
      return new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public FragmentComponentBuilder fragmentComponentBuilder() {
      return new FragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @Override
    public ViewComponentBuilder viewComponentBuilder() {
      return new ViewCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @Override
    public void injectMainActivity(MainActivity arg0) {
      injectMainActivity2(arg0);
    }

    @CanIgnoreReturnValue
    private MainActivity injectMainActivity2(MainActivity instance) {
      MainActivity_MembersInjector.injectSettingsStore(instance, singletonCImpl.settingsStoreProvider.get());
      MainActivity_MembersInjector.injectSessionManager(instance, singletonCImpl.sessionManagerProvider.get());
      return instance;
    }

    @IdentifierNameString
    private static final class LazyClassKeyProvider {
      static String info_prorabka_abik_android_presentation_auth_RegisterViewModel = "info.prorabka.abik.android.presentation.auth.RegisterViewModel";

      static String info_prorabka_abik_android_presentation_subscription_TemplatesViewModel = "info.prorabka.abik.android.presentation.subscription.TemplatesViewModel";

      static String info_prorabka_abik_android_presentation_subscription_MemberFinanceViewModel = "info.prorabka.abik.android.presentation.subscription.MemberFinanceViewModel";

      static String info_prorabka_abik_android_presentation_profile_ProfileViewModel = "info.prorabka.abik.android.presentation.profile.ProfileViewModel";

      static String info_prorabka_abik_android_presentation_team_MemberDetailsViewModel = "info.prorabka.abik.android.presentation.team.MemberDetailsViewModel";

      static String info_prorabka_abik_android_presentation_profile_ChangePhoneViewModel = "info.prorabka.abik.android.presentation.profile.ChangePhoneViewModel";

      static String info_prorabka_abik_android_presentation_shell_ShellViewModel = "info.prorabka.abik.android.presentation.shell.ShellViewModel";

      static String info_prorabka_abik_android_presentation_subscription_SubscriptionViewModel = "info.prorabka.abik.android.presentation.subscription.SubscriptionViewModel";

      static String info_prorabka_abik_android_presentation_team_TeamViewModel = "info.prorabka.abik.android.presentation.team.TeamViewModel";

      static String info_prorabka_abik_android_presentation_auth_LoginViewModel = "info.prorabka.abik.android.presentation.auth.LoginViewModel";

      static String info_prorabka_abik_android_presentation_onboarding_ProfileFormViewModel = "info.prorabka.abik.android.presentation.onboarding.ProfileFormViewModel";

      static String info_prorabka_abik_android_presentation_reports_ReportsViewModel = "info.prorabka.abik.android.presentation.reports.ReportsViewModel";

      static String info_prorabka_abik_android_presentation_subscription_ChargeCreateViewModel = "info.prorabka.abik.android.presentation.subscription.ChargeCreateViewModel";

      static String info_prorabka_abik_android_presentation_subscription_TemplateFormViewModel = "info.prorabka.abik.android.presentation.subscription.TemplateFormViewModel";

      static String info_prorabka_abik_android_presentation_admin_AdminViewModel = "info.prorabka.abik.android.presentation.admin.AdminViewModel";

      static String info_prorabka_abik_android_presentation_auth_ForgotPasswordViewModel = "info.prorabka.abik.android.presentation.auth.ForgotPasswordViewModel";

      static String info_prorabka_abik_android_presentation_onboarding_TeamOnboardingViewModel = "info.prorabka.abik.android.presentation.onboarding.TeamOnboardingViewModel";

      static String info_prorabka_abik_android_presentation_payment_PaymentFormViewModel = "info.prorabka.abik.android.presentation.payment.PaymentFormViewModel";

      static String info_prorabka_abik_android_presentation_splash_SplashViewModel = "info.prorabka.abik.android.presentation.splash.SplashViewModel";

      @KeepFieldType
      RegisterViewModel info_prorabka_abik_android_presentation_auth_RegisterViewModel2;

      @KeepFieldType
      TemplatesViewModel info_prorabka_abik_android_presentation_subscription_TemplatesViewModel2;

      @KeepFieldType
      MemberFinanceViewModel info_prorabka_abik_android_presentation_subscription_MemberFinanceViewModel2;

      @KeepFieldType
      ProfileViewModel info_prorabka_abik_android_presentation_profile_ProfileViewModel2;

      @KeepFieldType
      MemberDetailsViewModel info_prorabka_abik_android_presentation_team_MemberDetailsViewModel2;

      @KeepFieldType
      ChangePhoneViewModel info_prorabka_abik_android_presentation_profile_ChangePhoneViewModel2;

      @KeepFieldType
      ShellViewModel info_prorabka_abik_android_presentation_shell_ShellViewModel2;

      @KeepFieldType
      SubscriptionViewModel info_prorabka_abik_android_presentation_subscription_SubscriptionViewModel2;

      @KeepFieldType
      TeamViewModel info_prorabka_abik_android_presentation_team_TeamViewModel2;

      @KeepFieldType
      LoginViewModel info_prorabka_abik_android_presentation_auth_LoginViewModel2;

      @KeepFieldType
      ProfileFormViewModel info_prorabka_abik_android_presentation_onboarding_ProfileFormViewModel2;

      @KeepFieldType
      ReportsViewModel info_prorabka_abik_android_presentation_reports_ReportsViewModel2;

      @KeepFieldType
      ChargeCreateViewModel info_prorabka_abik_android_presentation_subscription_ChargeCreateViewModel2;

      @KeepFieldType
      TemplateFormViewModel info_prorabka_abik_android_presentation_subscription_TemplateFormViewModel2;

      @KeepFieldType
      AdminViewModel info_prorabka_abik_android_presentation_admin_AdminViewModel2;

      @KeepFieldType
      ForgotPasswordViewModel info_prorabka_abik_android_presentation_auth_ForgotPasswordViewModel2;

      @KeepFieldType
      TeamOnboardingViewModel info_prorabka_abik_android_presentation_onboarding_TeamOnboardingViewModel2;

      @KeepFieldType
      PaymentFormViewModel info_prorabka_abik_android_presentation_payment_PaymentFormViewModel2;

      @KeepFieldType
      SplashViewModel info_prorabka_abik_android_presentation_splash_SplashViewModel2;
    }
  }

  private static final class ViewModelCImpl extends AbikApp_HiltComponents.ViewModelC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ViewModelCImpl viewModelCImpl = this;

    private Provider<AdminViewModel> adminViewModelProvider;

    private Provider<ChangePhoneViewModel> changePhoneViewModelProvider;

    private Provider<ChargeCreateViewModel> chargeCreateViewModelProvider;

    private Provider<ForgotPasswordViewModel> forgotPasswordViewModelProvider;

    private Provider<LoginViewModel> loginViewModelProvider;

    private Provider<MemberDetailsViewModel> memberDetailsViewModelProvider;

    private Provider<MemberFinanceViewModel> memberFinanceViewModelProvider;

    private Provider<PaymentFormViewModel> paymentFormViewModelProvider;

    private Provider<ProfileFormViewModel> profileFormViewModelProvider;

    private Provider<ProfileViewModel> profileViewModelProvider;

    private Provider<RegisterViewModel> registerViewModelProvider;

    private Provider<ReportsViewModel> reportsViewModelProvider;

    private Provider<ShellViewModel> shellViewModelProvider;

    private Provider<SplashViewModel> splashViewModelProvider;

    private Provider<SubscriptionViewModel> subscriptionViewModelProvider;

    private Provider<TeamOnboardingViewModel> teamOnboardingViewModelProvider;

    private Provider<TeamViewModel> teamViewModelProvider;

    private Provider<TemplateFormViewModel> templateFormViewModelProvider;

    private Provider<TemplatesViewModel> templatesViewModelProvider;

    private ViewModelCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, SavedStateHandle savedStateHandleParam,
        ViewModelLifecycle viewModelLifecycleParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;

      initialize(savedStateHandleParam, viewModelLifecycleParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandle savedStateHandleParam,
        final ViewModelLifecycle viewModelLifecycleParam) {
      this.adminViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 0);
      this.changePhoneViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 1);
      this.chargeCreateViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 2);
      this.forgotPasswordViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 3);
      this.loginViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 4);
      this.memberDetailsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 5);
      this.memberFinanceViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 6);
      this.paymentFormViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 7);
      this.profileFormViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 8);
      this.profileViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 9);
      this.registerViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 10);
      this.reportsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 11);
      this.shellViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 12);
      this.splashViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 13);
      this.subscriptionViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 14);
      this.teamOnboardingViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 15);
      this.teamViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 16);
      this.templateFormViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 17);
      this.templatesViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 18);
    }

    @Override
    public Map<Class<?>, javax.inject.Provider<ViewModel>> getHiltViewModelMap() {
      return LazyClassKeyMap.<javax.inject.Provider<ViewModel>>of(MapBuilder.<String, javax.inject.Provider<ViewModel>>newMapBuilder(19).put(LazyClassKeyProvider.info_prorabka_abik_android_presentation_admin_AdminViewModel, ((Provider) adminViewModelProvider)).put(LazyClassKeyProvider.info_prorabka_abik_android_presentation_profile_ChangePhoneViewModel, ((Provider) changePhoneViewModelProvider)).put(LazyClassKeyProvider.info_prorabka_abik_android_presentation_subscription_ChargeCreateViewModel, ((Provider) chargeCreateViewModelProvider)).put(LazyClassKeyProvider.info_prorabka_abik_android_presentation_auth_ForgotPasswordViewModel, ((Provider) forgotPasswordViewModelProvider)).put(LazyClassKeyProvider.info_prorabka_abik_android_presentation_auth_LoginViewModel, ((Provider) loginViewModelProvider)).put(LazyClassKeyProvider.info_prorabka_abik_android_presentation_team_MemberDetailsViewModel, ((Provider) memberDetailsViewModelProvider)).put(LazyClassKeyProvider.info_prorabka_abik_android_presentation_subscription_MemberFinanceViewModel, ((Provider) memberFinanceViewModelProvider)).put(LazyClassKeyProvider.info_prorabka_abik_android_presentation_payment_PaymentFormViewModel, ((Provider) paymentFormViewModelProvider)).put(LazyClassKeyProvider.info_prorabka_abik_android_presentation_onboarding_ProfileFormViewModel, ((Provider) profileFormViewModelProvider)).put(LazyClassKeyProvider.info_prorabka_abik_android_presentation_profile_ProfileViewModel, ((Provider) profileViewModelProvider)).put(LazyClassKeyProvider.info_prorabka_abik_android_presentation_auth_RegisterViewModel, ((Provider) registerViewModelProvider)).put(LazyClassKeyProvider.info_prorabka_abik_android_presentation_reports_ReportsViewModel, ((Provider) reportsViewModelProvider)).put(LazyClassKeyProvider.info_prorabka_abik_android_presentation_shell_ShellViewModel, ((Provider) shellViewModelProvider)).put(LazyClassKeyProvider.info_prorabka_abik_android_presentation_splash_SplashViewModel, ((Provider) splashViewModelProvider)).put(LazyClassKeyProvider.info_prorabka_abik_android_presentation_subscription_SubscriptionViewModel, ((Provider) subscriptionViewModelProvider)).put(LazyClassKeyProvider.info_prorabka_abik_android_presentation_onboarding_TeamOnboardingViewModel, ((Provider) teamOnboardingViewModelProvider)).put(LazyClassKeyProvider.info_prorabka_abik_android_presentation_team_TeamViewModel, ((Provider) teamViewModelProvider)).put(LazyClassKeyProvider.info_prorabka_abik_android_presentation_subscription_TemplateFormViewModel, ((Provider) templateFormViewModelProvider)).put(LazyClassKeyProvider.info_prorabka_abik_android_presentation_subscription_TemplatesViewModel, ((Provider) templatesViewModelProvider)).build());
    }

    @Override
    public Map<Class<?>, Object> getHiltViewModelAssistedMap() {
      return Collections.<Class<?>, Object>emptyMap();
    }

    @IdentifierNameString
    private static final class LazyClassKeyProvider {
      static String info_prorabka_abik_android_presentation_onboarding_TeamOnboardingViewModel = "info.prorabka.abik.android.presentation.onboarding.TeamOnboardingViewModel";

      static String info_prorabka_abik_android_presentation_team_TeamViewModel = "info.prorabka.abik.android.presentation.team.TeamViewModel";

      static String info_prorabka_abik_android_presentation_team_MemberDetailsViewModel = "info.prorabka.abik.android.presentation.team.MemberDetailsViewModel";

      static String info_prorabka_abik_android_presentation_auth_LoginViewModel = "info.prorabka.abik.android.presentation.auth.LoginViewModel";

      static String info_prorabka_abik_android_presentation_subscription_TemplateFormViewModel = "info.prorabka.abik.android.presentation.subscription.TemplateFormViewModel";

      static String info_prorabka_abik_android_presentation_subscription_TemplatesViewModel = "info.prorabka.abik.android.presentation.subscription.TemplatesViewModel";

      static String info_prorabka_abik_android_presentation_admin_AdminViewModel = "info.prorabka.abik.android.presentation.admin.AdminViewModel";

      static String info_prorabka_abik_android_presentation_subscription_ChargeCreateViewModel = "info.prorabka.abik.android.presentation.subscription.ChargeCreateViewModel";

      static String info_prorabka_abik_android_presentation_subscription_MemberFinanceViewModel = "info.prorabka.abik.android.presentation.subscription.MemberFinanceViewModel";

      static String info_prorabka_abik_android_presentation_auth_RegisterViewModel = "info.prorabka.abik.android.presentation.auth.RegisterViewModel";

      static String info_prorabka_abik_android_presentation_auth_ForgotPasswordViewModel = "info.prorabka.abik.android.presentation.auth.ForgotPasswordViewModel";

      static String info_prorabka_abik_android_presentation_onboarding_ProfileFormViewModel = "info.prorabka.abik.android.presentation.onboarding.ProfileFormViewModel";

      static String info_prorabka_abik_android_presentation_profile_ProfileViewModel = "info.prorabka.abik.android.presentation.profile.ProfileViewModel";

      static String info_prorabka_abik_android_presentation_payment_PaymentFormViewModel = "info.prorabka.abik.android.presentation.payment.PaymentFormViewModel";

      static String info_prorabka_abik_android_presentation_reports_ReportsViewModel = "info.prorabka.abik.android.presentation.reports.ReportsViewModel";

      static String info_prorabka_abik_android_presentation_shell_ShellViewModel = "info.prorabka.abik.android.presentation.shell.ShellViewModel";

      static String info_prorabka_abik_android_presentation_splash_SplashViewModel = "info.prorabka.abik.android.presentation.splash.SplashViewModel";

      static String info_prorabka_abik_android_presentation_profile_ChangePhoneViewModel = "info.prorabka.abik.android.presentation.profile.ChangePhoneViewModel";

      static String info_prorabka_abik_android_presentation_subscription_SubscriptionViewModel = "info.prorabka.abik.android.presentation.subscription.SubscriptionViewModel";

      @KeepFieldType
      TeamOnboardingViewModel info_prorabka_abik_android_presentation_onboarding_TeamOnboardingViewModel2;

      @KeepFieldType
      TeamViewModel info_prorabka_abik_android_presentation_team_TeamViewModel2;

      @KeepFieldType
      MemberDetailsViewModel info_prorabka_abik_android_presentation_team_MemberDetailsViewModel2;

      @KeepFieldType
      LoginViewModel info_prorabka_abik_android_presentation_auth_LoginViewModel2;

      @KeepFieldType
      TemplateFormViewModel info_prorabka_abik_android_presentation_subscription_TemplateFormViewModel2;

      @KeepFieldType
      TemplatesViewModel info_prorabka_abik_android_presentation_subscription_TemplatesViewModel2;

      @KeepFieldType
      AdminViewModel info_prorabka_abik_android_presentation_admin_AdminViewModel2;

      @KeepFieldType
      ChargeCreateViewModel info_prorabka_abik_android_presentation_subscription_ChargeCreateViewModel2;

      @KeepFieldType
      MemberFinanceViewModel info_prorabka_abik_android_presentation_subscription_MemberFinanceViewModel2;

      @KeepFieldType
      RegisterViewModel info_prorabka_abik_android_presentation_auth_RegisterViewModel2;

      @KeepFieldType
      ForgotPasswordViewModel info_prorabka_abik_android_presentation_auth_ForgotPasswordViewModel2;

      @KeepFieldType
      ProfileFormViewModel info_prorabka_abik_android_presentation_onboarding_ProfileFormViewModel2;

      @KeepFieldType
      ProfileViewModel info_prorabka_abik_android_presentation_profile_ProfileViewModel2;

      @KeepFieldType
      PaymentFormViewModel info_prorabka_abik_android_presentation_payment_PaymentFormViewModel2;

      @KeepFieldType
      ReportsViewModel info_prorabka_abik_android_presentation_reports_ReportsViewModel2;

      @KeepFieldType
      ShellViewModel info_prorabka_abik_android_presentation_shell_ShellViewModel2;

      @KeepFieldType
      SplashViewModel info_prorabka_abik_android_presentation_splash_SplashViewModel2;

      @KeepFieldType
      ChangePhoneViewModel info_prorabka_abik_android_presentation_profile_ChangePhoneViewModel2;

      @KeepFieldType
      SubscriptionViewModel info_prorabka_abik_android_presentation_subscription_SubscriptionViewModel2;
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final ViewModelCImpl viewModelCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          ViewModelCImpl viewModelCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.viewModelCImpl = viewModelCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // info.prorabka.abik.android.presentation.admin.AdminViewModel 
          return (T) new AdminViewModel(singletonCImpl.adminRepositoryImplProvider.get(), singletonCImpl.geoRepositoryImplProvider.get());

          case 1: // info.prorabka.abik.android.presentation.profile.ChangePhoneViewModel 
          return (T) new ChangePhoneViewModel(singletonCImpl.userRepositoryImplProvider.get());

          case 2: // info.prorabka.abik.android.presentation.subscription.ChargeCreateViewModel 
          return (T) new ChargeCreateViewModel(singletonCImpl.chargeRepositoryImplProvider.get(), singletonCImpl.teamRepositoryImplProvider.get(), singletonCImpl.currentTeamManagerProvider.get());

          case 3: // info.prorabka.abik.android.presentation.auth.ForgotPasswordViewModel 
          return (T) new ForgotPasswordViewModel(singletonCImpl.authRepositoryImplProvider.get());

          case 4: // info.prorabka.abik.android.presentation.auth.LoginViewModel 
          return (T) new LoginViewModel(singletonCImpl.authRepositoryImplProvider.get());

          case 5: // info.prorabka.abik.android.presentation.team.MemberDetailsViewModel 
          return (T) new MemberDetailsViewModel(singletonCImpl.userRepositoryImplProvider.get(), singletonCImpl.currentTeamManagerProvider.get());

          case 6: // info.prorabka.abik.android.presentation.subscription.MemberFinanceViewModel 
          return (T) new MemberFinanceViewModel(singletonCImpl.chargeRepositoryImplProvider.get(), singletonCImpl.financeRepositoryImplProvider.get(), singletonCImpl.teamRepositoryImplProvider.get(), singletonCImpl.currentTeamManagerProvider.get());

          case 7: // info.prorabka.abik.android.presentation.payment.PaymentFormViewModel 
          return (T) new PaymentFormViewModel(singletonCImpl.financeRepositoryImplProvider.get(), singletonCImpl.teamRepositoryImplProvider.get(), singletonCImpl.currentTeamManagerProvider.get());

          case 8: // info.prorabka.abik.android.presentation.onboarding.ProfileFormViewModel 
          return (T) new ProfileFormViewModel(singletonCImpl.userRepositoryImplProvider.get(), singletonCImpl.geoRepositoryImplProvider.get());

          case 9: // info.prorabka.abik.android.presentation.profile.ProfileViewModel 
          return (T) new ProfileViewModel(singletonCImpl.userRepositoryImplProvider.get(), singletonCImpl.teamRepositoryImplProvider.get(), singletonCImpl.currentTeamManagerProvider.get(), singletonCImpl.settingsStoreProvider.get(), singletonCImpl.tokenStoreProvider.get());

          case 10: // info.prorabka.abik.android.presentation.auth.RegisterViewModel 
          return (T) new RegisterViewModel(singletonCImpl.authRepositoryImplProvider.get());

          case 11: // info.prorabka.abik.android.presentation.reports.ReportsViewModel 
          return (T) new ReportsViewModel(singletonCImpl.reportRepositoryImplProvider.get(), singletonCImpl.chargeRepositoryImplProvider.get(), singletonCImpl.teamRepositoryImplProvider.get(), singletonCImpl.currentTeamManagerProvider.get());

          case 12: // info.prorabka.abik.android.presentation.shell.ShellViewModel 
          return (T) new ShellViewModel(singletonCImpl.currentTeamManagerProvider.get(), singletonCImpl.teamRepositoryImplProvider.get(), singletonCImpl.financeRepositoryImplProvider.get(), singletonCImpl.authRepositoryImplProvider.get(), singletonCImpl.tokenStoreProvider.get());

          case 13: // info.prorabka.abik.android.presentation.splash.SplashViewModel 
          return (T) new SplashViewModel(singletonCImpl.tokenStoreProvider.get(), singletonCImpl.userRepositoryImplProvider.get(), singletonCImpl.teamRepositoryImplProvider.get());

          case 14: // info.prorabka.abik.android.presentation.subscription.SubscriptionViewModel 
          return (T) new SubscriptionViewModel(singletonCImpl.currentTeamManagerProvider.get(), singletonCImpl.teamRepositoryImplProvider.get(), singletonCImpl.financeRepositoryImplProvider.get());

          case 15: // info.prorabka.abik.android.presentation.onboarding.TeamOnboardingViewModel 
          return (T) new TeamOnboardingViewModel(singletonCImpl.teamRepositoryImplProvider.get(), singletonCImpl.geoRepositoryImplProvider.get());

          case 16: // info.prorabka.abik.android.presentation.team.TeamViewModel 
          return (T) new TeamViewModel(singletonCImpl.teamRepositoryImplProvider.get(), singletonCImpl.currentTeamManagerProvider.get());

          case 17: // info.prorabka.abik.android.presentation.subscription.TemplateFormViewModel 
          return (T) new TemplateFormViewModel(singletonCImpl.chargeRepositoryImplProvider.get(), singletonCImpl.teamRepositoryImplProvider.get(), singletonCImpl.currentTeamManagerProvider.get());

          case 18: // info.prorabka.abik.android.presentation.subscription.TemplatesViewModel 
          return (T) new TemplatesViewModel(singletonCImpl.chargeRepositoryImplProvider.get(), singletonCImpl.currentTeamManagerProvider.get());

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ActivityRetainedCImpl extends AbikApp_HiltComponents.ActivityRetainedC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl = this;

    private Provider<ActivityRetainedLifecycle> provideActivityRetainedLifecycleProvider;

    private ActivityRetainedCImpl(SingletonCImpl singletonCImpl,
        SavedStateHandleHolder savedStateHandleHolderParam) {
      this.singletonCImpl = singletonCImpl;

      initialize(savedStateHandleHolderParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandleHolder savedStateHandleHolderParam) {
      this.provideActivityRetainedLifecycleProvider = DoubleCheck.provider(new SwitchingProvider<ActivityRetainedLifecycle>(singletonCImpl, activityRetainedCImpl, 0));
    }

    @Override
    public ActivityComponentBuilder activityComponentBuilder() {
      return new ActivityCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public ActivityRetainedLifecycle getActivityRetainedLifecycle() {
      return provideActivityRetainedLifecycleProvider.get();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // dagger.hilt.android.ActivityRetainedLifecycle 
          return (T) ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory.provideActivityRetainedLifecycle();

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ServiceCImpl extends AbikApp_HiltComponents.ServiceC {
    private final SingletonCImpl singletonCImpl;

    private final ServiceCImpl serviceCImpl = this;

    private ServiceCImpl(SingletonCImpl singletonCImpl, Service serviceParam) {
      this.singletonCImpl = singletonCImpl;


    }
  }

  private static final class SingletonCImpl extends AbikApp_HiltComponents.SingletonC {
    private final ApplicationContextModule applicationContextModule;

    private final SingletonCImpl singletonCImpl = this;

    private Provider<SettingsStore> settingsStoreProvider;

    private Provider<TokenStore> tokenStoreProvider;

    private Provider<SessionManager> sessionManagerProvider;

    private Provider<AuthInterceptor> authInterceptorProvider;

    private Provider<BaseUrlHolder> baseUrlHolderProvider;

    private Provider<DynamicBaseUrlInterceptor> dynamicBaseUrlInterceptorProvider;

    private Provider<Retrofit> provideRetrofitProvider;

    private Provider<AuthApi> provideAuthApiProvider;

    private Provider<TokenAuthenticator> tokenAuthenticatorProvider;

    private Provider<OkHttpClient> provideOkHttpProvider;

    private Provider<Json> provideJsonProvider;

    private Provider<AdminApi> provideAdminApiProvider;

    private Provider<AdminRepositoryImpl> adminRepositoryImplProvider;

    private Provider<GeoApi> provideGeoApiProvider;

    private Provider<GeoRepositoryImpl> geoRepositoryImplProvider;

    private Provider<UserApi> provideUserApiProvider;

    private Provider<UserRepositoryImpl> userRepositoryImplProvider;

    private Provider<ChargeApi> provideChargeApiProvider;

    private Provider<ChargeRepositoryImpl> chargeRepositoryImplProvider;

    private Provider<TeamApi> provideTeamApiProvider;

    private Provider<InvitationApi> provideInvitationApiProvider;

    private Provider<TeamRepositoryImpl> teamRepositoryImplProvider;

    private Provider<CurrentTeamManager> currentTeamManagerProvider;

    private Provider<AuthRepositoryImpl> authRepositoryImplProvider;

    private Provider<BalanceApi> provideBalanceApiProvider;

    private Provider<PaymentApi> providePaymentApiProvider;

    private Provider<FinanceRepositoryImpl> financeRepositoryImplProvider;

    private Provider<ReportApi> provideReportApiProvider;

    private Provider<ReportRepositoryImpl> reportRepositoryImplProvider;

    private SingletonCImpl(ApplicationContextModule applicationContextModuleParam) {
      this.applicationContextModule = applicationContextModuleParam;
      initialize(applicationContextModuleParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final ApplicationContextModule applicationContextModuleParam) {
      this.settingsStoreProvider = DoubleCheck.provider(new SwitchingProvider<SettingsStore>(singletonCImpl, 0));
      this.tokenStoreProvider = DoubleCheck.provider(new SwitchingProvider<TokenStore>(singletonCImpl, 2));
      this.sessionManagerProvider = DoubleCheck.provider(new SwitchingProvider<SessionManager>(singletonCImpl, 1));
      this.authInterceptorProvider = DoubleCheck.provider(new SwitchingProvider<AuthInterceptor>(singletonCImpl, 7));
      this.baseUrlHolderProvider = DoubleCheck.provider(new SwitchingProvider<BaseUrlHolder>(singletonCImpl, 9));
      this.dynamicBaseUrlInterceptorProvider = DoubleCheck.provider(new SwitchingProvider<DynamicBaseUrlInterceptor>(singletonCImpl, 8));
      this.provideRetrofitProvider = new DelegateFactory<>();
      this.provideAuthApiProvider = DoubleCheck.provider(new SwitchingProvider<AuthApi>(singletonCImpl, 11));
      this.tokenAuthenticatorProvider = DoubleCheck.provider(new SwitchingProvider<TokenAuthenticator>(singletonCImpl, 10));
      this.provideOkHttpProvider = DoubleCheck.provider(new SwitchingProvider<OkHttpClient>(singletonCImpl, 6));
      this.provideJsonProvider = DoubleCheck.provider(new SwitchingProvider<Json>(singletonCImpl, 12));
      DelegateFactory.setDelegate(provideRetrofitProvider, DoubleCheck.provider(new SwitchingProvider<Retrofit>(singletonCImpl, 5)));
      this.provideAdminApiProvider = DoubleCheck.provider(new SwitchingProvider<AdminApi>(singletonCImpl, 4));
      this.adminRepositoryImplProvider = DoubleCheck.provider(new SwitchingProvider<AdminRepositoryImpl>(singletonCImpl, 3));
      this.provideGeoApiProvider = DoubleCheck.provider(new SwitchingProvider<GeoApi>(singletonCImpl, 14));
      this.geoRepositoryImplProvider = DoubleCheck.provider(new SwitchingProvider<GeoRepositoryImpl>(singletonCImpl, 13));
      this.provideUserApiProvider = DoubleCheck.provider(new SwitchingProvider<UserApi>(singletonCImpl, 16));
      this.userRepositoryImplProvider = DoubleCheck.provider(new SwitchingProvider<UserRepositoryImpl>(singletonCImpl, 15));
      this.provideChargeApiProvider = DoubleCheck.provider(new SwitchingProvider<ChargeApi>(singletonCImpl, 18));
      this.chargeRepositoryImplProvider = DoubleCheck.provider(new SwitchingProvider<ChargeRepositoryImpl>(singletonCImpl, 17));
      this.provideTeamApiProvider = DoubleCheck.provider(new SwitchingProvider<TeamApi>(singletonCImpl, 20));
      this.provideInvitationApiProvider = DoubleCheck.provider(new SwitchingProvider<InvitationApi>(singletonCImpl, 21));
      this.teamRepositoryImplProvider = DoubleCheck.provider(new SwitchingProvider<TeamRepositoryImpl>(singletonCImpl, 19));
      this.currentTeamManagerProvider = DoubleCheck.provider(new SwitchingProvider<CurrentTeamManager>(singletonCImpl, 22));
      this.authRepositoryImplProvider = DoubleCheck.provider(new SwitchingProvider<AuthRepositoryImpl>(singletonCImpl, 23));
      this.provideBalanceApiProvider = DoubleCheck.provider(new SwitchingProvider<BalanceApi>(singletonCImpl, 25));
      this.providePaymentApiProvider = DoubleCheck.provider(new SwitchingProvider<PaymentApi>(singletonCImpl, 26));
      this.financeRepositoryImplProvider = DoubleCheck.provider(new SwitchingProvider<FinanceRepositoryImpl>(singletonCImpl, 24));
      this.provideReportApiProvider = DoubleCheck.provider(new SwitchingProvider<ReportApi>(singletonCImpl, 28));
      this.reportRepositoryImplProvider = DoubleCheck.provider(new SwitchingProvider<ReportRepositoryImpl>(singletonCImpl, 27));
    }

    @Override
    public Set<Boolean> getDisableFragmentGetContextFix() {
      return Collections.<Boolean>emptySet();
    }

    @Override
    public ActivityRetainedComponentBuilder retainedComponentBuilder() {
      return new ActivityRetainedCBuilder(singletonCImpl);
    }

    @Override
    public ServiceComponentBuilder serviceComponentBuilder() {
      return new ServiceCBuilder(singletonCImpl);
    }

    @Override
    public void injectAbikApp(AbikApp abikApp) {
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // info.prorabka.abik.android.core.prefs.SettingsStore 
          return (T) new SettingsStore(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 1: // info.prorabka.abik.android.core.prefs.SessionManager 
          return (T) new SessionManager(singletonCImpl.tokenStoreProvider.get());

          case 2: // info.prorabka.abik.android.core.prefs.TokenStore 
          return (T) new TokenStore(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 3: // info.prorabka.abik.android.data.repository.AdminRepositoryImpl 
          return (T) new AdminRepositoryImpl(singletonCImpl.provideAdminApiProvider.get());

          case 4: // info.prorabka.abik.android.data.api.AdminApi 
          return (T) NetworkModule_ProvideAdminApiFactory.provideAdminApi(singletonCImpl.provideRetrofitProvider.get());

          case 5: // retrofit2.Retrofit 
          return (T) NetworkModule_ProvideRetrofitFactory.provideRetrofit(singletonCImpl.provideOkHttpProvider.get(), singletonCImpl.provideJsonProvider.get());

          case 6: // okhttp3.OkHttpClient 
          return (T) NetworkModule_ProvideOkHttpFactory.provideOkHttp(singletonCImpl.authInterceptorProvider.get(), singletonCImpl.dynamicBaseUrlInterceptorProvider.get(), singletonCImpl.tokenAuthenticatorProvider.get());

          case 7: // info.prorabka.abik.android.core.network.AuthInterceptor 
          return (T) new AuthInterceptor(singletonCImpl.tokenStoreProvider.get());

          case 8: // info.prorabka.abik.android.core.network.DynamicBaseUrlInterceptor 
          return (T) new DynamicBaseUrlInterceptor(singletonCImpl.baseUrlHolderProvider.get());

          case 9: // info.prorabka.abik.android.core.network.BaseUrlHolder 
          return (T) new BaseUrlHolder(singletonCImpl.settingsStoreProvider.get());

          case 10: // info.prorabka.abik.android.core.network.TokenAuthenticator 
          return (T) new TokenAuthenticator(singletonCImpl.tokenStoreProvider.get(), singletonCImpl.sessionManagerProvider.get(), singletonCImpl.provideAuthApiProvider);

          case 11: // info.prorabka.abik.android.data.api.AuthApi 
          return (T) NetworkModule_ProvideAuthApiFactory.provideAuthApi(singletonCImpl.provideRetrofitProvider.get());

          case 12: // kotlinx.serialization.json.Json 
          return (T) NetworkModule_ProvideJsonFactory.provideJson();

          case 13: // info.prorabka.abik.android.data.repository.GeoRepositoryImpl 
          return (T) new GeoRepositoryImpl(singletonCImpl.provideGeoApiProvider.get());

          case 14: // info.prorabka.abik.android.data.api.GeoApi 
          return (T) NetworkModule_ProvideGeoApiFactory.provideGeoApi(singletonCImpl.provideRetrofitProvider.get());

          case 15: // info.prorabka.abik.android.data.repository.UserRepositoryImpl 
          return (T) new UserRepositoryImpl(singletonCImpl.provideUserApiProvider.get());

          case 16: // info.prorabka.abik.android.data.api.UserApi 
          return (T) NetworkModule_ProvideUserApiFactory.provideUserApi(singletonCImpl.provideRetrofitProvider.get());

          case 17: // info.prorabka.abik.android.data.repository.ChargeRepositoryImpl 
          return (T) new ChargeRepositoryImpl(singletonCImpl.provideChargeApiProvider.get());

          case 18: // info.prorabka.abik.android.data.api.ChargeApi 
          return (T) NetworkModule_ProvideChargeApiFactory.provideChargeApi(singletonCImpl.provideRetrofitProvider.get());

          case 19: // info.prorabka.abik.android.data.repository.TeamRepositoryImpl 
          return (T) new TeamRepositoryImpl(singletonCImpl.provideTeamApiProvider.get(), singletonCImpl.provideInvitationApiProvider.get());

          case 20: // info.prorabka.abik.android.data.api.TeamApi 
          return (T) NetworkModule_ProvideTeamApiFactory.provideTeamApi(singletonCImpl.provideRetrofitProvider.get());

          case 21: // info.prorabka.abik.android.data.api.InvitationApi 
          return (T) NetworkModule_ProvideInvitationApiFactory.provideInvitationApi(singletonCImpl.provideRetrofitProvider.get());

          case 22: // info.prorabka.abik.android.core.CurrentTeamManager 
          return (T) new CurrentTeamManager(singletonCImpl.teamRepositoryImplProvider.get(), singletonCImpl.settingsStoreProvider.get());

          case 23: // info.prorabka.abik.android.data.repository.AuthRepositoryImpl 
          return (T) new AuthRepositoryImpl(singletonCImpl.provideAuthApiProvider.get(), singletonCImpl.tokenStoreProvider.get(), singletonCImpl.sessionManagerProvider.get());

          case 24: // info.prorabka.abik.android.data.repository.FinanceRepositoryImpl 
          return (T) new FinanceRepositoryImpl(singletonCImpl.provideBalanceApiProvider.get(), singletonCImpl.providePaymentApiProvider.get());

          case 25: // info.prorabka.abik.android.data.api.BalanceApi 
          return (T) NetworkModule_ProvideBalanceApiFactory.provideBalanceApi(singletonCImpl.provideRetrofitProvider.get());

          case 26: // info.prorabka.abik.android.data.api.PaymentApi 
          return (T) NetworkModule_ProvidePaymentApiFactory.providePaymentApi(singletonCImpl.provideRetrofitProvider.get());

          case 27: // info.prorabka.abik.android.data.repository.ReportRepositoryImpl 
          return (T) new ReportRepositoryImpl(singletonCImpl.provideReportApiProvider.get(), ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 28: // info.prorabka.abik.android.data.api.ReportApi 
          return (T) NetworkModule_ProvideReportApiFactory.provideReportApi(singletonCImpl.provideRetrofitProvider.get());

          default: throw new AssertionError(id);
        }
      }
    }
  }
}
