# Akış — Kurulum ve test

Faz 1 (MVP) için kurulum adımları ve kabul kriterlerinin (BRIEF Bölüm 12) tek tek nasıl test edileceği.

> **Önemli:** Bu kod Google Maven'a erişimi olmayan bir ortamda yazıldı. `./gradlew assembleDebug` henüz
> hiç çalıştırılmadı. Saf Kotlin mantığı (protokol, odak seçimi, gönderim politikası, ses birleştirme,
> döner çerçeve adımları) 44 birim testle doğrulandı. Android katmanı derlenmedi. İlk derlemede
> çıkabilecek sorunlar için `docs/OPEN_QUESTIONS.md` içindeki S1'e bakın.

## 1. Gereksinimler

- Android Studio (güncel kararlı sürüm), JDK 17 ya da üstü
- Android SDK Platform 36
- Telefon: Galaxy A55 5G (Android 14+), USB hata ayıklama açık
- Saat: Galaxy Watch 9 (Wear OS 6), telefonla Galaxy Wearable üzerinden eşleşmiş
- Bilgisayar, telefon ve saat aynı Wi-Fi ağında (saate kablosuz ADB için)

## 2. Derleme

```bash
./gradlew test             # :shared, :mobile, :wear birim testleri
./gradlew assembleDebug    # iki APK
```

Çıktılar:

- `mobile/build/outputs/apk/debug/mobile-debug.apk`: telefon, **Akış Köprü**
- `wear/build/outputs/apk/debug/wear-debug.apk`: saat, **Akış**

**İki APK'yı aynı bilgisayarda derleyin.** Data Layer, saat ve telefon uygulamalarının aynı
`applicationId` (`com.erenkng.akis`) ve **aynı imza anahtarına** sahip olmasını şart koşar. Debug
anahtarı bilgisayara özeldir (`~/.android/debug.keystore`). Farklı bilgisayarda derlenen iki APK
birbirini görmez.

## 3. Telefona kurulum (Akış Köprü)

```bash
adb devices                                     # telefon listede olmalı
adb install -r mobile/build/outputs/apk/debug/mobile-debug.apk
```

1. Telefonda **Akış Köprü**'yü aç.
2. **Bildirim erişimine izin ver** → listede Akış Köprü'yü aç.
3. Anahtar gri ve dokunulamaz durumdaysa Android bu izni "kısıtlı ayar" olarak kilitlemiş demektir.
   Kurulum ekranındaki **Uygulama bilgisini aç** butonuna bas, sağ üstteki ⋮ menüsünden
   **Kısıtlı ayarlara izin ver**'i seç, sonra 2. adımı tekrarla.
4. Kurulum ekranına dönünce "İzin verildi" yazmalı ve "Saat" kartında saatin adı görünmeli.

Geliştirme sırasında izni ADB ile vermek ya da geri almak için:

```bash
# izin ver
adb shell cmd notification allow_listener com.erenkng.akis/com.erenkng.akis.bridge.listener.AkisNotificationListener
# geri al (onboarding testi için)
adb shell cmd notification disallow_listener com.erenkng.akis/com.erenkng.akis.bridge.listener.AkisNotificationListener
```

## 4. Saate kurulum (Wi-Fi üzerinden ADB)

One UI Watch'ta menü adları sürüme göre biraz farklı olabilir.

1. Saatte **Ayarlar → Saat hakkında → Yazılım bilgisi → Yazılım sürümü** satırına 5 kez dokun.
   "Geliştirici modu açıldı" mesajı gelir.
