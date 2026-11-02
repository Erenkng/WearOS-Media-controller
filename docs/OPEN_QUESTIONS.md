# Açık sorular

Uygulama sırasında emin olunamayan API davranışları ve brief'te açık bırakılan noktalar. Tahmin
yürütülmedi. Her madde için davranışın nasıl doğrulanacağı ve sonucuna göre ne değişeceği yazıldı.

## S1. Derleme ve sürümler ✅ (çözüldü)

Geliştirme ortamı Google Maven'a erişemediği için derleme GitHub Actions'a taşındı
(`.github/workflows/build.yml`). İlk çalıştırmada 4 derleme hatası çıktı ve düzeltildi:
`onAudioInfoChanged` imzası, `basicCurvedText` ve Wear `PagerState`. Şu an her push'ta birim testleri,
iki debug APK'nın derlenmesi, imza kontrolü ve izin kontrolü geçiyor. `libs.versions.toml`'daki
sürümlerin hepsi çözüldü.

Henüz yapılmayan: gerçek cihazda çalıştırma (bkz. `docs/TESTING.md`).

## S2. Wear OS 6'da saatteki uygulamaya bildirim erişimi verilebiliyor mu? (brief)

Faz 3'ü (saatin kendi medyası) belirler. Faz 1'i etkilemez. Faz 3 başında Galaxy Watch 9'da denenecek.
Ayar ekranı yoksa yalnızca `adb shell cmd notification allow_listener …` ile çalışır.

## S3. Galaxy Watch 9 çerçeve olayları (brief)

- Dokunmatik ve fiziksel çerçeve olayları `onRotaryScrollEvent`'e eksiksiz geliyor mu?
- **Adım kalibrasyonu:** `RotaryVolume.kt` bir tık için `ViewConfiguration.scaledVerticalScrollFactor × 0.9`
  piksel kabul ediyor. Galaxy çerçevesi tık başına tam bir "scroll factor" üretiyorsa 1 tık = 1 adım olur.
  K3 testi 15 tıkta 15 adım vermezse bu katsayı değişir.
- **Yön:** Saat yönü = pozitif piksel = ses artışı varsayıldı. Tersse `RotaryStepper` çağrısında işaret çevrilir.

İlk gün gerçek cihazda, `adb logcat` ile ham `verticalScrollPixels` değerleri loglanarak doğrulanmalı.

## S4. Telefonun ses çıkışı değiştirilebilir mi? Uzak cihazın adı okunabilir mi? (brief)

MVP çıkışı yalnızca **gösterir**. `outputLabel` yerel çalmada `AudioManager.getDevices` ile üretiliyor
(Bluetooth cihaz adı, kablolu kulaklık ya da telefon hoparlörü). Uzak oturumlarda (Chromecast vb.)
platform `MediaController` cihaz adını vermiyor. Bu yüzden "Uzak cihaz" yazıyor. Brief'teki
"Uzak cihaz: Salon TV" için MediaRouter2 araştırılmalı (Faz 2).

Saat, ikon seçimi için (kulaklık/telefon/uzak) bu etiketin metnine bakıyor. Protokolde ayrı bir alan
yok. Faz 2'de `NowPlayingState`'e bir `outputType` alanı eklemek daha sağlam olur, ama bu brief'teki
alan listesini değiştirdiği için onay gerektirir.

## S5. Play Store dışından kurulan köprüde bildirim erişimi kilitli olabilir

Android 13+ "kısıtlı ayar" mekanizması, yan yüklenen uygulamalarda bildirim erişimi anahtarını
kilitleyebilir. Kurulum ekranı bunun için "Uygulama bilgisini aç" yolunu gösteriyor. A55'te
`adb install` ile kurulumda kilidin çıkıp çıkmadığı K7 sırasında not edilmeli. Play Store'dan
kurulumda sorun beklenmiyor.

## S6. Karıştır / tekrarla (Faz 2)

Platformun `android.media.session.MediaController` sınıfı shuffle/repeat okuma ve yazma sunmuyor.
Bunlar yalnızca `MediaControllerCompat`/Media3 tarafında var. Şu an `shuffle=false`,
`repeat=NONE` gönderiliyor ve `SET_SHUFFLE`/`SET_REPEAT` komutları `ok=false` dönüyor.

Faz 2 seçenekleri: (a) uygulamaların bunları özel eylem olarak sunduğu durumlarda ⋯ menüsünde özel
eylem olarak göstermek, (b) Media3 `MediaController` ile oturum token'ına bağlanmak (ek bağımlılık).

