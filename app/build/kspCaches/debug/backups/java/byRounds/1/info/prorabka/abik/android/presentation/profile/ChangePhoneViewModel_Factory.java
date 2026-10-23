package info.prorabka.abik.android.presentation.profile;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import info.prorabka.abik.android.domain.repository.UserRepository;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class ChangePhoneViewModel_Factory implements Factory<ChangePhoneViewModel> {
  private final Provider<UserRepository> userRepositoryProvider;

  public ChangePhoneViewModel_Factory(Provider<UserRepository> userRepositoryProvider) {
    this.userRepositoryProvider = userRepositoryProvider;
  }

  @Override
  public ChangePhoneViewModel get() {
    return newInstance(userRepositoryProvider.get());
  }

  public static ChangePhoneViewModel_Factory create(
      Provider<UserRepository> userRepositoryProvider) {
    return new ChangePhoneViewModel_Factory(userRepositoryProvider);
  }

  public static ChangePhoneViewModel newInstance(UserRepository userRepository) {
    return new ChangePhoneViewModel(userRepository);
  }
}
