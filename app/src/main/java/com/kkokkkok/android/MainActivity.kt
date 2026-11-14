package com.kkokkkok.android

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kkokkkok.android.analysis.AnalysisState
import com.kkokkkok.android.analysis.BackendAnalysisRepository
import com.kkokkkok.android.ui.analysis.AnalysisFlowScreen
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kkokkkok.android.ui.home.HomeScreen
import com.kkokkkok.android.ui.photo.PhotoSelectionViewModel
import com.kkokkkok.android.ui.photo.PhotoSelectionViewModelFactory
import com.kkokkkok.android.ui.photo.PhotoLocationScreen
import com.kkokkkok.android.ui.photo.PhotoAccessInfoDialog
import com.kkokkkok.android.protection.ProtectionEditViewModel
import com.kkokkkok.android.protection.ProtectionEditViewModelFactory
import com.kkokkkok.android.protection.BackendProtectionEditRepository
import com.kkokkkok.android.protection.ProtectionStep
import com.kkokkkok.android.ui.protection.ProtectionEditFlow
import com.kkokkkok.android.account.AccountViewModel
import com.kkokkkok.android.account.SignInProvider
import com.kkokkkok.android.ui.account.AccountLoginScreen
import com.kkokkkok.android.ui.account.AccountTermsScreen
import com.kkokkkok.android.ui.account.SignupCompletionScreen
import com.kkokkkok.android.location.SensitiveLocationViewModel
import com.kkokkkok.android.location.SensitiveLocationViewModelFactory
import com.kkokkkok.android.location.BackendSensitiveLocationRepository
import com.kkokkkok.android.ui.location.SensitiveLocationFlow
import com.kkokkkok.android.history.HistoryViewModel
import com.kkokkkok.android.history.HistoryViewModelFactory
import com.kkokkkok.android.history.BackendHistoryRepository
import com.kkokkkok.android.history.HistoryRecord
import com.kkokkkok.android.ui.history.HistoryRecordsScreen
import com.kkokkkok.android.ui.history.HistoryRecordResultScreen
import com.kkokkkok.android.protectionhistory.ProtectionHistoryViewModel
import com.kkokkkok.android.ui.protectionhistory.ProtectionHistoryScreen
import com.kkokkkok.android.profile.ProfileViewModel
import com.kkokkkok.android.ui.profile.MyProfileScreen
import com.kkokkkok.android.snscheck.SnsCumulativeViewModel
import com.kkokkkok.android.snscheck.SnsCumulativeViewModelFactory
import com.kkokkkok.android.snscheck.BackendSnsCumulativeRepository
import com.kkokkkok.android.ui.snscheck.SnsCumulativeFlow

private enum class AccountPage { LOGIN, TERMS, COMPLETION }

class MainActivity : ComponentActivity() {
    override fun onResume() {
        super.onResume()
        // Back from the Naver login tab without a return link means the user closed it.
        com.kkokkkok.android.account.NaverLoginBridge.appResumed()
    }

