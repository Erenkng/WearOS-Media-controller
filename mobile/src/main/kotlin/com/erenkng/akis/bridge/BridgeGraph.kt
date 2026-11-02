package com.erenkng.akis.bridge

import android.content.ComponentName
import android.content.Context
import android.service.notification.NotificationListenerService
import com.erenkng.akis.bridge.command.AckCache
import com.erenkng.akis.bridge.command.CommandExecutor
import com.erenkng.akis.bridge.command.CommandHandler
import com.erenkng.akis.bridge.command.VolumeController
import com.erenkng.akis.bridge.listener.AkisNotificationListener
import com.erenkng.akis.bridge.listener.NotificationAccess
import com.erenkng.akis.bridge.session.AppInfoResolver
import com.erenkng.akis.bridge.session.RecentAppsStore
import com.erenkng.akis.bridge.session.SessionTracker
import com.erenkng.akis.bridge.session.StateMapper
import com.erenkng.akis.bridge.sync.ArtworkEncoder
import com.erenkng.akis.bridge.sync.StatePublisher
import com.erenkng.akis.bridge.wear.SubscriptionState
import com.erenkng.akis.shared.model.BridgeStatusCode
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Elle DI kabı (BRIEF Bölüm 10: "DI: elle"). Uygulama süreci boyunca tek örnek.
 *
 * Süreç, sistem bildirim dinleyicisine bağlı kaldığı sürece yaşar; bu yüzden ayrı bir
 * ön plan servisi gerekmez.
 */
class BridgeGraph(context: Context) {

    val appContext: Context = context.applicationContext
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val listenerComponent = ComponentName(appContext, AkisNotificationListener::class.java)

    private val dataClient = Wearable.getDataClient(appContext)
    private val messageClient = Wearable.getMessageClient(appContext)
    val nodeClient = Wearable.getNodeClient(appContext)

    val appInfo = AppInfoResolver(appContext)
    val recentApps = RecentAppsStore(appContext)
    val tracker = SessionTracker(appContext, listenerComponent, appInfo, recentApps)
    val volume = VolumeController(appContext)
    val artwork = ArtworkEncoder(appContext)
    val mapper = StateMapper(appInfo, artwork, volume)
    val subscription = SubscriptionState()

    val publisher = StatePublisher(
        scope = scope,
        dataClient = dataClient,
        tracker = tracker,
        mapper = mapper,
        recentApps = recentApps,
        appInfo = appInfo,
        artwork = artwork,
        subscription = subscription,
        bridgeVersionCode = BuildConfig.VERSION_CODE,
    )

    private val executor = CommandExecutor(appContext, tracker, volume)
    val commandHandler = CommandHandler(scope, executor, messageClient, AckCache())

    init {
        publisher.start()
    }

    /**
     * İzin varsa oturum takibini başlatır ve köprü durumunu yayınlar. Süreç her başladığında,
     * dinleyici bağlandığında ve kurulum ekranı öne geldiğinde çağrılır; tekrar çağrılması zararsızdır.
     */
    fun ensureTracking() {
        val status = if (NotificationAccess.isGranted(appContext) && (tracker.isRunning || tracker.start())) {
            BridgeStatusCode.OK
        } else {
            tracker.stop()
            BridgeStatusCode.PERMISSION_MISSING
        }
        if (status != lastBridgeStatus) {
            lastBridgeStatus = status
            publisher.publishBridgeStatus(status)
        }
    }

    private var lastBridgeStatus: BridgeStatusCode? = null

    fun onListenerConnected() = ensureTracking()

    /** Sistem dinleyiciyi izin geri alındığı için mi, başka bir sebeple mi ayırdı? */
    fun onListenerDisconnected() {
        if (NotificationAccess.isGranted(appContext)) {
            // Bellek baskısı gibi geçici bir ayrılma: yeniden bağlanmayı iste, takip sürsün.
            runCatching { NotificationListenerService.requestRebind(listenerComponent) }
        } else {
            ensureTracking()
        }
    }
}
