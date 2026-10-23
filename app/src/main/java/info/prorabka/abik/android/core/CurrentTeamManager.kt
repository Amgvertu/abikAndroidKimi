package info.prorabka.abik.android.core

import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.core.prefs.SettingsStore
import info.prorabka.abik.android.domain.model.Team
import info.prorabka.abik.android.domain.repository.TeamRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Текущая выбранная команда. Список команд и выбор живут здесь —
 * тулбар, главная, абонемент и экран команды читают одно состояние.
 */
@Singleton
class CurrentTeamManager @Inject constructor(
    private val teamRepository: TeamRepository,
    private val settingsStore: SettingsStore,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _teams = MutableStateFlow<List<Team>>(emptyList())
    val teams: StateFlow<List<Team>> = _teams

    private val _selected = MutableStateFlow<Team?>(null)
    val selected: StateFlow<Team?> = _selected

    /** Перезагрузить список команд; выбрать сохранённую или первую. */
    suspend fun refresh(): ApiResult<List<Team>> {
        val result = teamRepository.myTeams()
        if (result is ApiResult.Success) {
            _teams.value = result.data
            val savedId = settingsStore.selectedTeamId.first()
            val current = result.data.firstOrNull { it.id == savedId }
                ?: result.data.firstOrNull { it.id == _selected.value?.id }
                ?: result.data.firstOrNull()
            _selected.value = current
        }
        return result
    }

    fun select(team: Team) {
        _selected.value = team
        scope.launch { settingsStore.setSelectedTeamId(team.id) }
    }
}
