package info.prorabka.abik.android.presentation.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.prorabka.abik.android.core.CurrentTeamManager
import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.domain.model.ChargeGroup
import info.prorabka.abik.android.domain.model.ChargeTemplate
import info.prorabka.abik.android.domain.repository.ChargeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Список шаблонов систематических начислений текущей команды. */
@HiltViewModel
class TemplatesViewModel @Inject constructor(
    private val chargeRepository: ChargeRepository,
    private val teamManager: CurrentTeamManager,
) : ViewModel() {

    data class UiState(
        val loading: Boolean = true,
        val templates: List<ChargeTemplate> = emptyList(),
        val groups: Map<Long, String> = emptyMap(),
        val error: String? = null,
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    fun load() {
        val teamId = teamManager.selected.value?.id ?: return
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }
            when (val g = chargeRepository.groups(teamId)) {
                is ApiResult.Success -> _ui.update {
                    it.copy(groups = g.data.associate { grp -> grp.id to grp.name })
                }
                else -> Unit
            }
            when (val t = chargeRepository.templates(teamId)) {
                is ApiResult.Success -> _ui.update {
                    it.copy(templates = t.data, loading = false)
                }
                is ApiResult.HttpError -> _ui.update {
                    it.copy(loading = false, error = t.message)
                }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(loading = false, error = "Нет соединения с сервером")
                }
            }
        }
    }

    fun toggleActive(template: ChargeTemplate) {
        val teamId = teamManager.selected.value?.id ?: return
        viewModelScope.launch {
            when (
                val r = chargeRepository.setTemplateActive(teamId, template.id, !template.active)
            ) {
                is ApiResult.Success -> load()
                is ApiResult.HttpError -> _ui.update { it.copy(error = r.message) }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(error = "Нет соединения с сервером")
                }
            }
        }
    }
}