## S7. "Telefonda aç" (OPEN_ON_PHONE, Faz 2 menüsü)

Android 14+ arka plandan Activity başlatmayı kısıtlıyor. Köprü `sessionActivity` PendingIntent'ini
`MODE_BACKGROUND_ACTIVITY_START_ALLOWED` ile gönderiyor. Bunun bildirim dinleyicisi olan bir uygulama
için yeterli olup olmadığı doğrulanmadı. Faz 2'de ⋯ menüsü yazılırken test edilecek.

## S8. "Son kullanılanlar"dan çalmayı başlatma

Oturumu kapanmış bir uygulamaya köprü doğrudan `MEDIA_BUTTON` (oynat) yayını gönderiyor. Media3
tabanlı uygulamalar bu yayınla arka planda ön plan servisi başlatamayabilir (Android 12+ kısıtı).
Olmazsa yedek olarak `AudioManager.dispatchMediaKeyEvent` kullanılıyor. Ancak bu tuş seçilen
uygulamaya değil, sistemin son medya uygulamasına gider. Üç uygulamada ayrı ayrı denenmeli.

## S9. Tile `lastClickableId` davranışı

Sistemin bir tıklamadan sonraki (tıklama kaynaklı olmayan) tile isteklerinde `lastClickableId`'yi
temizleyip temizlemediği belgelerde net değil. Temizlemiyorsa her güncellemede komut tekrar gönderilirdi.
Bu yüzden tıklama kimlikleri her düzende benzersiz bir nesil numarası taşıyor ve son işlenen kimlik
diskte tutuluyor (`WearPrefs.lastHandledTileClick`). K6'nın 5. adımı bunu test ediyor.

## S10. Bluetooth kopmasının algılanma süresi

Saat, ekran açıkken her 2 sn'de capability durumunu yerel olarak sorguluyor ve komut zaman aşımında
hemen yeniden ölçüyor. Wear OS'un bir düğümü "erişilemez" saymasının ne kadar sürdüğü cihaza bağlı.
5 sn hedefi tutmazsa ekran açıkken hafif bir ping mesajı eklenebilir (pil maliyeti ölçülerek).
Saat Wi-Fi üzerinden de bağlı kalabildiği için test sırasında saatin Wi-Fi'si kapatılmalı.

## S11. Tile renkleri

Tile, ProtoLayout Material 3'ün varsayılan (sistem dinamik) renk şemasını kullanıyor. Kor turuncusu
vurgu tile'a uygulanmadı. ProtoLayout M3 `ColorScheme` API'si derlenerek doğrulanınca Faz 4 cilasında
eklenebilir.

## S12. Uygulama adı "Akış" kesin mi? (brief)

Paket adı `com.erenkng.akis`. Görünen ad (`app_name`) değişirse paket adının değişmesi gerekmez.
Paket adı değişirse iki modülün `applicationId`'si birlikte değişmeli.

## S13. Telefondan saate kablosuz kurulum (yalnızca debug)

"Saate yükle" ekranı libadb-android (JitPack, Apache-2.0 lisansıyla kullanılıyor) üzerinden saatin
kablosuz hata ayıklamasına bağlanıp `cmd package install` ile APK'yı akıtır. Gerçek cihazda doğrulanacaklar:

- **Wi-Fi:** Galaxy Watch, telefona Bluetooth ile bağlıyken Wi-Fi'yi kapatabilir. Kurulum sırasında saatin
  Wi-Fi'ye bağlı kalması gerekir.
- **mDNS keşfi:** Telefonun `_adb-tls-pairing._tcp` / `_adb-tls-connect._tcp` servislerini bulması bazı
  ağlarda (misafir ağı, istemci yalıtımı) engellenebilir. Bu durumda ekrandaki "elle IP:port" alanları kullanılır.
- **Akış kurulumu:** `exec:cmd package install -r -S <boyut>` Wear OS 6'da çalışmalı (Android 7+ özelliği).
  Çalışmazsa yedek yol: önce `sync:` ile `/data/local/tmp`'ye yükleyip `pm install`.
- **Güvenlik:** Telefon uygulaması saatte ADB yetkisi olan bir anahtar tutar (uygulamanın özel klasöründe).
  Kurulumdan sonra saatte Kablosuz hata ayıklamanın kapatılması önerilir. Bu özellik ve INTERNET izni
  yalnızca debug sürümünde var; release sürümü brief'teki gibi çevrimdışı (CI bunu her derlemede kontrol ediyor).
- **Play Store:** Uygulama Play'de yayınlandığında saat uygulaması oradan otomatik kurulur ve bu ekran gerekmez.
