# Akış — Faz 1 (MVP) uygulama planı

Kaynak: `docs/BRIEF.md`. Bu plan Bölüm 13'teki çalışma şeklinin 1. adımıdır. Onaydan önce kod yazılmaz.

Faz 1 kapsamı (Bölüm 12): köprü uygulaması, Şimdi Çalıyor ekranı, döner çerçeveyle ses, Kaynaklar paneli (sadece telefon hedefi), onboarding ve temel tile.

Faz 2+ (Odak Modu, dinamik renk, şekil değiştiren buton, ⋯ menüsü, ambient, komplikasyonlar, saatin yerel medyası) yazılmaz. Mimari bunlara yer bırakacak şekilde kurulur. Her yerin altında "Faz 2 kancası" notu var.

---

## 0. Onay gereken kararlar

Bunlar brief'te açık bırakılmış ya da brief'le küçük bir çelişki doğuran noktalar. Önerdiğim seçenek **kalın**.

| # | Konu | Öneri | Neden |
|---|---|---|---|
| K1 | Paket adı | **`com.erenkng.akis`, iki uygulamada da aynı** | Data Layer, saat ve telefon uygulamalarının **aynı applicationId ve aynı imza anahtarıyla** derlenmesini şart koşar. Debug sürümünde iki APK aynı makinede derlenmeli. İstenirse repoya ortak bir `debug.keystore` konur. |
| K2 | Kaynaklar'daki "Son kullanılanlar" ve uygulama ikonları | **Protokolü genişlet:** `/akis/sessions` DataItem'ına `recent: SessionSummary[]` alanı, uygulama ikonları için `/akis/icon/<paket>` DataItem'ları (48 px PNG Asset) | Bölüm 10'daki tablo bu veriyi taşımıyor. Brief "birebir" diyor, bu yüzden ekleme olarak onay istiyorum. `NowPlayingState` ve `Command`'a dokunulmuyor. |
| K3 | Köprü durumu (`PERMISSION_MISSING`) | **Ayrı DataItem: `/akis/bridge` → `{status: OK / PERMISSION_MISSING}`** | Bölüm 11 bu durumu saate göndermeyi istiyor, ama `NowPlayingState.state` oynatma durumu için ayrılmış. Ayrı bir yol, iki kavramı birbirinden ayırır. |
| K4 | Faz 1 pager düzeni | **İki sayfa: [Şimdi Çalıyor, Kaynaklar], başlangıç sayfası 0** | Odak Modu Faz 2'de listenin başına eklenince başlangıç sayfası 1 olur. Bu, Bölüm 5 ile birebir uyumlu. Sayfalar bir listeden üretildiği için değişiklik tek satır. |
| K5 | Ayarlar ekranı | **Faz 1'de minimal:** köprü durumu, bildirim izni durumu, sürüm ve lisanslar | Kaynaklar listesinde "Ayarlar" satırı Faz 1'de var, boş bir ekrana gitmemeli. Diğer anahtarlar (çift dokunuş, dinamik renk vb.) ilgili fazda eklenir. |
| K6 | Derleme ortamı | **Senin aksiyonun gerekiyor** | Bu bulut ortamında `dl.google.com` ağ politikasıyla engelli. Android SDK, AGP ve AndroidX buradan geliyor, bu yüzden `./gradlew assembleDebug` burada çalışamaz. Ortam ayarlarında (oturum başlığındaki ortam menüsü → Edit → Network access) `dl.google.com` ve `maven.google.com`'u izinli alanlara eklersen her adımı derleyerek doğrularım. Eklemezsen kodu derlenmemiş olarak yazarım ve bunu açıkça belirtirim. |

---

## 1. Kök proje

