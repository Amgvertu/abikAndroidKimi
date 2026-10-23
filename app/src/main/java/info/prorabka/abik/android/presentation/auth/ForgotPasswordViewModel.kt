package info.prorabka.abik.android.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.core.util.PhoneUtils
import info.prorabka.abik.android.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    data class UiState(
        val phone: String = "",
        val code: String = "",
        val newPassword: String = "",
        val newPasswordRepeat: String = "",
        val step: Step = Step.PHONE,
        val loading: Boolean = false,
        val error: String? = null,
    )

    enum class Step { PHONE, CODE }

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    fun onPhoneChange(v: String) = _ui.update { it.copy(phone = v, error = null) }
    fun onCodeChange(v: String) = _ui.update { it.copy(code = v.filter(Char::isDigit).take(4), error = null) }
    fun onNewPasswordChange(v: String) = _ui.update { it.copy(newPassword = v, error = null) }
    fun onNewPasswordRepeatChange(v: String) = _ui.update { it.copy(newPasswordRepeat = v, error = null) }

    fun sendCode() {
        val phone = PhoneUtils.normalize(_ui.value.phone)
        if (phone == null) {
            _ui.update { it.copy(error = "Введите корректный номер телефона") }
            return
        }
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }
            when (val r = authRepository.passwordResetStart(phone)) {
                is ApiResult.Success -> _ui.update {
                    it.copy(loading = false, step = Step.CODE, phone = phone)
                }
                is ApiResult.HttpError -> _ui.update { it.copy(loading = false, error = r.message) }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(loading = false, error = "Нет соединения с сервером")
                }
            }
        }
    }

    fun confirm(onSuccess: () -> Unit) {
        val s = _ui.value
        when {
            s.code.length != 4 -> _ui.update { it.copy(error = "Введите 4-значный код из SMS") }
            s.newPassword.length < 8 -> _ui.update { it.copy(error = "Пароль — минимум 8 символов") }
            s.newPassword != s.newPasswordRepeat ->
                _ui.update { it.copy(error = "Пароли не совпадают") }
            else -> viewModelScope.launch {
                _ui.update { it.copy(loading = true, error = null) }
                when (val r = authRepository.passwordResetConfirm(s.phone, s.code, s.newPassword)) {
                    is ApiResult.Success -> onSuccess()
                    is ApiResult.HttpError -> _ui.update { it.copy(loading = false, error = r.message) }
                    is ApiResult.NetworkError -> _ui.update {
                        it.copy(loading = false, error = "Нет соединения с сервером")
                    }
                }
            }
        }
    }
}
