package info.prorabka.abik.android.data.repository;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import info.prorabka.abik.android.data.api.InvitationApi;
import info.prorabka.abik.android.data.api.TeamApi;
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
public final class TeamRepositoryImpl_Factory implements Factory<TeamRepositoryImpl> {
  private final Provider<TeamApi> teamApiProvider;

  private final Provider<InvitationApi> invitationApiProvider;

  public TeamRepositoryImpl_Factory(Provider<TeamApi> teamApiProvider,
      Provider<InvitationApi> invitationApiProvider) {
    this.teamApiProvider = teamApiProvider;
    this.invitationApiProvider = invitationApiProvider;
  }

  @Override
  public TeamRepositoryImpl get() {
    return newInstance(teamApiProvider.get(), invitationApiProvider.get());
  }

  public static TeamRepositoryImpl_Factory create(Provider<TeamApi> teamApiProvider,
      Provider<InvitationApi> invitationApiProvider) {
    return new TeamRepositoryImpl_Factory(teamApiProvider, invitationApiProvider);
  }

  public static TeamRepositoryImpl newInstance(TeamApi teamApi, InvitationApi invitationApi) {
    return new TeamRepositoryImpl(teamApi, invitationApi);
  }
}
