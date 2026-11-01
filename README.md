# Akış

Galaxy Watch için, telefonda çalan **herhangi bir** uygulamanın medyasını kontrol eden Wear OS uygulaması.
Müzik ve video uygulamaları, podcast uygulamaları ve tarayıcı — `MediaSession` yayınlayan her şeyle çalışır.

İki parçadan oluşur:

| Modül | Cihaz | Görev |
|---|---|---|
| `:wear` | Saat (Wear OS, minSdk 33) | **Akış**: Şimdi Çalıyor ekranı, döner çerçeveyle ses, Kaynaklar paneli, onboarding, tile |
| `:mobile` | Telefon (Android, minSdk 29) | **Akış Köprü**: telefondaki medya oturumlarını okur, saatten gelen komutları uygular |
| `:shared` | Saf Kotlin | Data Layer protokolü (`NowPlayingState`, `Command`, …) |

İki uygulama Wearable Data Layer API ile konuşur. İnternet izni ve analiz/telemetri yoktur.

## Belgeler

- [`docs/BRIEF.md`](docs/BRIEF.md): tasarım ve teknik brief
- [`docs/PLAN.md`](docs/PLAN.md): Faz 1 uygulama planı ve onaylanan kararlar
- [`docs/TESTING.md`](docs/TESTING.md): kurulum (Galaxy Watch'a Wi-Fi ADB dahil) ve kabul kriteri testleri
- [`docs/OPEN_QUESTIONS.md`](docs/OPEN_QUESTIONS.md): açık sorular ve doğrulanmamış noktalar

## Durum

**Faz 1 (MVP)** kodu tamam. Her push'ta GitHub Actions birim testlerini çalıştırıyor, iki debug APK'yı
derliyor, imzaları ve izinleri kontrol ediyor. Gerçek cihaz testleri `docs/TESTING.md`'de.

**Kurulum (bilgisayarsız):** Actions → son "Build" → Artifacts → `akis-debug-apks` → telefona
`mobile-debug.apk`'yı kur → Akış Köprü → **Saate yükle**. Saat uygulaması telefon APK'sının içinde gelir
ve saate kablosuz kurulur (yalnızca debug sürümü).

```bash
./gradlew test assembleDebug
```

## Lisanslar

- Manrope yazı tipi: SIL Open Font License 1.1 (`wear/src/main/assets/licenses/Manrope-OFL.txt`)
- Material Symbols ikonları: Apache License 2.0
- libadb-android (yalnızca debug, saate kurulum): Apache License 2.0 (çift lisanslı, Apache seçildi)
