package com.kkokkkok.android.account

/** Account authentication is intentionally unavailable until a real auth backend is agreed. */
interface AccountRepository {
    suspend fun signIn(provider: SignInProvider): AccountResult
    suspend fun completeSignupPreview(): AccountResult
}

enum class SignInProvider(val displayName: String) {
    KAKAO("카카오"),
    NAVER("네이버"),
}

sealed interface AccountResult {
    data object PreviewCompleted : AccountResult
    data class Unavailable(val message: String) : AccountResult
}

/** A UI-only mock. It never stores credentials or creates an account. */
class MockAccountRepository : AccountRepository {
    override suspend fun signIn(provider: SignInProvider): AccountResult = AccountResult.Unavailable(
        "${provider.displayName} 로그인에 실패했어요. 잠시 후 다시 시도해주세요.",
    )

    override suspend fun completeSignupPreview(): AccountResult = AccountResult.PreviewCompleted
}
