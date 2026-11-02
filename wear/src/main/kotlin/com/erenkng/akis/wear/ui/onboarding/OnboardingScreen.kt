package com.erenkng.akis.wear.ui.onboarding

import android.content.Intent
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.pager.VerticalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.VerticalPageIndicator
import androidx.wear.remote.interactions.RemoteActivityHelper
import com.erenkng.akis.wear.R
import com.erenkng.akis.wear.player.OnboardingStep
import com.erenkng.akis.wear.ui.common.awaitResult
import com.erenkng.akis.wear.ui.common.toast
import com.erenkng.akis.wear.ui.theme.AkisColors
import com.erenkng.akis.wear.ui.theme.AkisType
import com.erenkng.akis.wear.ui.theme.LocalAccent
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

/**
 * İlk kurulum, 3 ekran, dikey kaydırmalı (BRIEF Bölüm 7.7). Yatay kaydırma sistemin geri jestidir.
 * Wear `VerticalPager`'ın varsayılan döner davranışı korunur: çerçeve burada sayfa çevirir.
 *
 * Saat, eksik adımdan başlar. İzin telefonda verilince köprü `/akis/bridge` ile bildirir ve
 * saat bu ekrandan kendiliğinden ana ekrana geçer (bu ekranı gösteren durum ortadan kalkar).
 */
@Composable
fun OnboardingScreen(firstStep: OnboardingStep, onWelcomeDone: () -> Unit) {
    val steps = OnboardingStep.entries
    val pagerState = rememberPagerState(initialPage = steps.indexOf(firstStep)) { steps.size }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val remoteHelper = remember(context) { RemoteActivityHelper(context, Executors.newSingleThreadExecutor()) }

    fun openOnPhone(uri: String) {
        scope.launch {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).addCategory(Intent.CATEGORY_BROWSABLE)
                remoteHelper.startRemoteActivity(intent, null).awaitResult()
                context.toast(R.string.onboarding_opened_on_phone)
            } catch (_: Exception) {
                context.toast(R.string.onboarding_open_failed)
            }
        }
    }

    Box(Modifier.fillMaxSize().background(AkisColors.Bg)) {
        VerticalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            when (steps[page]) {
                OnboardingStep.WELCOME -> StepPage(
                    icon = R.drawable.ic_watch,
                    title = stringResource(R.string.onboarding_welcome_title),
                    body = stringResource(R.string.onboarding_welcome_body),
                    button = stringResource(R.string.onboarding_welcome_next),
                    onClick = onWelcomeDone,
                )

                OnboardingStep.INSTALL_BRIDGE -> StepPage(
                    icon = R.drawable.ic_install_mobile,
                    title = stringResource(R.string.onboarding_install_title),
                    body = stringResource(R.string.onboarding_install_body),
                    button = stringResource(R.string.onboarding_install_button),
                    onClick = { openOnPhone("market://details?id=${context.packageName}") },
                )

                OnboardingStep.GRANT_PERMISSION -> StepPage(
                    icon = R.drawable.ic_notifications,
                    title = stringResource(R.string.onboarding_permission_title),
                    body = stringResource(R.string.onboarding_permission_body),
                    button = stringResource(R.string.onboarding_permission_button),
                    // Telefondaki köprünün kurulum ekranı; oradan tek dokunuşla izin ayarına gidilir.
                    onClick = { openOnPhone("akis://setup") },
                )
            }
        }
        VerticalPageIndicator(pagerState = pagerState)
    }
}

@Composable
private fun StepPage(
    @DrawableRes icon: Int,
    title: String,
    body: String,
    button: String,
    onClick: () -> Unit,
) {
    val accent = LocalAccent.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(painter = painterResource(icon), contentDescription = null, tint = accent.accent, modifier = Modifier.size(28.dp))
        Text(text = title, style = AkisType.screenTitle, textAlign = TextAlign.Center)
        Text(text = body, style = AkisType.bodyDim, textAlign = TextAlign.Center)
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(containerColor = accent.accent, contentColor = accent.onAccent),
            label = { Text(text = button, style = AkisType.button.copy(color = accent.onAccent)) },
        )
    }
}
