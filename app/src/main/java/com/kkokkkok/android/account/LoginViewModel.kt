package com.kkokkkok.android.account

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LoginPage { LOGIN, TERMS, COMPLETION, DONE }

data class LoginUiState(
    val page: LoginPage,
    val account: AccountUiState = AccountUiState(),
    val busy: Boolean = false,
)

/** Real login flow for builds with LOGIN_REQUIRED: login → (new user) terms → completion → app. */
class LoginViewModel(private val repository: LoginRepository) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState(initialPage()))
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    private fun initialPage(): LoginPage {
        val session = AccountSession.current() ?: return LoginPage.LOGIN
        return if (AccountSession.termsAccepted(session.userId)) LoginPage.DONE else LoginPage.TERMS
    }

    /** Re-evaluates after the session changed elsewhere (expiry, logout). */
    fun sync() = _state.update { it.copy(page = initialPage(), busy = false) }

    fun signIn(provider: SignInProvider, context: Context) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, account = it.account.copy(provider = provider, message = null)) }
        viewModelScope.launch {
            when (val result = repository.signIn(provider, context)) {
                is LoginResult.SignedIn -> {
                    val needsTerms = !result.termsAccepted
                    _state.update {
                        it.copy(busy = false, page = if (needsTerms) LoginPage.TERMS else LoginPage.DONE,
                            // Real consent starts unchecked; the user must agree explicitly.
                            account = it.account.copy(agreements = AgreementState(false, false, false, false)))
                    }
                }
                LoginResult.Cancelled -> _state.update { it.copy(busy = false) }
                is LoginResult.Failed -> _state.update {
                    it.copy(busy = false, account = it.account.copy(message = result.message, loginAttemptId = it.account.loginAttemptId + 1))
                }
            }
        }
    }

    fun toggleAll() = _state.update { current ->
        val accepted = !current.account.agreements.allAccepted
        current.copy(account = current.account.copy(agreements = AgreementState(accepted, accepted, accepted, accepted)))
    }

    fun toggle(agreement: Agreement) = _state.update { current ->
        val previous = current.account.agreements
        val next = when (agreement) {
            Agreement.SERVICE_TERMS -> previous.copy(serviceTerms = !previous.serviceTerms)
            Agreement.PRIVACY -> previous.copy(privacy = !previous.privacy)
            Agreement.LOCATION -> previous.copy(location = !previous.location)
            Agreement.MARKETING -> previous.copy(marketing = !previous.marketing)
        }
        current.copy(account = current.account.copy(agreements = next))
    }

    fun showTermsUnavailable() = _state.update { it.copy(account = it.account.copy(message = "약관 전문은 아직 준비되지 않았어요.")) }

    fun acceptTerms() {
        val current = _state.value
        if (current.busy || !current.account.agreements.requiredAccepted) return
        if (AccountSession.current() == null) return sync()
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            val error = repository.agreeTerms(current.account.agreements)
            _state.update {
                if (error == null) it.copy(busy = false, page = LoginPage.COMPLETION)
                else it.copy(busy = false, account = it.account.copy(message = error))
            }
        }
    }

    /** Leaving signup before agreeing ends the new session so the user is not half signed up. */
    fun cancelSignup() {
        viewModelScope.launch {
            repository.logout()
            _state.update { LoginUiState(LoginPage.LOGIN) }
        }
    }

    fun finishSignup(openPlaceAdd: Boolean) {
        AccountSession.openPlaceAddOnStart = openPlaceAdd
        _state.update { it.copy(page = LoginPage.DONE) }
    }

    fun clearMessage() = _state.update { it.copy(account = it.account.copy(message = null)) }

    fun logout(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.logout()
            _state.update { LoginUiState(LoginPage.LOGIN) }
            onDone()
        }
    }
}

class LoginViewModelFactory(private val repository: LoginRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = LoginViewModel(repository) as T
}
