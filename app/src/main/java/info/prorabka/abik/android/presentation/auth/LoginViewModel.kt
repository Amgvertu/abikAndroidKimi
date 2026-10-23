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
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    data class UiState(
        val phone: String = "",
        val password: String = "",
        val loading: Boolean = false,
        val error: String? = null,
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    fun onPhoneChange(value: String) = _ui.update { it.copy(phone = value, error = null) }
    fun onPasswordChange(value: String) = _ui.update { it.copy(password = value, error = null) }

    fun login(onSuccess: () -> Unit) {
        val state = _ui.value
        val phone = PhoneUtils.normalize(state.phone)
        if (phone == null) {
            _ui.update { it.copy(error = "Введите номер телефона") }
            return
        }
        if (state.password.isBlank()) {
            _ui.update { it.copy(error = "Введите пароль") }
            return
        }
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }
            when (val result = authRepository.login(phone, state.password)) {
                is ApiResult.Success -> onSuccess()
                is ApiResult.HttpError -> _ui.update {
                    it.copy(loading = false, error = result.message)
                }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(loading = false, error = "Нет соединения с сервером")
                }
            }
        }
    }
}
