package com.kkokkkok.android.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class AgreementState(
    val serviceTerms: Boolean = true,
    val privacy: Boolean = true,
    val location: Boolean = true,
    val marketing: Boolean = false,
) {
    val requiredAccepted: Boolean get() = serviceTerms && privacy && location
    val allAccepted: Boolean get() = requiredAccepted && marketing
}

data class AccountUiState(
    val provider: SignInProvider? = null,
    val agreements: AgreementState = AgreementState(),
    val message: String? = null,
    val loginAttemptId: Int = 0,
) 

class AccountViewModel(
    private val repository: AccountRepository = MockAccountRepository(),
) : ViewModel() {
    private val _state = MutableStateFlow(AccountUiState())
    val state: StateFlow<AccountUiState> = _state.asStateFlow()

    fun previewSignup(provider: SignInProvider) {
        _state.update {
            it.copy(
                provider = provider,
                agreements = AgreementState(),
                message = null,
            )
        }
    }

    fun toggleAllAgreements() {
        _state.update { current ->
            val accepted = !current.agreements.allAccepted
            current.copy(agreements = AgreementState(accepted, accepted, accepted, accepted))
        }
    }

    fun toggleAgreement(agreement: Agreement) {
        _state.update { current ->
            val previous = current.agreements
            val next = when (agreement) {
                Agreement.SERVICE_TERMS -> previous.copy(serviceTerms = !previous.serviceTerms)
                Agreement.PRIVACY -> previous.copy(privacy = !previous.privacy)
                Agreement.LOCATION -> previous.copy(location = !previous.location)
                Agreement.MARKETING -> previous.copy(marketing = !previous.marketing)
            }
            current.copy(agreements = next)
        }
    }

    fun showTermsUnavailable() {
        _state.update { it.copy(message = "약관 전문은 아직 준비되지 않았어요.") }
    }

    fun continueSignupPreview() {
        val current = _state.value
        val provider = current.provider ?: return
        if (!current.agreements.requiredAccepted) return

        viewModelScope.launch {
            val result = repository.signIn(provider)
            when (result) {
                is AccountResult.Unavailable -> _state.update {
                    it.copy(message = result.message, loginAttemptId = it.loginAttemptId + 1)
                }
            }
        }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }

    fun clearProvider() = _state.update { it.copy(provider = null, message = null) }
}

enum class Agreement {
    SERVICE_TERMS,
    PRIVACY,
    LOCATION,
    MARKETING,
}
