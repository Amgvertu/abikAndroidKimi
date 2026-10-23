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
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    data class UiState(
        val phone: String = "",
        val password: String = "",
        val passwordRepeat: String = "",
        val acceptedPd: Boolean = false,
        val code: String = "",
        val step: Step = Step.FORM,
        val loading: Boolean = false,
        val error: String? = null,
    )

    enum class Step { FORM, CODE }

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    fun onPhoneChange(v: String) = _ui.update { it.copy(phone = v, error = null) }
    fun onPasswordChange(v: String) = _ui.update { it.copy(password = v, error = null) }
    fun onPasswordRepeatChange(v: String) = _ui.update { it.copy(passwordRepeat = v, error = null) }
    fun onAcceptedPdChange(v: Boolean) = _ui.update { it.copy(acceptedPd = v, error = null) }
    fun onCodeChange(v: String) = _ui.update { it.copy(code = v.filter(Char::isDigit).take(4), error = null) }

    fun start() {
        val s = _ui.value
        val phone = PhoneUtils.normalize(s.phone)
        when {
            phone == null -> _ui.update { it.copy(error = "Введите корректный номер телефона") }
            s.password.length < 8 -> _ui.update { it.copy(error = "Пароль — минимум 8 символов") }
            s.password != s.passwordRepeat -> _ui.update { it.copy(error = "Пароли не совпадают") }
            !s.acceptedPd -> _ui.update { it.copy(error = "Нужно согласие на обработку персональных данных") }
            else -> {
                viewModelScope.launch {
                    _ui.update { it.copy(loading = true, error = null) }
                    when (val r = authRepository.registerStart(phone, s.password, s.acceptedPd)) {
                        is ApiResult.Success -> _ui.update {
                            it.copy(loading = false, step = Step.CODE, phone = phone)
                        }
                        is ApiResult.HttpError -> _ui.update {
                            it.copy(loading = false, error = r.message)
                        }
                        is ApiResult.NetworkError -> _ui.update {
                            it.copy(loading = false, error = "Нет соединения с сервером")
                        }
                    }
                }
            }
        }
    }

    fun confirm(onSuccess: () -> Unit) {
        val s = _ui.value
        if (s.code.length != 4) {
            _ui.update { it.copy(error = "Введите 4-значный код из SMS") }
            return
        }
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }
            when (val r = authRepository.registerConfirm(s.phone, s.code)) {
                is ApiResult.Success -> onSuccess()
                is ApiResult.HttpError -> _ui.update {
                    it.copy(loading = false, error = r.message)
                }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(loading = false, error = "Нет соединения с сервером")
                }
            }
        }
    }

    /** Повторная отправка кода = повторный register/start (сервер перевыпускает код для PENDING). */
    fun resend() {
        val s = _ui.value
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }
            when (val r = authRepository.registerStart(s.phone, s.password, s.acceptedPd)) {
                is ApiResult.Success -> _ui.update { it.copy(loading = false, error = null) }
                is ApiResult.HttpError -> _ui.update { it.copy(loading = false, error = r.message) }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(loading = false, error = "Нет соединения с сервером")
                }
            }
        }
    }
}
