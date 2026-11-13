package com.kkokkkok.android.ui.account

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.kkokkkok.android.BuildConfig
import com.kkokkkok.android.account.AccountSession
import com.kkokkkok.android.account.LoginPage
import com.kkokkkok.android.account.LoginViewModel

/** Logout action for screens inside the login gate; null when the build does not use login. */
val LocalLogout = staticCompositionLocalOf<(() -> Unit)?> { null }

@Composable
fun LoginGate(viewModel: LoginViewModel, content: @Composable () -> Unit) {
    if (!BuildConfig.LOGIN_REQUIRED) { content(); return }
    val generation by AccountSession.generation.collectAsStateWithLifecycle()
    LaunchedEffect(generation) { viewModel.sync() }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    when (state.page) {
        LoginPage.DONE -> {
            // Feature ViewModels are disposed when the user changes so no screen state leaks across accounts.
            val store = remember(generation) { ViewModelStore() }
            val owner = remember(store) { object : ViewModelStoreOwner { override val viewModelStore = store } }
            DisposableEffect(store) { onDispose { store.clear() } }
            CompositionLocalProvider(
                LocalViewModelStoreOwner provides owner,
                LocalLogout provides { viewModel.logout() },
                content = content,
            )
        }
        LoginPage.LOGIN -> AccountLoginScreen(
            state = state.account,
            onProvider = { provider -> viewModel.signIn(provider, context) },
            onDismissMessage = viewModel::clearMessage,
        )
        LoginPage.TERMS -> {
            BackHandler(onBack = viewModel::cancelSignup)
            AccountTermsScreen(
                state = state.account,
                onBack = viewModel::cancelSignup,
                onToggleAll = viewModel::toggleAll,
                onToggle = viewModel::toggle,
                onOpenTerms = viewModel::showTermsUnavailable,
                onContinue = viewModel::acceptTerms,
                onDismissMessage = viewModel::clearMessage,
                isPreview = false,
            )
        }
        LoginPage.COMPLETION -> {
            BackHandler { viewModel.finishSignup(openPlaceAdd = false) }
            SignupCompletionScreen(
                onBack = { viewModel.finishSignup(openPlaceAdd = false) },
                onRegisterPlace = { viewModel.finishSignup(openPlaceAdd = true) },
                onStart = { viewModel.finishSignup(openPlaceAdd = false) },
                onLater = { viewModel.finishSignup(openPlaceAdd = false) },
                displayName = AccountSession.current()?.displayName ?: "꼭꼭 사용자",
            )
        }
    }
}