    override fun onPause() {
        com.kkokkkok.android.account.NaverLoginBridge.appPaused()
        super.onPause()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        com.kkokkkok.android.pilot.PilotCredentials.initialize(this)
        com.kkokkkok.android.account.AccountSession.initialize(this)
        if (BuildConfig.KAKAO_NATIVE_APP_KEY.isNotBlank()) {
            com.kakao.sdk.common.KakaoSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)
            // The Kakao map SDK ships only ARM native libraries; x86 emulators show a map-unavailable notice instead.
            if (android.os.Build.SUPPORTED_ABIS.any { it == "arm64-v8a" || it == "armeabi-v7a" }) {
                runCatching { com.kakao.vectormap.KakaoMapSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY) }
            }
        }
        val loginFactory = com.kkokkkok.android.account.LoginViewModelFactory(
            com.kkokkkok.android.account.BackendLoginRepository(com.kkokkkok.android.network.InstallationIdentity(this)),
        )
        val selectionFactory = PhotoSelectionViewModelFactory(
            BackendAnalysisRepository(contentResolver, BuildConfig.BACKEND_BASE_URL),
        )
        val historyFactory = HistoryViewModelFactory(
            BackendHistoryRepository(this, BuildConfig.BACKEND_BASE_URL),
        )
        val protectionFactory = ProtectionEditViewModelFactory(
            BackendProtectionEditRepository(this, BuildConfig.BACKEND_BASE_URL),
        )
        val sensitiveLocationFactory = SensitiveLocationViewModelFactory(
            BackendSensitiveLocationRepository(this, BuildConfig.BACKEND_BASE_URL),
        )
        val protectionHistoryFactory = com.kkokkkok.android.protectionhistory.ProtectionHistoryViewModelFactory(
            com.kkokkkok.android.protectionhistory.BackendProtectionHistoryRepository(this, BuildConfig.BACKEND_BASE_URL),
        )
        val cumulativeFactory = SnsCumulativeViewModelFactory(
            BackendSnsCumulativeRepository(this, BuildConfig.BACKEND_BASE_URL),
        )
        val profileFactory = com.kkokkkok.android.profile.ProfileViewModelFactory(
            com.kkokkkok.android.profile.BackendProfileRepository(this, BuildConfig.BACKEND_BASE_URL),
        )
        setContent {
            com.kkokkkok.android.pilot.PilotRegistrationGate {
            com.kkokkkok.android.ui.account.LoginGate(viewModel(factory = loginFactory)) {
            KkokKkokApp(
                selection = viewModel(factory = selectionFactory),
                history = viewModel(factory = historyFactory),
                protection = viewModel(factory = protectionFactory),
                sensitiveLocations = viewModel(factory = sensitiveLocationFactory),
                snsCumulative = viewModel(factory = cumulativeFactory),
                protectionHistory = viewModel(factory = protectionHistoryFactory),
                profile = viewModel(factory = profileFactory),
            )
            }
            }
        }
    }
}