| Dosya | Sorumluluk |
|---|---|
| `settings.gradle.kts` | `:shared`, `:mobile`, `:wear` modülleri; `google()`, `mavenCentral()`, `gradlePluginPortal()` depoları |
| `build.gradle.kts` | Eklentileri `apply false` ile tanımlar |
| `gradle/libs.versions.toml` | Tüm sürümler (Kotlin 2.x, AGP, Compose BOM, Wear Compose M3, Tiles/ProtoLayout, play-services-wearable, kotlinx.serialization/coroutines). Sürümler, ağ erişimi açılınca Google Maven'da gerçekten çözülen güncel kararlı sürümlerden seçilir. |
| `gradle.properties` | AndroidX, JVM argümanları, `kotlin.code.style=official` |
| `gradle/wrapper/*`, `gradlew`, `gradlew.bat` | Gradle wrapper |
| `.gitignore` | `build/`, `.gradle/`, `local.properties`, `.idea/` |
| `README.md` | Kısa tanıtım ve `docs/` dosyalarına bağlantılar |
| `docs/TESTING.md` | Kurulum (Wi-Fi ADB ile Galaxy Watch dahil) ve Faz 1 kabul kriterlerinin test adımları |
| `docs/OPEN_QUESTIONS.md` | Emin olunamayan API davranışları (Bölüm 12'deki açık sorular + uygulama sırasında çıkanlar) |

## 2. `:shared` — saf Kotlin (JVM), kotlinx.serialization

Paket: `com.erenkng.akis.shared`

| Dosya | Sorumluluk |
|---|---|
| `protocol/AkisPaths.kt` | `/akis/state`, `/akis/sessions`, `/akis/cmd`, `/akis/subscribe`, `/akis/ack` yolları (K2/K3 onaylanırsa `/akis/bridge`, `/akis/icon/`). Capability adı `akis_bridge`. Asset anahtarı `art`. |
| `model/NowPlayingState.kt` | Bölüm 10'daki alanlar birebir. `PlaybackStatus` enum'ı (PLAYING / PAUSED / BUFFERING / STOPPED / ERROR), `RepeatMode`, `CustomActionInfo {id, label, iconPng?}` |
| `model/MediaActions.kt` | `actions` bit maskesi sabitleri (`PlaybackState.ACTION_*` değerlerinin Android'den bağımsız kopyası) + `canPlay`, `canSkipNext`, `canSeek` gibi yardımcılar |
| `model/SessionSummary.kt` | Aktif oturum satırı: `sessionId`, `packageName`, `appLabel`, `title`, `state` |
| `model/Command.kt` | `sealed interface Command`. 17 tür birebir, `@SerialName` ile. Kablo üzerinde `CommandEnvelope(seq: Long, command: Command)`. |
| `model/SubscribeLevel.kt` | `FULL`, `TRACK_ONLY`, `NONE` |
| `model/CommandAck.kt` | `{seq, ok, volume?}` |
| `model/BridgeStatus.kt` | K3 için |
| `codec/AkisJson.kt` | Ortak `Json` yapılandırması (`ignoreUnknownKeys`, `encodeDefaults`, sınıf ayırıcı), `ByteArray` ile kodlama ve çözme |
| `position/PositionCalculator.kt` | `pos = positionMs + (now − positionUpdatedAtEpochMs) × speed`. Yalnızca PLAYING iken ilerler, `[0, durationMs]` aralığına sınırlanır, süresi bilinmeyen yayında sınır yok. Saf fonksiyon. |
| `test/…` | Serileştirme gidiş-dönüş testleri (her `Command` türü için) ve `PositionCalculator` sınır testleri |

İleride protobuf'a geçiş: yalnızca `AkisJson` değişir.

## 3. `:mobile` — Akış Köprü (Android 14+, minSdk 29)

Paket: `com.erenkng.akis` (K1). Elle DI.

| Dosya | Sorumluluk |
|---|---|
| `AndroidManifest.xml` | `AkisNotificationListener` (`BIND_NOTIFICATION_LISTENER_SERVICE`), `BridgeListenerService` (`MESSAGE_RECEIVED` filtresi, yol öneki `/akis`), `SetupActivity` (launcher + `akis://setup` deep link). **INTERNET izni yok.** Birleşmiş manifest kontrol edilir; bir kütüphane INTERNET eklerse `tools:node="remove"` ile kaldırılır. |
| `AkisBridgeApp.kt` | `Application`. `BridgeGraph`'ı oluşturur. |
| `BridgeGraph.kt` | Elle DI kabı: tracker, publisher, executor, volume, abonelik durumu |
| `listener/AkisNotificationListener.kt` | `onListenerConnected` → `SessionTracker.start()`. `onListenerDisconnected` → durdurur ve saate `PERMISSION_MISSING` gönderir. **`onNotificationPosted`/`Removed` boş.** |
| `session/SessionTracker.kt` | `getActiveSessions(componentName)` + `addOnActiveSessionsChangedListener`. Her `MediaController` için `Callback` (metadata, playbackState, queue, audioInfo, sessionDestroyed). Odaklanan oturumu ve oturum listesini `StateFlow` olarak yayar. |
| `session/FocusPolicy.kt` | Saf seçim mantığı: kullanıcının seçtiği oturum → ilk çalan oturum → en son çalmış oturum. Birim testli. |
| `session/StateMapper.kt` | `MediaController` → `NowPlayingState` / `SessionSummary`. `isRemote` = `playbackInfo.playbackType == REMOTE`. `outputLabel` en iyi çabayla doldurulur (çıkış değiştirme yok). |
| `session/RecentAppsStore.kt` | (K2) Oturumu kapanan son 5 medya uygulaması. `SharedPreferences` içinde tutulur. |
| `sync/StatePublisher.kt` | 150 ms debounce. Pozisyon yalnızca durum değişince ya da en fazla 15 sn'de bir gönderilir. `PutDataMapRequest.setUrgent()`. `artHash` aynıysa Asset yeniden eklenmez. Abonelik `NONE` iken yalnızca parça değişimi gider (tile güncellemesi için). |
| `sync/ArtworkEncoder.kt` | Kapak → 256×256 WebP (%80), `artHash`. Uygulama ikonu → 48 px PNG (K2). |
| `command/CommandExecutor.kt` | `Command` → `TransportControls`. Desteklenmeyen eylem `ok=false` döner. `RESUME_LAST` → `AudioManager.dispatchMediaKeyEvent(KEYCODE_MEDIA_PLAY)`. `OPEN_ON_PHONE` → oturumun `sessionActivity`'si. Her komuttan sonra `/akis/ack` gönderir. |
| `command/VolumeController.kt` | Uzak oturumda `controller.setVolumeTo/adjustVolume`, yerelde `AudioManager.setStreamVolume(STREAM_MUSIC, v, 0)`. Gerçek değer ölçülüp ack ile geri döner (Samsung'daki ses zıplamasına karşı). Sabit ses → "kilitli" bayrağı. |
| `wear/BridgeListenerService.kt` | `WearableListenerService`: `/akis/cmd` → `CommandExecutor`, `/akis/subscribe` → `SubscriptionState` |
| `wear/SubscriptionState.kt` | Saatin istediği abonelik seviyesi + son görülme zamanı |
| `ui/SetupActivity.kt`, `ui/SetupScreen.kt` | Tek ekran Compose M3: izin durumu, "Bildirim erişimine izin ver" butonu (`ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS`, olmazsa genel liste), bağlı saat adı, gizlilik cümlesi |
| `res/values/wear.xml` | `android_wear_capabilities` = `akis_bridge` |
| `res/values/strings.xml` | Türkçe metinler |
| `test/…` | `FocusPolicy`, `StateMapper` (sahte değerlerle) |

Faz 2 kancası: `onQueueChanged` verisi `SessionTracker`'da tutulur ama gönderilmez. Uyku zamanlayıcısı için `SLEEP_TIMER` komutu Faz 1'de `ok=false` döner.

## 4. `:wear` — Akış (Wear OS, minSdk 33)

Paket: `com.erenkng.akis` (K1). `com.google.android.wearable.standalone = false`, çünkü uygulama telefon köprüsüne ihtiyaç duyuyor.

### 4.1 Uygulama ve veri katmanı

| Dosya | Sorumluluk |
|---|---|
| `AndroidManifest.xml` | `MainActivity` (`akis://now` deep link), `MediaTileService`, `WearDataListenerService` (`DATA_CHANGED` / `MESSAGE_RECEIVED` filtreleri), `uses-feature watch`. INTERNET yok. |
| `AkisWearApp.kt`, `WearGraph.kt` | Elle DI |
| `data/PhoneRepository.kt` | `DataClient` dinleyicisi → `StateFlow<NowPlayingState?>`, `StateFlow<List<SessionSummary>>`, köprü durumu. Kapak Asset'ini yükler. |
| `data/ConnectionMonitor.kt` | `CapabilityClient` (`akis_bridge`) + `NodeClient`. Durumlar: `Connected` / `Unreachable` / `BridgeMissing`. Kabul kriteri "Bluetooth kapanınca ≤ 5 sn" için capability değişikliği + ack zaman aşımı birlikte kullanılır. |
| `data/CommandSender.kt` | `MessageClient` ile `/akis/cmd`, artan `seq`. 3 sn içinde ack gelmezse 1 kez tekrar. İkinci zaman aşımı → `Unreachable`. |
| `data/LastStateStore.kt` | Son durum + son kapak diske yazılır. Soğuk açılışta önce bu gösterilir (< 700 ms hedefi). |
| `data/ArtworkCache.kt` | Bellek içi LRU (10 kapak, `artHash` anahtarlı) |
| `data/WearDataListenerService.kt` | Uygulama kapalıyken gelen durum değişiminde tile güncellemesi ister (`TileService.getUpdater`) |

### 4.2 Sunum

| Dosya | Sorumluluk |
|---|---|
| `player/PlayerViewModel.kt` | `PlayerUiState` (parça, butonların etkinliği, bağlantı, boş durum, podcast/canlı/reklam varyantları). Eylemler. Yaşam döngüsüne göre `/akis/subscribe`: ekran açık → FULL, arka planda → NONE. Faz 2 kancası: ambient → TRACK_ONLY. |
| `player/PositionTicker.kt` | Pozisyonu **ayrı bir `State`** olarak yayar. Yalnızca ilerleme halkası ve süre yeniden çizilir (Bölüm 11'deki recomposition hedefi). |
| `player/VolumeAccumulator.kt` | 120 ms penceresinde gelen çerçeve tıklarını toplayıp tek `ADJUST_VOLUME(delta)` gönderir. İyimser yerel değer tutar, ack gelince düzeltir. Saf mantık, birim testli. |

### 4.3 Tema ve kaynaklar

| Dosya | Sorumluluk |
|---|---|
| `ui/theme/AkisColors.kt` | Bölüm 4'teki token'lar (`bg`, `surface`, `surfaceHigh`, `onSurface`, `onSurfaceDim`, `accentFallback`, `danger`). `accent` şimdilik sabit Kor turuncusu. Faz 2'de `AccentController` buraya bağlanır (palette + kontrast + yeşil yasağı). |
| `ui/theme/AkisTypography.kt` | Manrope ölçekleri: başlık 16/800, sanatçı 13/500, etiket 10/600 +0.08em, süre 11 `tnum` |
| `ui/theme/AkisTheme.kt` | Wear M3 `MaterialTheme` üzerine token'lar |
| `res/font/manrope_{300,500,600,800}.ttf` + `OFL.txt` | Gömülü font (Google Fonts, OFL) |
| `res/drawable/ic_*.xml` | Material Symbols Rounded (ağırlık 500, dolgu 0/1) SVG'lerinden üretilen vektörler: play, pause, skip_next, skip_previous, volume_up, headphones, smartphone, watch, speaker, more_horiz, music_note, refresh, settings, lock, bluetooth_disabled. Lisans: Apache 2.0. |
| `res/values/strings.xml` | Tüm Türkçe metinler ve `contentDescription`'lar ("Oynat", "Sonraki parça", "Ses: %1$d / %2$d") |

### 4.4 Ekranlar

| Dosya | Sorumluluk |
|---|---|
| `MainActivity.kt` | `AkisNavHost`'u gösterir, deep link'i işler |
| `ui/AkisNavHost.kt` | `SwipeDismissableNavHost`: `main`, `onboarding`, `settings`. Köprü yoksa ya da izin eksikse `onboarding` ile başlar. |
| `ui/main/MainPager.kt` | `VerticalPager` + `VerticalPageIndicator`. Sayfalar bir listeden üretilir (K4). Önemli: Wear `VerticalPager` döner girdiyi varsayılan olarak sayfa değiştirmek için kullanır. Şimdi Çalıyor sayfasında bu kapatılır, çerçeve sese gider. Kaynaklar sayfasında çerçeve listeyi kaydırır. |
| `ui/nowplaying/NowPlayingScreen.kt` | Durum → içerik seçimi (normal / boş / bağlantı yok) |
| `ui/nowplaying/NowPlayingLayout.kt` | Bölüm 6'daki tablo: tüm konumlar ekran çapının yüzdesi olarak (`screenWidthDp`). Kapak arka planı + %35/%55/%85 karartma gradyanı. |
| `ui/nowplaying/ProgressRing.kt` | 4 dp halka, saat 12'den saat yönünde. Canlı yayında "CANLI" etiketi. |
| `ui/nowplaying/TransportButtons.kt` | Önceki/sonraki 44 dp, oynat 64 dp (Faz 1'de sabit daire, Faz 2'de şekil değiştiren buton). 48 dp minimum dokunma alanı. Desteklenmeyen eylem: %30 opaklık + titreşim + toast. Podcast'te −15/+30 sn. Arabellekte dönen halka. |
| `ui/nowplaying/SourceLabel.kt`, `TrackText.kt` | Kaynak etiketi (BÜYÜK HARF). Başlıkta 2 sn bekleyip başlayan marquee, `TextDirection.Content`. |
| `ui/nowplaying/CurvedDuration.kt` | Alt kavisli "1:12 · −2:33" |
| `ui/nowplaying/VolumeOverlay.kt` | Sağ kenarda 60°'lik kavis + ortada geçici "8/15". 150 ms giriş, 1,5 sn bekleme, 300 ms çıkış. Sabit seste kilit ikonu. |
| `ui/nowplaying/RotaryVolume.kt` | `onRotaryScrollEvent` + `FocusRequester` (ekran açılınca odak istenir) → `VolumeAccumulator`. Ses adımında hafif titreşim, sınırda tek belirgin titreşim. |
| `ui/nowplaying/EmptyState.kt`, `DisconnectedState.kt` | "Telefonda şu an bir şey çalmıyor" + "Son çalanı sürdür". "Telefona ulaşılamıyor" + kırmızı halka + gri kapak + "Tekrar dene". |
| `ui/sources/SourcesScreen.kt` | `TransformingLazyColumn` + rotary: Şu an aktif → Son kullanılanlar (K2) → Çıkış (sadece gösterir) → Ayarlar. Hedef seçici Faz 3'e kadar gizli. |
| `ui/onboarding/OnboardingScreen.kt` | 3 sayfa. 2. sayfada `RemoteActivityHelper` ile telefonda Play Store açılır. 3. sayfada `akis://setup` ile telefonda Köprü'nün kurulum ekranı açılır, oradan izin ayarına gidilir. Arka plandaki bir servis Activity başlatamadığı için doğrudan ayar ekranı açılmaz. İzin gelince ana ekrana otomatik geçilir. |
| `ui/settings/SettingsScreen.kt` | K5: minimal |
| `ui/common/Haptics.kt` | Bölüm 8'deki titreşim tablosu. "Titreşim yoğunluğu" ayarı Faz 2'de buraya bağlanır. |

### 4.5 Tile

| Dosya | Sorumluluk |
|---|---|
| `tile/MediaTileService.kt` | `TileService`. `lastClickableId` (`play_pause` / `next` / `prev` / `resume` / `open`) → `CommandSender`. İyimser durumla düzen döndürür. |
| `tile/MediaTileLayout.kt` | ProtoLayout Material 3: kaynak etiketi, 48 dp daire kapak (kaynak sürümü = `artHash`), başlık, sanatçı, 3 buton, "Aç" hap butonu. Boş durum düzeni. |

### 4.6 Testler

`VolumeAccumulator` (pencere birleştirme, iyimser değer + düzeltme), `PlayerViewModel` eşlemeleri (podcast / canlı / desteklenmeyen eylem), `PositionTicker`.

---

## 5. Uygulama sırası (onaydan sonra)

Her adımın sonunda `./gradlew assembleDebug` ve birim testleri çalıştırılır (K6'ya bağlı) ve adım ayrı commit olarak gönderilir.

1. Kök proje + `:shared` (+ testler)
2. `:mobile` köprü
3. `:wear` veri katmanı
4. Tema, font, ikonlar + Şimdi Çalıyor ekranı + döner çerçeveyle ses
5. Kaynaklar paneli
6. Onboarding + minimal Ayarlar
7. Tile
8. `docs/TESTING.md` (Wi-Fi ADB ile Galaxy Watch kurulumu ve Faz 1 kabul kriterlerinin tek tek test adımları) + `docs/OPEN_QUESTIONS.md`

## 6. Şimdiden bilinen riskler (`OPEN_QUESTIONS.md`'ye gidecek)

- **Döner girdi:** Galaxy Watch 9'un dokunmatik çerçeve olaylarının `onRotaryScrollEvent`'e eksiksiz gelip gelmediği (brief'teki açık soru). İlk gün gerçek cihazda test edilmeli.
- **Kısıtlı ayar:** Android 13+ telefonlarda Play Store dışından yüklenen uygulamalarda bildirim erişimi "Kısıtlı ayar" olarak kilitli görünebilir. Bu durumda telefonda Uygulama bilgisi → ⋮ → "Kısıtlı ayarlara izin ver" gerekir. A55 / One UI'da `adb install` ile kurulumda durumun ne olduğu doğrulanacak.
- **Bluetooth kopması:** Bluetooth kopmasının ≤ 5 sn'de algılanması capability bildirimlerinin hızına bağlı. Gerekirse ekran açıkken hafif bir ping eklenir.
- **Çıkış adı:** Telefonun ses çıkışının adını (`outputLabel`) bildirim dinleyicisi yetkisiyle güvenilir şekilde okumak her cihazda mümkün olmayabilir. Okunamazsa genel etiket gösterilir ("Telefon", "Bluetooth").