2. **Ayarlar → Geliştirici seçenekleri**:
   - **ADB hata ayıklama**: Açık
   - **Kablosuz hata ayıklama**: Açık (saat Wi-Fi'ye bağlı olmalı)
3. **Kablosuz hata ayıklama → Yeni cihaz eşleştir**. Ekranda bir IP:port ve 6 haneli kod görünür.
4. Bilgisayarda:

   ```bash
   adb pair 192.168.1.23:37415        # eşleştirme ekranındaki IP:port, sonra kodu gir
   adb connect 192.168.1.23:41235     # Kablosuz hata ayıklama ana ekranındaki IP:port (eşleştirme portundan farklı)
   adb devices                         # saat "192.168.1.23:41235  device" olarak görünmeli
   adb -s 192.168.1.23:41235 install -r wear/build/outputs/apk/debug/wear-debug.apk
   ```

5. Saatte uygulama listesinden **Akış**'ı aç.

İpucu: Testler sırasında saat ekranı kapanmasın diye **Ayarlar → Ekran → Ekran zaman aşımı** süresini
uzat. Akış ekranı açık tutmak için WAKE_LOCK kullanmaz.

### Tile'ı ekleme

Saat kadranında sola kaydırarak tile'lara gel → en sona kaydır → **+ Tile ekle** → **Akış**.

### Logları izleme

```bash
# Telefon
adb -s <telefon> logcat -v time -s SessionTracker StatePublisher CommandExecutor CommandHandler
# Saat
adb -s <saat-ip:port> logcat -v time -s PhoneRepository ConnectionMonitor CommandSender
```

Telefondaki `durum →` satırı durumun gönderildiği anı, saatteki `durum ←` satırı ulaştığı anı gösterir.

## 5. Faz 1 kabul kriterleri: nasıl test edilir

Her kriter için Spotify, YouTube Music ve YouTube ayrı ayrı denenir (aşağıda "üç uygulama").

### K1. Başlık, sanatçı ve kapak 1 sn içinde görünüyor

1. Telefonda üç uygulamadan birinde bir parça çal. Saatte Akış açık olsun.
2. Telefonda sonraki parçaya geç.
3. İki cihazın logunu `-v time` ile yan yana izle. Telefondaki `durum →` ile saatteki `durum ←` arasındaki
   fark < 1 sn olmalı. Kapak da aynı satırda gelir (`art=true`).
4. Gözle kontrol: saat ekranında başlık, sanatçı ve kapak, telefonun ekranıyla aynı anda değişmeli.
5. Üç uygulamada tekrarla. Kapağı olmayan bir içerikte (örneğin tarayıcı) uygulama ikonunun bulanık
   arka plan olarak geldiğini doğrula.

Not: Telefon iki taraftaki saatlerin farkını değil, olayların sırasını gösterir. İki cihazın saati
otomatik ağ saatinde olmalı.

### K2. Oynat/duraklat, önceki, sonraki üç uygulamada çalışıyor

Her uygulamada:

1. Oynat/duraklat'a dokun. Telefonda çalma durmalı ve tekrar başlamalı. Buton ikonu dokunur dokunmaz
   değişmeli (iyimser güncelleme).
2. Sonraki ve önceki butonları parçayı değiştirmeli.
3. Uygulamanın izin vermediği bir eylemde (örneğin reklam sırasında atlama) buton %30 opak görünmeli.
   Dokununca çift titreşim ve "Bu uygulama desteklemiyor" mesajı gelmeli.
4. Bir podcast uygulamasında (atlama yok, sarma var) yan butonlar −15 sn / +30 sn olmalı.

### K3. Çerçeve 15 tık → telefon sesi 15 adım, zıplama yok

1. Telefon medya sesini 0'a indir. Kontrol için:
   `adb -s <telefon> shell cmd media_session volume --stream 3 --get`
   (çalışmazsa: `adb -s <telefon> shell dumpsys audio | grep -A4 "STREAM_MUSIC"`)
2. Saatte Şimdi Çalıyor ekranındayken çerçeveyi saat yönünde **tam 15 tık** çevir. Önce yavaş
   (tık başına ~1 sn), sonra hızlı (tek hamlede) dene.
3. Her iki durumda telefon sesi tam 15 adım artmış olmalı (A55'te maksimum genelde 15'tir; değilse
   `volumeMax` değerine kadar). Komutla tekrar kontrol et.
4. Ters yönde 15 tık: ses 0'a dönmeli.
5. Zıplama testi: hızlı çevirirken ses göstergesi geriye atlamamalı. Bırakınca telefondaki değerle
   aynı sayıda durmalı.
6. Sınırda (0 ya da max) bir kez belirgin titreşim olmalı. Çevirmeye devam edince titreşim tekrarlamamalı.

### K4. İlerleme halkası 5 dk boyunca telefondan en fazla 1 sn sapıyor

1. 6 dakikadan uzun bir parça başlat.
2. Saat ve telefon ekranını yan yana koy. 0., 1., 3. ve 5. dakikada iki ekranın fotoğrafını tek karede çek.
3. Saatteki alt kavisteki süre ("1:12 · −2:33") ile telefondaki geçen süre arasındaki fark ≤ 1 sn olmalı.
4. Arada bir kez telefondan sar (seek). Saat 1 sn içinde yeni pozisyona atlamalı.

### K5. Telefon Bluetooth'u kapanınca 5 sn içinde bağlantı hatası

1. Saatte Akış açık ve bir parça çalıyor olsun.
2. Telefonda Bluetooth'u kapat, kronometreyi başlat.
3. Saatte kırmızı ince halka, "Telefona ulaşılamıyor" ve "Tekrar dene" butonu ≤ 5 sn içinde görünmeli.
   Son kapak gri tonlu kalmalı.
4. Bluetooth'u aç, **Tekrar dene**'ye bas. Normal görünüm geri gelmeli.
5. Saat Wi-Fi üzerinden de telefona bağlanabildiği için önce saatin Wi-Fi'sini de kapatarak test et.
   Wi-Fi açıkken bağlantı kopmayabilir; bu beklenen davranış.

### K6. Tile'dan oynat/duraklat çalışıyor

1. Akış tile'ını ekle (bkz. 4. bölüm). Uygulamayı kapat.
2. Tile'da oynat/duraklat'a dokun. Telefonda çalma durmalı ve başlamalı. Tile ikonu değişmeli.
3. Önceki ve sonraki butonlarını dene.
4. Telefonda parçayı değiştir. Tile birkaç saniye içinde yeni başlığı göstermeli (sistem tile
   güncellemelerini seyreltebilir).
5. Tekrar tetiklenme testi: tek dokunuştan sonra tile 1–2 kez yenilense bile çalma durumu yalnızca
   bir kez değişmeli.
6. Telefonda hiçbir şey çalmıyorken tile "Bir şey çalmıyor" ve "Son çalanı sürdür" göstermeli.

### K7. Bildirim erişimi yokken onboarding doğru adımı gösteriyor

1. **Köprü kurulu değil:** `adb -s <telefon> uninstall com.erenkng.akis`. Saatte Akış'ı aç. İlk
   açılışta "Telefonundaki müziği bilekten yönet" görünmeli. **Başla** → "Telefonda Akış Köprü'yü kur".
   **Telefonda aç** telefonda Play Store sayfasını açmaya çalışmalı (uygulama yayında olmadığı için sayfa
   boş olabilir).
2. **Köprü kurulu, izin yok:** Köprüyü kur ama izin verme (ya da 3. bölümdeki `disallow_listener` komutunu
   çalıştır). Saat "Bildirim erişimine izin ver" adımını göstermeli. **Telefonda aç** telefonda Akış
   Köprü kurulum ekranını açmalı.
3. **İzin verildi:** Telefonda izni ver. Saat, kullanıcı hiçbir şeye dokunmadan ana ekrana geçmeli.
4. **İzin geri alındı:** Kullanırken `disallow_listener` çalıştır. Saat birkaç saniye içinde 3. adıma
   dönmeli.

## 6. Ek kontroller

- **Soğuk açılış < 700 ms:** Uygulamayı kapat (`adb shell am force-stop com.erenkng.akis`), sonra aç.
  Son kapak canlı veri gelmeden önce diskten görünmeli.
- **Kaynaklar paneli:** Telefonda iki uygulama birden açık olsun. Şimdi Çalıyor'da yukarı kaydır, iki
  oturum da "Şu an aktif" altında görünmeli. Diğerine dokununca ana ekran o oturuma geçmeli. Bir uygulamayı
  kapat, "Son kullanılanlar"a düşmeli. Dokununca telefonda çalmaya başlamalı (OPEN_QUESTIONS S8).
- **Türkçe karakterler:** "Dağlar ğ ş İ ı" gibi bir başlıkta kayan yazı doğru görünmeli.
- **TalkBack:** Butonlar Türkçe okunmalı ("Oynat", "Sonraki parça", "Ses: 8 / 15").
- **Büyük yazı:** Sistem yazı boyutunu büyütünce başlık 2 satıra çıkmalı, sanatçı gizlenmeli.