@Composable
internal fun KkokKkokApp(
    selection: PhotoSelectionViewModel = viewModel(),
    protection: ProtectionEditViewModel = viewModel(),
    account: AccountViewModel = viewModel(),
    sensitiveLocations: SensitiveLocationViewModel = viewModel(),
    history: HistoryViewModel = viewModel(),
    protectionHistory: ProtectionHistoryViewModel = viewModel(),
    profile: ProfileViewModel = viewModel(),
    snsCumulative: SnsCumulativeViewModel = viewModel(),
) {
    val photo by selection.selectedPhoto.collectAsStateWithLifecycle()
    val photos by selection.selectedPhotos.collectAsStateWithLifecycle()
    val batchStates by selection.batchAnalysisStates.collectAsStateWithLifecycle()
    val resultPage by selection.resultPageIndex.collectAsStateWithLifecycle()
    val needsPhotoLocation by selection.needsLocationInput.collectAsStateWithLifecycle()
    val analysis by selection.analysisState.collectAsStateWithLifecycle()
    val location by selection.locationState.collectAsStateWithLifecycle()
    val edit by protection.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val protectionPicker = remember { mutableStateOf(false) }
    val showPhotoAccessInfo = remember { mutableStateOf(false) }
    val accountPage = remember { mutableStateOf<AccountPage?>(null) }
    val showSensitiveLocations = remember { mutableStateOf(false) }
    val showHistory = remember { mutableStateOf(false) }
    val showProtectionHistory = remember { mutableStateOf(false) }
    val returnToProtectionHistory = remember { mutableStateOf(false) }
    val showMyProfile = remember { mutableStateOf(false) }
    val showSnsCheck = remember { mutableStateOf(false) }
    val returnToMyProfileFromSensitive = remember { mutableStateOf(false) }
    val returnToSnsFromSensitive = remember { mutableStateOf(false) }
    val returnToSignupFromSensitive = remember { mutableStateOf(false) }
    val selectedHistoryRecord = remember { mutableStateOf<HistoryRecord?>(null) }
    val cumulativeEditPhotos = remember { mutableStateOf<List<com.kkokkkok.android.protection.ProtectionSource>>(emptyList()) }
    val cumulativeEditIndex = remember { mutableStateOf(0) }
    val cumulativeEditFromHistory = remember { mutableStateOf(false) }
    val cumulativeState by snsCumulative.state.collectAsStateWithLifecycle()
    fun startCumulativeEdit(sources: List<com.kkokkkok.android.protection.ProtectionSource>, fromHistory: Boolean) {
        if (sources.isEmpty()) return
        snsCumulative.state.value.protectionMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
        cumulativeEditPhotos.value = sources
        cumulativeEditIndex.value = 0
        cumulativeEditFromHistory.value = fromHistory
        showSnsCheck.value = false
        showHistory.value = false
        protection.begin(sources.first().photo, sources.first().result)
    }
    fun finishCumulativeEdit() {
        protection.leave()
        cumulativeEditPhotos.value = emptyList()
        if (cumulativeEditFromHistory.value) showHistory.value = true else showSnsCheck.value = true
    }
    fun finishInspection() {
        snsCumulative.cancelProtectionPreparation()
        selection.cancelPhotoLocation()
        protection.leave()
        cumulativeEditPhotos.value = emptyList()
        returnToProtectionHistory.value = false
        selectedHistoryRecord.value = null
        history.retry()
        showHistory.value = false
        showProtectionHistory.value = false
        showMyProfile.value = false
        showSnsCheck.value = false
        showSensitiveLocations.value = false
    }
    LaunchedEffect(Unit) {
        if (com.kkokkkok.android.account.AccountSession.openPlaceAddOnStart) {
            com.kkokkkok.android.account.AccountSession.openPlaceAddOnStart = false
            sensitiveLocations.beginAdd()
            showSensitiveLocations.value = true
        }
    }
    val accountState by account.state.collectAsStateWithLifecycle()
    LaunchedEffect(accountState.loginAttemptId) {
        if (accountState.loginAttemptId > 0) accountPage.value = AccountPage.LOGIN
    }
    LaunchedEffect(accountState.signupCompleted) {
        if (accountState.signupCompleted) accountPage.value = AccountPage.COMPLETION
    }
    val picker = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null) {
            if (protectionPicker.value) {
                protectionPicker.value = false
                selection.onPhotoPicked(uri)
            } else {
                selection.onPhotoPicked(uri)
            }
        }
    }
    val multiPicker = rememberLauncherForActivityResult(PickMultipleVisualMedia(maxItems = 20)) { uris ->
        if (!protectionPicker.value) selection.onPhotosPicked(uris)
    }
    if (showPhotoAccessInfo.value) {
        PhotoAccessInfoDialog(
            allowMultiple = !protectionPicker.value,
            onDismiss = { showPhotoAccessInfo.value = false },
            onContinue = {
                showPhotoAccessInfo.value = false
                if (protectionPicker.value) picker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
                else multiPicker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
            },
        )
    }
    if (accountPage.value == AccountPage.COMPLETION) {
        BackHandler {
            account.clearSignupCompleted()
            accountPage.value = AccountPage.TERMS
        }
        SignupCompletionScreen(
            onBack = {
                account.clearSignupCompleted()
                accountPage.value = AccountPage.TERMS
            },
            onRegisterPlace = {
                returnToSignupFromSensitive.value = true
                accountPage.value = null
                sensitiveLocations.beginAdd()
                showSensitiveLocations.value = true
            },
            onStart = {
                account.clearSignupCompleted()
                account.clearProvider()
                accountPage.value = null
            },
            onLater = {
                account.clearSignupCompleted()
                account.clearProvider()
                accountPage.value = null
            },
        )
    } else if (accountPage.value == AccountPage.LOGIN) {
        BackHandler {
            account.clearProvider()
            accountPage.value = null
        }
        AccountLoginScreen(
            state = accountState,
            onProvider = { provider ->
                account.previewSignup(provider)
                accountPage.value = AccountPage.TERMS
            },
            onDismissMessage = account::clearMessage,
            onOpenSensitiveLocations = {
                account.clearProvider()
                returnToMyProfileFromSensitive.value = false
                showSensitiveLocations.value = true
            },
        )
    } else if (accountPage.value == AccountPage.TERMS) {
        BackHandler {
            account.clearMessage()
            accountPage.value = AccountPage.LOGIN
        }
        AccountTermsScreen(
            state = accountState,
            onBack = {
                account.clearMessage()
                accountPage.value = AccountPage.LOGIN
            },
            onToggleAll = account::toggleAllAgreements,
            onToggle = account::toggleAgreement,
            onOpenTerms = account::showTermsUnavailable,
            onContinue = account::completeSignupPreview,
            onDismissMessage = account::clearMessage,
        )
    } else if (showSnsCheck.value) {
        SnsCumulativeFlow(
            viewModel = snsCumulative,
            onClose = { showSnsCheck.value = false },
            onAddPlace = {
                returnToMyProfileFromSensitive.value = false
                returnToSnsFromSensitive.value = true
                showSnsCheck.value = false
                sensitiveLocations.beginAdd()
                showSensitiveLocations.value = true
            },
            onMessage = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() },
            onProtect = { startCumulativeEdit(it, false) },
        )
    } else if (showHistory.value) {
        BackHandler {
            snsCumulative.cancelProtectionPreparation()
            if (selectedHistoryRecord.value != null) selectedHistoryRecord.value = null
            else showHistory.value = false
        }
        val selectedRecord = selectedHistoryRecord.value
        if (selectedRecord != null) {
            HistoryRecordResultScreen(selectedRecord,
                onFinish = { finishInspection() },
                onCumulativeProtect = selectedRecord.cumulativeResult?.let { result ->
                    { snsCumulative.prepareProtection(result) { startCumulativeEdit(it, true) } }
                },
                preparingProtection = cumulativeState.preparingProtection,
                protectionMessage = cumulativeState.protectionMessage,
                onProtect = {
                val savedPhoto = selectedRecord.photoUri
                val savedResult = selectedRecord.result
                if (savedPhoto != null && savedResult != null) {
                    protection.begin(savedPhoto, savedResult)
                    showHistory.value = false
                }
            }) { snsCumulative.cancelProtectionPreparation(); selectedHistoryRecord.value = null }
        } else {
            HistoryRecordsScreen(
                viewModel = history,
                onHome = { showHistory.value = false },
                onProtectionHistory = {
                    showHistory.value = false
                    protectionHistory.retry()
                    showProtectionHistory.value = true
                },
                onAccount = {
                    showHistory.value = false
                    showProtectionHistory.value = false
                    showMyProfile.value = true
                },
                onOpenRecord = { selectedHistoryRecord.value = it },
            )
        }
    } else if (showProtectionHistory.value) {
        BackHandler { showProtectionHistory.value = false }
        ProtectionHistoryScreen(
            viewModel = protectionHistory,
            onReedit = { record ->
                val original = record.originalUri
                val result = record.originalResult
                if (original != null && result != null) {
                    protection.begin(original, result)
                    returnToProtectionHistory.value = true
                    showProtectionHistory.value = false
                }
            },
            onHome = { showProtectionHistory.value = false },
            onInspectionHistory = {
                showProtectionHistory.value = false
                showHistory.value = true
            },
            onAccount = {
                showProtectionHistory.value = false
                showMyProfile.value = true
            },
        )
    } else if (showMyProfile.value) {
        BackHandler { showMyProfile.value = false }
        MyProfileScreen(
            viewModel = profile,
            onLogout = com.kkokkkok.android.ui.account.LocalLogout.current,
            onDataDeleted = {
                android.widget.Toast.makeText(context, "서비스 데이터를 삭제했어요.", android.widget.Toast.LENGTH_LONG).show()
                context.startActivity(android.content.Intent(context, MainActivity::class.java)
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK))
            },
            onHome = { showMyProfile.value = false },
            onInspectionHistory = {
                showMyProfile.value = false
                showHistory.value = true
            },
            onProtectionHistory = {
                showMyProfile.value = false
                protectionHistory.retry()
                showProtectionHistory.value = true
            },
            onManageSensitiveLocations = {
                returnToSnsFromSensitive.value = false
                returnToMyProfileFromSensitive.value = true
                showMyProfile.value = false
                showSensitiveLocations.value = true
            },
        )
    } else if (showSensitiveLocations.value) {
        SensitiveLocationFlow(
            viewModel = sensitiveLocations,
            onCumulative = { place ->
                showSensitiveLocations.value = false
                showMyProfile.value = false
                snsCumulative.beginForPlace(place.id)
                showSnsCheck.value = true
            },
            onBack = {
                showSensitiveLocations.value = false
                if (returnToSignupFromSensitive.value) {
                    returnToSignupFromSensitive.value = false
                    accountPage.value = AccountPage.COMPLETION
                } else if (returnToMyProfileFromSensitive.value) {
                    returnToMyProfileFromSensitive.value = false
                    showMyProfile.value = true
                } else if (returnToSnsFromSensitive.value) {
                    returnToSnsFromSensitive.value = false
                    snsCumulative.refreshPlaces()
                    showSnsCheck.value = true
                }
            },
            onMessage = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() },
        )
    } else if (edit.photo != null) {
        val onBack: () -> Unit = {
            if (cumulativeEditPhotos.value.isNotEmpty() && edit.step in setOf(ProtectionStep.LOCATION, ProtectionStep.TARGETS)) {
                finishCumulativeEdit()
            } else if (returnToProtectionHistory.value && edit.step in setOf(ProtectionStep.LOCATION, ProtectionStep.TARGETS)) {
                protection.leave()
                returnToProtectionHistory.value = false
                showProtectionHistory.value = true
            } else if (edit.step == ProtectionStep.LOCATION) protection.leave() else protection.back()
        }
        BackHandler(onBack = onBack)
        ProtectionEditFlow(
            state = edit,
            onBack = onBack,
            onChangePhoto = {
                protection.leave()
                protectionPicker.value = false
                picker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
            },
            onLocation = protection::updateLocation,
            onLocationCategory = protection::selectLocationCategory,
            onContinue = {
                when (edit.step) {
                    ProtectionStep.LOCATION -> protection.continueToTargets()
                    ProtectionStep.TARGETS -> protection.continueToRequest()
                    else -> Unit
                }
            },
            onToggleMode = protection::toggleMode,
            onToggleTarget = protection::toggleTarget,
            onStrength = protection::updateStrength,
            onRequest = protection::updateRequest,
            onSuggestion = protection::useSuggestion,
            onCreate = protection::createPreview,
            photoLabel = if (cumulativeEditPhotos.value.isNotEmpty()) "사진 ${cumulativeEditIndex.value + 1} / ${cumulativeEditPhotos.value.size}" else null,
            nextPhotoLabel = if (cumulativeEditPhotos.value.isEmpty()) null
                else if (cumulativeEditIndex.value < cumulativeEditPhotos.value.lastIndex) "다음 사진 편집" else "누적 결과로 돌아가기",
            onNextPhoto = if (cumulativeEditPhotos.value.isEmpty()) null else ({
                val next = cumulativeEditIndex.value + 1
                if (next > cumulativeEditPhotos.value.lastIndex) finishCumulativeEdit()
                else {
                    cumulativeEditIndex.value = next
                    val source = cumulativeEditPhotos.value[next]
                    protection.begin(source.photo, source.result)
                }
            }),
        )
    } else if (photo != null && analysis == AnalysisState.Idle && needsPhotoLocation) {
        BackHandler {
            selection.cancelPhotoLocation()
        }
        PhotoLocationScreen(
            photo = requireNotNull(photo),
            batchPhotoIndex = photos.indexOf(photo).takeIf { photos.size > 1 }?.plus(1),
            batchPhotoCount = photos.size,
            state = location,
            onBack = {
                selection.cancelPhotoLocation()
            },
            onChangePhoto = {
                protectionPicker.value = false
                multiPicker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
            },
            onLocation = selection::updateLocation,
            onCategory = selection::selectLocationCategory,
            onStart = {
                selection.startAnalysis()
            },
        )
    } else if (analysis == AnalysisState.Idle || photo == null) {
        HomeScreen(
            onSelectPhoto = {
                protectionPicker.value = false
                showPhotoAccessInfo.value = true
            },
            onProtectPhoto = {
                protectionPicker.value = true
                showPhotoAccessInfo.value = true
            },
            onOpenAccount = { showMyProfile.value = true },
            onOpenHistory = { showHistory.value = true },
            onOpenProtectionHistory = {
                protectionHistory.retry()
                showProtectionHistory.value = true
            },
            onOpenSnsCheck = {
                snsCumulative.refreshPlaces()
                showSnsCheck.value = true
            },
        )
    } else {
        BackHandler(onBack = selection::cancelAnalysis)
        if (photos.size > 1) {
            AnalysisFlowScreen(
                uri = requireNotNull(photos.getOrNull(resultPage) ?: photo),
                state = batchStates.getOrNull(resultPage) ?: analysis,
                onBack = selection::cancelAnalysis,
                onHome = { finishInspection() },
                onRetry = selection::retryAnalysis,
                onProtect = {
                    val page = resultPage
                    val result = (batchStates.getOrNull(page) as? AnalysisState.Completed)?.result
                    protection.begin(requireNotNull(photos.getOrNull(page) ?: photo), result)
                },
                batchStates = batchStates,
                batchPhotos = photos,
                batchIndex = resultPage,
                onBatchPage = selection::showResultPage,
            )
        } else AnalysisFlowScreen(
            uri = requireNotNull(photo),
            state = analysis,
            onBack = selection::cancelAnalysis,
            onHome = { finishInspection() },
            onRetry = selection::retryAnalysis,
            onProtect = {
                val result = (analysis as? AnalysisState.Completed)?.result
                protection.begin(requireNotNull(photo), result)
            },
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AppPreview() {
    HomeScreen()
}
