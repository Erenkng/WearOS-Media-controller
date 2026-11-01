package com.erenkng.akis.wear.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.VerticalPageIndicator
import com.erenkng.akis.wear.R
import com.erenkng.akis.wear.player.PlayerViewModel
import com.erenkng.akis.wear.ui.common.toast
import com.erenkng.akis.wear.ui.nowplaying.NowPlayingActions
import com.erenkng.akis.wear.ui.nowplaying.NowPlayingScreen
import com.erenkng.akis.wear.ui.sources.SourcesScreen
import kotlinx.coroutines.launch

/** Ana dikey eksen (BRIEF Bölüm 5): aşağı = daha sade, yukarı = daha fazla seçenek. */
enum class MainPage {
    // Faz 2 kancası: FOCUS (Odak Modu) listenin başına eklenir; başlangıç sayfası kendiliğinden 1 olur.
    NOW_PLAYING,
    SOURCES,
}

private val Pages = MainPage.entries

/**
 * Not: Compose foundation'ın `VerticalPager`'ı kullanılır; döner girdiyi kendisi tüketmez.
 * Böylece Şimdi Çalıyor'da çerçeve sese, Kaynaklar'da listeye gider (PLAN 4.4).
 *
 * @param showNowPlayingSignal her değiştiğinde (tile/komplikasyon deep link'i) Şimdi Çalıyor'a döner.
 */
@Composable
fun MainPager(viewModel: PlayerViewModel, showNowPlayingSignal: Int, onOpenSettings: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val volume by viewModel.volume.display.collectAsStateWithLifecycle()
    val nowPlayingIndex = Pages.indexOf(MainPage.NOW_PLAYING)
    val pagerState = rememberPagerState(initialPage = nowPlayingIndex) { Pages.size }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(showNowPlayingSignal) {
        if (showNowPlayingSignal > 0) pagerState.scrollToPage(nowPlayingIndex)
    }

    val actions = NowPlayingActions(
        onPlayPause = viewModel::onPlayPause,
        onNext = viewModel::onNext,
        onPrevious = viewModel::onPrevious,
        onVolumeSteps = viewModel::onVolumeSteps,
        onResumeLast = { viewModel.onResumeLast() },
        onRetry = viewModel::onRetry,
        onUnsupported = { context.toast(R.string.toast_unsupported) },
        onMore = { context.toast(R.string.toast_more_coming) },
    )

    Box(Modifier.fillMaxSize()) {
        VerticalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { index ->
            when (Pages[index]) {
                MainPage.NOW_PLAYING -> NowPlayingScreen(
                    state = state,
                    volume = volume,
                    isActivePage = pagerState.currentPage == index,
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
