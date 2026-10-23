package info.prorabka.abik.android.presentation.team;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import info.prorabka.abik.android.core.CurrentTeamManager;
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
public final class MemberDetailsViewModel_Factory implements Factory<MemberDetailsViewModel> {
  private final Provider<UserRepository> userRepositoryProvider;

  private final Provider<CurrentTeamManager> teamManagerProvider;

  public MemberDetailsViewModel_Factory(Provider<UserRepository> userRepositoryProvider,
      Provider<CurrentTeamManager> teamManagerProvider) {
    this.userRepositoryProvider = userRepositoryProvider;
    this.teamManagerProvider = teamManagerProvider;
  }

  @Override
  public MemberDetailsViewModel get() {
    return newInstance(userRepositoryProvider.get(), teamManagerProvider.get());
  }

  public static MemberDetailsViewModel_Factory create(
      Provider<UserRepository> userRepositoryProvider,
      Provider<CurrentTeamManager> teamManagerProvider) {
    return new MemberDetailsViewModel_Factory(userRepositoryProvider, teamManagerProvider);
  }

  public static MemberDetailsViewModel newInstance(UserRepository userRepository,
      CurrentTeamManager teamManager) {
    return new MemberDetailsViewModel(userRepository, teamManager);
  }
}
