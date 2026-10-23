package info.prorabka.abik.android.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.core.util.PhoneUtils
import info.prorabka.abik.android.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Смена номера: подтверждение старого → новый номер → подтверждение нового.
 * После смены сервер отзывает refresh-токены — клиент уходит на вход.
 */
@HiltViewModel
class ChangePhoneViewModel @Inject constructor(
    private val userRepository: UserRepository,
) : ViewModel() {

    enum class Step { OLD_CODE, NEW_PHONE, NEW_CODE }

    data class UiState(
        val step: Step = Step.OLD_CODE,
        val oldCode: String = "",
        val newPhone: String = "",
        val newCode: String = "",
        val changeToken: String? = null,
        val loading: Boolean = false,
        val error: String? = null,
        val started: Boolean = false,
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    fun onOldCode(v: String) = _ui.update { it.copy(oldCode = v.filter(Char::isDigit).take(4), error = null) }
    fun onNewPhone(v: String) = _ui.update { it.copy(newPhone = v, error = null) }
    fun onNewCode(v: String) = _ui.update { it.copy(newCode = v.filter(Char::isDigit).take(4), error = null) }

    fun start() {
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }
            when (val r = userRepository.phoneChangeStart()) {
                is ApiResult.Success -> _ui.update { it.copy(loading = false, started = true) }
                is ApiResult.HttpError -> _ui.update { it.copy(loading = false, error = r.message) }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(loading = false, error = "Нет соединения с сервером")
                }
            }
        }
    }

    fun verifyOld() {
        val code = _ui.value.oldCode
        if (code.length != 4) {
            _ui.update { it.copy(error = "Введите 4-значный код") }
            return
        }
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }
            when (val r = userRepository.phoneChangeVerifyOld(code)) {
                is ApiResult.Success -> _ui.update {
                    it.copy(loading = false, step = Step.NEW_PHONE, changeToken = r.data)
                }
                is ApiResult.HttpError -> _ui.update { it.copy(loading = false, error = r.message) }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(loading = false, error = "Нет соединения с сервером")
                }
            }
        }
    }

    fun sendNew() {
        val phone = PhoneUtils.normalize(_ui.value.newPhone)
        val token = _ui.value.changeToken ?: return
        if (phone == null) {
            _ui.update { it.copy(error = "Введите корректный новый номер") }
            return
        }
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }
            when (val r = userRepository.phoneChangeSendNew(token, phone)) {
                is ApiResult.Success -> _ui.update {
                    it.copy(loading = false, step = Step.NEW_CODE, newPhone = phone)
                }
                is ApiResult.HttpError -> _ui.update { it.copy(loading = false, error = r.message) }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(loading = false, error = "Нет соединения с сервером")
                }
            }
        }
    }

    fun confirm(onDone: () -> Unit) {
        val s = _ui.value
        val token = s.changeToken ?: return
        if (s.newCode.length != 4) {
            _ui.update { it.copy(error = "Введите 4-значный код") }
            return
        }
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }
            when (val r = userRepository.phoneChangeConfirm(token, s.newCode)) {
                is ApiResult.Success -> onDone()
                is ApiResult.HttpError -> _ui.update { it.copy(loading = false, error = r.message) }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(loading = false, error = "Нет соединения с сервером")
                }
            }
        }
    }
}
