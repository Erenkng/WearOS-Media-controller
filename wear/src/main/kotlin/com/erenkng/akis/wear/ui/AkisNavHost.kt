package com.erenkng.akis.wear.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.erenkng.akis.wear.R
import com.erenkng.akis.wear.WearGraph
import com.erenkng.akis.wear.player.PlayerEvent
import com.erenkng.akis.wear.player.PlayerScreen
import com.erenkng.akis.wear.player.PlayerViewModel
import com.erenkng.akis.wear.ui.common.rememberHaptics
import com.erenkng.akis.wear.ui.common.toast
import com.erenkng.akis.wear.ui.main.MainPage
import com.erenkng.akis.wear.ui.main.MainPager
import com.erenkng.akis.wear.ui.onboarding.OnboardingScreen
import com.erenkng.akis.wear.ui.settings.SettingsScreen
import com.erenkng.akis.wear.ui.theme.AkisTheme
import com.erenkng.akis.wear.ui.theme.rememberArtworkAccent

private object Routes {
    const val MAIN = "main"
    const val SETTINGS = "settings"
    // Faz 2: ⋯ menüsü, sıra, ses ve çıkış ekranları buraya eklenir (BRIEF Bölüm 5).
}

/**
 * Yan ekranlar `SwipeDismissableNavHost` içinde; sağa kaydırma sistemin geri jestidir (BRIEF Bölüm 5).
 * Onboarding ayrı bir rota değil: köprü/izin durumu değişince ana rota kendiliğinden ona ya da
 * oynatıcıya döner, böylece "izin gelince saat otomatik ana ekrana geçer".
 *
 * Vurgu rengi çalan şarkının kapağından gelir ve bütün ekranlara [AkisTheme] ile dağıtılır.
 * Sistem `TimeText`'i yalnızca liste/ayar ekranlarında görünür; oynatıcı ekranları kendi düz saatini çizer.
 */
@Composable
fun AkisApp(graph: WearGraph, showNowPlayingSignal: Int) {
    val viewModel: PlayerViewModel = viewModel { PlayerViewModel(graph) }
    val navController = rememberSwipeDismissableNavController()
    val context = LocalContext.current
    val haptics = rememberHaptics()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val accent = rememberArtworkAccent(uiState.artwork, uiState.track?.artHash)

    var mainPage by remember { mutableStateOf(MainPage.NOW_PLAYING) }
    var settingsOpen by remember { mutableStateOf(false) }
    val showSystemTime = settingsOpen || uiState.screen is PlayerScreen.Onboarding || mainPage.showsSystemTime

    // Ekran açık → FULL abonelik + bağlantı yoklaması; kapalı → NONE (BRIEF Bölüm 11, pil).
    LifecycleStartEffect(viewModel) {
        viewModel.onVisible(true)
        onStopOrDispose { viewModel.onVisible(false) }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            haptics.error()
            when (event) {
                PlayerEvent.Unsupported -> context.toast(R.string.toast_unsupported)
                PlayerEvent.Unreachable -> context.toast(R.string.toast_unreachable)
            }
        }
    }

    LaunchedEffect(showNowPlayingSignal) {
        if (showNowPlayingSignal > 0) navController.popBackStack(Routes.MAIN, inclusive = false)
    }

    AkisTheme(accent = accent) {
        AppScaffold(timeText = { if (showSystemTime) TimeText() }) {
            SwipeDismissableNavHost(navController = navController, startDestination = Routes.MAIN) {
                composable(Routes.MAIN) {
                    when (val screen = uiState.screen) {
                        is PlayerScreen.Onboarding -> OnboardingScreen(
                            firstStep = screen.firstStep,
                            onWelcomeDone = viewModel::onWelcomeDone,
                        )

                        else -> MainPager(
                            viewModel = viewModel,
                            showNowPlayingSignal = showNowPlayingSignal,
                            onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                            onPageChanged = { mainPage = it },
                        )
                    }
                }
                composable(Routes.SETTINGS) {
                    DisposableEffect(Unit) {
                        settingsOpen = true
                        onDispose { settingsOpen = false }
                    }
                    SettingsScreen(uiState)
                }
            }
        }
    }
}
