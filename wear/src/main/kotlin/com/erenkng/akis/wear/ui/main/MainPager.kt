package com.erenkng.akis.wear.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.pager.VerticalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.VerticalPageIndicator
import com.erenkng.akis.wear.R
import com.erenkng.akis.wear.player.PlayerViewModel
import com.erenkng.akis.wear.ui.common.toast
import com.erenkng.akis.wear.ui.focus.FocusScreen
import com.erenkng.akis.wear.ui.nowplaying.NowPlayingActions
import com.erenkng.akis.wear.ui.nowplaying.NowPlayingScreen
import com.erenkng.akis.wear.ui.sources.SourcesScreen
import kotlinx.coroutines.launch

/** Ana dikey eksen (BRIEF Bölüm 5): aşağı = daha sade (Odak), yukarı = daha fazla seçenek (Kaynaklar). */
enum class MainPage {
    FOCUS,
    NOW_PLAYING,
    SOURCES,
    ;

    /** Oynatıcı sayfaları kendi düz saatini çizer; sistem `TimeText`'i yalnızca listelerde görünür. */
    val showsSystemTime: Boolean get() = this == SOURCES
}

private val Pages = MainPage.entries

/**
 * Wear `VerticalPager`, `rotaryScrollableBehavior = null` ile: döner çerçeve sayfa değiştirmez.
 * Pager her sayfayı `hierarchicalFocusGroup(aktif sayfa)` ile sarar; böylece çerçeve Odak ve Şimdi
 * Çalıyor'da sese, Kaynaklar'da listeye gider (PLAN 4.4).
 *
 * @param showNowPlayingSignal her değiştiğinde (tile/komplikasyon deep link'i) Şimdi Çalıyor'a döner.
 * @param onPageChanged sistem saatinin gösterilip gösterilmeyeceği için [com.erenkng.akis.wear.ui.AkisApp]'e bildirir.
 */
@Composable
fun MainPager(
    viewModel: PlayerViewModel,
    showNowPlayingSignal: Int,
    onOpenSettings: () -> Unit,
    onPageChanged: (MainPage) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val volume by viewModel.volume.display.collectAsStateWithLifecycle()
    val nowPlayingIndex = Pages.indexOf(MainPage.NOW_PLAYING)
    val sourcesIndex = Pages.indexOf(MainPage.SOURCES)
    val pagerState = rememberPagerState(initialPage = nowPlayingIndex) { Pages.size }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val currentOnPageChanged by rememberUpdatedState(onPageChanged)

    LaunchedEffect(showNowPlayingSignal) {
        if (showNowPlayingSignal > 0) pagerState.scrollToPage(nowPlayingIndex)
    }
    LaunchedEffect(pagerState.currentPage) {
        currentOnPageChanged(Pages[pagerState.currentPage])
    }

    val actions = NowPlayingActions(
        onPlayPause = viewModel::onPlayPause,
        onNext = viewModel::onNext,
        onPrevious = viewModel::onPrevious,
        onVolumeSteps = viewModel::onVolumeSteps,
        onResumeLast = { viewModel.onResumeLast() },
        onRetry = viewModel::onRetry,
        onUnsupported = { context.toast(R.string.toast_unsupported) },
        onLike = viewModel::onLike,
        onOpenSources = { scope.launch { pagerState.animateScrollToPage(sourcesIndex) } },
    )

    Box(Modifier.fillMaxSize()) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            rotaryScrollableBehavior = null,
        ) { index ->
            val isActive = pagerState.currentPage == index
            when (Pages[index]) {
                MainPage.FOCUS -> FocusScreen(
                    state = state,
                    volume = volume,
                    isActivePage = isActive,
                    // Faz 2 kancası: "Çift dokunuşu kapat" ayarı (BRIEF 7.6).
                    doubleTapEnabled = true,
                    actions = actions,
                )

                MainPage.NOW_PLAYING -> NowPlayingScreen(
                    state = state,
                    volume = volume,
                    isActivePage = isActive,
                    actions = actions,
                )

                MainPage.SOURCES -> SourcesScreen(
                    state = state,
                    onSessionSelected = { sessionId ->
                        viewModel.onFocusSession(sessionId)
                        scope.launch { pagerState.animateScrollToPage(nowPlayingIndex) }
                    },
                    onRecentSelected = { packageName ->
                        viewModel.onResumeLast(packageName)
                        scope.launch { pagerState.animateScrollToPage(nowPlayingIndex) }
                    },
                    onOutputClick = { scope.launch { pagerState.animateScrollToPage(nowPlayingIndex) } },
                    onOpenSettings = onOpenSettings,
                )
            }
        }
        VerticalPageIndicator(pagerState = pagerState)
    }
}
