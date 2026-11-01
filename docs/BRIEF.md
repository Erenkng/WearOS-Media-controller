# Wear OS Medya Kontrolcüsü — UI & Özellik Brief'i

Nov 1, 2026 · @Eren Doğu Kan

## 1. Proje özeti

Hedef: Galaxy Watch 9 için, telefonda (Galaxy A55) çalan **herhangi bir** uygulamanın medyasını kontrol eden bir Wear OS uygulaması. İşlev olarak Samsung'un Media Controller'ının yerine geçer; layout ve etkileşim dili olarak Spotify'ın Mart 2026'daki yeni Wear OS arayüzünü örnek alır; görsel kimlik tamamen özgündür.

Çalışma adı: **Akış** (değiştirilebilir; alternatifler: Nabız, Dalga, Bilek).

### Neden?

- Samsung Media Controller işini görüyor ama görsel olarak sade ve kişiselleştirilemez.
- Spotify'ın yeni arayüzü (büyük kapak, jestle kontrol, tek dokunuş = oynat/duraklat) çok iyi ama sadece Spotify'ı kontrol ediyor.
- Akış = Spotify'ın etkileşim kalitesi + Samsung'un "her uygulamayı kontrol et" yeteneği.

### Hedefler

1. Telefonda çalan her MediaSession'ı (Spotify, YouTube Music, YouTube, podcast uygulamaları, tarayıcı) saatten kontrol etmek.
2. Saatin kendisinde çalan medyayı da kontrol edebilmek (Faz 2).
3. Döner çerçeve ile ses kontrolü, kapak sanatına göre değişen renkler, jestle hızlı kontrol.
4. Tile + komplikasyon + Ongoing Activity ile saat kadranından tek dokunuşla erişim.
5. Pil dostu: ekran kapalıyken veri akışı durur, AOD'de minimal mod.

### Hedef cihazlar

| Cihaz | Rol | Not |
| --- | --- | --- |
| Galaxy Watch 9 | Ana hedef (saat uygulaması) | Wear OS 6+ / One UI Watch; yuvarlak ekran |
| Galaxy A55 5G | Köprü uygulaması (telefon) | Android 14+; bildirim erişimi izni gerekli |
| Diğer Wear OS saatleri | İkincil | Pixel Watch vb. — farklı ekran boyutlarına uyumlu layout |

### Kapsam dışı (şimdilik)

- Kendi müzik çalarımız ya da akış servisi değil; sadece **kontrolcü**.
- Spotify'ın logosu, yeşili, ikonları, "Spotify" adı kullanılmayacak — sadece layout fikri ve etkileşim kalıpları alınacak.
- Uygulamaların kütüphanesinde gezinme (Spotify'ın Home/Search'ü gibi) MVP'de yok; yerine "Kaynaklar" ekranı var (Bölüm 7).
- iOS desteği yok.

## 2. Referans: Spotify Wear OS (Mart 2026 yenilemesi)

Spotify, 17 Mart 2026'da Wear OS uygulamasının yeni arayüzünü tüm kullanıcılara açtı ([Android Authority](https://www.androidauthority.com/spotify-wear-os-app-redesign-rollout-3649744/), [Android Central](https://www.androidcentral.com/apps-software/wear-os/spotify-on-wear-os-just-got-a-big-redesign-that-makes-it-much-easier-to-use)). Şubat'ta Galaxy Watch 7'de sınırlı testte görülmüştü ([Android Authority](https://www.androidauthority.com/spotify-wear-os-now-playing-redesign-3640437)). Akış'ın layout'u bu yapıyı temel alır.

### Spotify'ın yapı taşları

| Spotify özelliği | Nasıl çalışıyor | Akış'taki karşılığı |
| --- | --- | --- |
| Açılış ekranı = oynatıcı | Uygulama doğrudan çalan şarkıyla açılır; kapak sanatı başlık ve kontrollerin arkasında durur | Aynı: Ana ekran "Şimdi Çalıyor" |
| Music-First Mode | Açılış ekranında aşağı kaydırınca büyük kapaklı sürükleyici görünüm açılır | **Odak Modu** (aşağı kaydır) |
| Odak jestleri | Tek dokunuş = oynat/duraklat, çift dokunuş = sonraki parça | Aynı + uzun basış = önceki parça (Akış'a özgü) |
| Keşif paneli | Yukarı kaydırınca Home, Search, Library, Downloads | **Kaynaklar paneli** (yukarı kaydır): aktif uygulamalar, son çalanlar, çıkış, ayarlar |
| Bağlamsal buton | Oynatıcıdaki yeni buton: sıra yönetimi, uyku zamanlayıcısı, genişletilmiş menü | **Daha fazla (⋯)** menüsü: sıra, uyku zamanlayıcısı, karıştır/tekrarla, beğen |
| Kütüphane | İndirmeler, çalma listeleri, podcastler, sanatçılar, beğenilen albümler | MVP'de yok; sıra (queue) ekranı varsa uygulamanın sırası |
| Çıkış ve ses | Çıkış seçici ve ses kontrolü; "telefonun" / "bu saat" gibi hedefler | **Hedef seçici** (Telefon / Saat) + döner çerçeve ile ses |

### Spotify'dan alınacak dersler

- **Açılışta sıfır gecikme:** Kullanıcı uygulamayı açtığında menü değil, şarkı görür.
- **Jestler kulaklık mantığında:** Bakmadan kontrol — koşarken ekranın herhangi bir yerine dokunmak yeter.
- **Dikey eksen = derinlik:** Aşağı = daha sade/sürükleyici, yukarı = daha fazla seçenek. Yatay kaydırma sisteme (geri) bırakılır.
- **Tek "⋯" butonu:** Az kullanılan her şey tek menüde; ana ekran kalabalıklaşmaz.

### Spotify'dan **alınmayacaklar**

- Yeşil vurgu rengi, logo, ikon seti, "Spotify" tipografisi ve marka ifadeleri.
- Ekran görüntüsünün piksel piksel kopyası — oranlar ve yerleşim mantığı alınır, görsel dil Bölüm 4'teki gibi özgün olur.

## 3. Referans: Samsung Media Controller

Samsung'un uygulaması işlevsel referanstır: eşleşmiş telefondaki medyayı ve saatte çalan müziği kontrol eder ([APKMirror sürüm notu](https://apkmirror.com/apk/samsung-electronics-co-ltd/media-controller-wear-os/media-controller-wear-os-1-0-24-6142-release)). Akış bu davranışları birebir karşılamalı.

| Samsung özelliği | Kaynak | Akış'ta |
| --- | --- | --- |
| Oynat/duraklat (döner çerçeveyle de), önceki/sonraki | [APKMirror](https://apkmirror.com/apk/samsung-electronics-co-ltd/media-controller-wear-os/) | MVP |
| Medya çalınca otomatik açılma | [APKMirror](https://apkmirror.com/apk/samsung-electronics-co-ltd/media-controller-wear-os/) | Ongoing Activity + kadran çipi (Bölüm 9); tam otomatik açılma deneysel |
| Saat kadranında çalan uygulamanın ikonu | [AT&T rehberi](https://www.att.com/device-support/article/wireless/000112348/Samsung/SamsungSML505U) | Ongoing Activity çipi |
| Ses düğmesi → ses kaydırıcı, çıkış cihazı göstergesi | [Cloudspress](https://www.cloudspress.com/?p=622186) | MVP: döner çerçeve + ses ekranı |
| Media Controller tile'ı | [SamMobile](https://www.sammobile.com/?p=16590878) | MVP: kendi tile'ımız |
| "Medya kontrollerini göster" ayarı | [SamMobile](https://www.sammobile.com/?p=16590878) | Ayarlar'da "Otomatik öne çık" anahtarı |

### Samsung'un zayıf noktaları (Akış'ın fırsatı)

- **Telefon sesi tutarsızlığı:** Galaxy Watch 4 Classic kullanıcıları, saatten telefon sesini değiştirince sesin rastgele zıpladığını bildirmiş ([Samsung Community](https://eu.community.samsung.com/t5/wearables/media-controller-no-longer-correctly-controls-phone-volume/m-p/8338510/highlight/true)). Akış'ta ses değeri telefondan geri bildirimle doğrulanacak (Bölüm 10).
- **Ses için çok adım:** Oynatıcı → ses ikonu → kaydırıcı ([Samsung Developers forumu](https://forum.developer.samsung.com/t/control-phone-volume-from-watch/7650)). Akış'ta ana ekranda çerçeveyi çevirmek doğrudan sesi değiştirir.
- **Görsel olarak düz:** Kapak sanatı küçük, renk uyarlaması yok.

### Kritik teknik fark

Samsung'un uygulaması sistem ayrıcalığıyla çalışır; telefonun medya oturumlarını doğrudan görür. Üçüncü taraf bir saat uygulaması bunu yapamaz. Bu yüzden Akış **iki parçalıdır**: saatte arayüz, telefonda küçük bir **köprü uygulaması**. Ayrıntı Bölüm 10'da.

## 4. Tasarım dili

Kural: Spotify gibi **hissettirsin**, Spotify gibi **görünmesin**. Ortak olan: koyu zemin, kapak sanatının baskınlığı, kalın başlık, az buton. Farklı olan: renk, şekil dili, ikonlar, hareket.

Temel: Compose for Wear OS **Material 3 Expressive** — Google'ın Wear OS için önerdiği tasarım sistemi ([Android Developers](https://developer.android.com/training/wearables/apps)). Üzerine kendi temamız giydirilir.

### Renk

| Token | Değer | Kullanım |
| --- | --- | --- |
| `bg` | #000000 | Her ekranın zemini (OLED, pil) |
| `surface` | #141416 | Kartlar, liste satırları |
| `surfaceHigh` | #202024 | Basılı durum, menü arka planı |
| `onSurface` | #F2F0EB | Ana metin (saf beyaz değil, hafif sıcak) |
| `onSurfaceDim` | #9C9890 | Sanatçı adı, ikincil metin |
| `accentFallback` | #FF8A3D (Kor turuncusu) | Kapaktan renk çıkmazsa vurgu |
| `accentDynamic` | Kapaktan çıkarılır | Oynat butonu, ilerleme halkası, ses göstergesi |
| `danger` | #FF5A5F | Hata, bağlantı koptu |

**Dinamik renk kuralı:** `androidx.palette` ile kapaktan `vibrant` → `lightVibrant` → `dominant` sırasıyla renk alınır. Seçilen renk siyah üzerinde en az **4.5:1** kontrast sağlamıyorsa açıklığı artırılır (HSL L ≥ 0.55). Renk geçişi 400 ms `tween` ile yapılır; şarkı değişince ekran "yanıp sönmez".

Spotify yeşili (#1DB954 civarı) ve ona yakın tonlar (H 130–150°) **yasak**; dinamik renk bu aralığa düşerse ton 20° kaydırılır.

### Tipografi

- Yazı tipi: **Manrope** (Google Fonts, OFL lisans) — `res/font` içine gömülü, internet gerektirmez. Alternatif: Wear sistem fontu.
- Şarkı adı: 16 sp, ExtraBold (800), 1 satır, kayan yazı (marquee) 2 sn bekleyip başlar.
- Sanatçı: 13 sp, Medium (500), `onSurfaceDim`, 1 satır.
- Uygulama etiketi ("YouTube Music'ten"): 10 sp, SemiBold, harf aralığı +0.08 em, BÜYÜK HARF.
- Süre (0:42 / 3:15): 11 sp, tabular rakamlar (`fontFeatureSettings = "tnum"`).

### Şekil dili

- Oynat/duraklat butonu: **şekil değiştiren buton** — çalarken yuvarlatılmış kare (köşe %30), duraklatılmışken tam daire. Geçiş 250 ms spring. (M3 Expressive'in şekil morphing fikri; Spotify'da yok.)
- Yan butonlar: daire, 40 dp, `surface` dolgu.
- Kartlar ve liste satırları: tam hap (stadium) şekli, Wear M3 standardı.
- Kapak sanatı: ana ekranda tam ekran arka plan; listelerde daire kırpma (Horologist kuralı: kapak için `Icon` değil `Image` + daire + `ContentScale.Crop` — [Horologist](https://google.github.io/horologist/media-ui-material3)).

### İkonlar

Material Symbols **Rounded**, dolgu 1 (seçili) / 0 (normal), ağırlık 500. Spotify ikon seti kullanılmaz.

### Hareket

- Ekranlar arası: dikey kaydırmada kapak sanatı paralaks yapar (%15 daha yavaş kayar).
- Şarkı değişimi: eski kapak sola 24 dp kayarak söner, yeni kapak sağdan gelir ("sonraki" yönünü gösterir); "önceki"de ters yön.
- Bütün animasyonlar "Animasyonları kaldır" erişilebilirlik ayarında kapanır.

## 5. Ekran mimarisi ve navigasyon

Uygulama düz bir yapıdadır: her ekran Şimdi Çalıyor'dan en fazla bir kaydırma ya da bir dokunuş uzaktadır. Wear OS rehberi de medya uygulamalarında hiyerarşiyi azaltıp düz bir bilgi mimarisi önerir ([Android Developers — Media](https://developer.android.com/design/ui/wear/guides/patterns/media?hl=ko)).

&#91;embedded content: navigasyon haritası · 1 merkez, 2 dikey katman, 3 yan ekran\]

Dikey eksen Spotify'daki gibi çalışır: aşağı = daha sade (Odak Modu), yukarı = daha fazla seçenek (Kaynaklar). Yan ekranlar butonlarla açılır ve sistemin sağa kaydırma jestiyle kapanır.

### Teknik karşılığı

- Ana üçlü (Odak / Şimdi Çalıyor / Kaynaklar) tek bir dikey pager: `VerticalPager`, başlangıç sayfası 1 (orta). Sayfa göstergesi sağ kenarda dikey `VerticalPageIndicator`.
- Yan ekranlar (⋯ Menüsü, Sıra, Ses ve çıkış, Ayarlar, Onboarding) Wear Navigation ile `SwipeDismissableNavHost` içinde ayrı hedefler.
- Tile, komplikasyon ve kadran çipi her zaman doğrudan Şimdi Çalıyor'u açar (deep link: `akis://now`).

## 6. Ana ekran: Şimdi Çalıyor

Uygulamanın açılış ekranı; kullanıcının zamanının %90'ı burada geçer. Wear OS rehberindeki **5 butonlu medya düzeni** temel alınır — bu düzen minimum dokunma alanlarını garanti eder ([Android Developers — Media](https://developer.android.com/design/ui/wear/guides/patterns/media?hl=ko)).

### Yerleşim (yuvarlak ekran, çapın yüzdesi olarak)

Referans: 225 dp çaplı büyük yuvarlak ekran. Tüm ölçüler `LocalConfiguration.screenWidthDp` ile ölçeklenir.

| Katman / öğe | Konum | Boyut | Ayrıntı |
| --- | --- | --- | --- |
| Kapak sanatı (arka plan) | Tam ekran | %100 | `ContentScale.Crop`; üstüne dikey karartma: üstte %35, ortada %55, altta %85 siyah |
| İlerleme halkası | Ekran kenarı, 2 dp içeride | 4 dp kalınlık | Saat 12'den saat yönünde; iz `onSurface` %15, dolgu `accentDynamic` |
| Saat (TimeText) | Üst kavis | Sistem | Wear standardı, her zaman görünür |
| Kaynak etiketi | y %20 | 14 dp ikon + 10 sp metin | Çalan uygulamanın ikonu + adı: "YOUTUBE MUSIC" |
| Şarkı adı | y %32 | genişlik %70 | 16 sp ExtraBold, kayan yazı |
| Sanatçı | y %40 | genişlik %65 | 13 sp, `onSurfaceDim` |
| Önceki | y %55, x %22 | 44 dp | Daire, `surface` %70 opaklık |
| Oynat/Duraklat | y %55, merkez | 64 dp | Şekil değiştiren buton, `accentDynamic` dolgu, ikon siyah |
| Sonraki | y %55, x %78 | 44 dp | Önceki ile simetrik |
| Ses / çıkış | y %77, x %35 | 36 dp | İkon çıkışa göre değişir: kulaklık / telefon / saat / hoparlör |
| Daha fazla (⋯) | y %77, x %65 | 36 dp | Bağlamsal menü (Bölüm 7) |
| Süre | Alt kavis | 11 sp | Kavisli metin: "1:12 · −2:33" |

### Davranış kuralları

- **Döner çerçeve = ses.** Ana ekranda çerçeve/döner düğme çevrilince sesi değiştirir. Ses göstergesi yalnızca çevirme sırasında görünür, 1,5 sn sonra kaybolur — bu Wear OS rehberinin kuralıdır ([Android Developers — Media](https://developer.android.com/design/ui/wear/guides/patterns/media?hl=ko)).
- **Ses göstergesi:** Sağ kenarda, ilerleme halkasının üstünde 60°'lik kavisli çubuk + ortada geçici büyük rakam ("8/15").
- **İlerleme yorumlama:** Pozisyon her saniye telefondan istenmez; son bilinen pozisyon + geçen süre × hız ile saatte hesaplanır (Bölüm 10).
- **İlerleme halkasında sarma:** Halkaya uzun basılınca "sarma modu" açılır; bu modda çerçeve çevirmek ±5 sn sarar, bırakınca `seekTo` gönderilir. Uygulama sarmayı desteklemiyorsa (ACTION\_SEEK\_TO yoksa) kapalı.
- **Butonlar uygulamaya göre:** Sadece PlaybackState'in izin verdiği eylemler etkin. İzin yoksa buton %30 opaklıkla görünür, dokununca hafif titreşim + "Bu uygulama desteklemiyor" toast'ı.

### Duruma göre görünüm

| Durum | Ne değişir |
| --- | --- |
| Çalıyor | Oynat butonu yuvarlatılmış kare, ikon "duraklat"; halka ilerler |
| Duraklatıldı | Buton tam daire, ikon "oynat"; kapak %20 daha karanlık |
| Arabelleğe alıyor | Buton içinde dönen ince halka; diğer butonlar etkin kalır |
| Podcast / sesli kitap | Önceki/sonraki yerine −15 sn / +30 sn (uygulama atlama desteklemiyor ama sarmayı destekliyorsa) |
| Canlı yayın (süre yok) | Halka yerine kenarda "CANLI" etiketi; sarma kapalı |
| Reklam çalıyor | Uygulamanın kapattığı butonlar soluk; kapak yerine kaynak uygulama ikonu |
| Hiçbir şey çalmıyor | Boş durum: büyük nota ikonu, "Telefonda şu an bir şey çalmıyor", **"Son çalanı sürdür"** butonu |
| Telefon bağlı değil | Kırmızı ince halka, "Telefona ulaşılamıyor", "Tekrar dene" butonu; son bilinen kapak gri tonlu |

### Ambient (AOD) modu

- Kapak sanatı yok; siyah zemin.
- Şarkı adı + sanatçı gri tonlu, ince fontla (Light 300), ekranın ortasında.
- İlerleme halkası yok; sadece oynat/duraklat durumunu gösteren 8 dp'lik ince ikon.
- Ekran yanması önlemi: içerik her dakika ±4 dp rastgele kayar.
- Ambient'te telefondan veri akışı durur; sadece şarkı değişimi bildirimi alınır.

## 7. Diğer ekranlar

### 7.1 Odak Modu (ana ekranda aşağı kaydır)

Spotify'ın Music-First Mode'unun karşılığı: bakmadan kontrol için.

- Kapak sanatı tam ekran, **karartma yok**; buton, saat, metin yok.
- Alt kavis: sadece şarkı adı, 3 sn sonra söner (dokununca 3 sn geri gelir).
- İlerleme halkası 2 dp, %40 opaklık.
- Jestler: tek dokunuş = oynat/duraklat · çift dokunuş = sonraki · uzun basış (600 ms) = önceki · çerçeve = ses · yukarı kaydır = ana ekrana dönüş.
- Her jeste farklı titreşim (Bölüm 8), çünkü kullanıcı ekrana bakmıyor.
- Tek ve çift dokunuş çakışması: tek dokunuş 250 ms beklenir; bu sürede ikinci dokunuş gelmezse oynat/duraklat. Ayarlardan "Çift dokunuşu kapat" seçilirse tek dokunuş anında çalışır.
- Ekran açık kalma: Odak Modu'nda ekran sistemin zaman aşımına uyar; isteğe bağlı "Odakta ekranı açık tut" ayarı (varsayılan kapalı, pil uyarısıyla).

### 7.2 Kaynaklar paneli (ana ekranda yukarı kaydır)

Spotify'ın keşif panelinin (Home / Search / Library / Downloads) yerine, kontrolcüye uygun içerik. Wear M3 `TransformingLazyColumn` listesi; döner çerçeveyle kaydırılır.

1. **Hedef seçici** (en üstte, iki parçalı hap): `Telefon` | `Saat`. Kontrollerin hangi cihaza gideceğini belirler.
2. **Şu an aktif** — o cihazdaki tüm aktif medya oturumları. Her satır: uygulama ikonu (daire), şarkı adı, uygulama adı, küçük çalıyor/duraklatıldı göstergesi. Dokununca o oturum "odaklanan" olur ve ana ekrana dönülür.
3. **Son kullanılanlar** — son 5 medya uygulaması (oturumu kapanmış olanlar). Dokununca telefonda o uygulamaya "oynat" medya tuşu gönderilir.
4. **Çıkış** — mevcut ses çıkışı (ör. "Sony WH-1000XM5 · Bluetooth") ve ses ekranına kısayol.
5. **Ayarlar** satırı.

### 7.3 Daha fazla (⋯) menüsü

Spotify'daki sıra / uyku zamanlayıcısı / bağlamsal menü butonunun karşılığı. Tam ekran liste, üstte küçük kapak + şarkı adı başlığı.

| Satır | Ne zaman görünür | Davranış |
| --- | --- | --- |
| Sıra (Queue) | Uygulama `getQueue()` döndürüyorsa | 7.4'e gider |
| Uyku zamanlayıcısı | Her zaman | 15 / 30 / 45 / 60 dk / "Bölüm bitince"; son 30 sn'de ses yavaşça kısılır, sonra duraklatılır, ses eski haline döner |
| Karıştır / Tekrarla | Uygulama destekliyorsa | Tek dokunuşla geçiş, durum çipi |
| Uygulamanın özel eylemleri | PlaybackState özel eylemleri varsa (ör. "Beğen", "Kaydet") | Uygulamanın kendi ikonu ve etiketiyle listelenir |
| Hız (0.8× – 2×) | Podcast/sesli kitapta, uygulama destekliyorsa | Stepper |
| Telefonda aç | Her zaman | Telefonda kaynak uygulamayı öne getirir |

### 7.4 Sıra ekranı

- Liste: mevcut parça vurgulu (accent kenarlık), üstünde "Çalındı", altında "Sırada" başlıkları. Wear OS rehberi önceki parçaları gösteren ve göstermeyen iki sıra türünü ayırır ([Android Developers — Media](https://developer.android.com/design/ui/wear/guides/patterns/media?hl=ko)).
- Açılınca liste mevcut parçaya kaydırılmış gelir.
- Satıra dokunmak = `skipToQueueItem`. Sıra düzenleme (silme, taşıma) yok — çoğu uygulama dışarıya izin vermez.

### 7.5 Ses ve çıkış ekranı

- Ortada büyük ses değeri ("8"), çevresinde 270°'lik kavisli ses çubuğu; çerçeve veya +/− butonları (Wear M3 `Stepper` benzeri).
- Altında çıkış satırı: ikon + cihaz adı. MVP'de sadece **gösterir**; çıkış değiştirme araştırma maddesi (Bölüm 12).
- Telefon hedefindeyken: telefonun medya sesi. Uygulama uzak çalıyorsa (Chromecast, Spotify Connect gibi) oturumun kendi ses kontrolü kullanılır ve "Uzak cihaz: Salon TV" yazar.
- Saat hedefindeyken: saatin medya sesi. Wear OS rehberi, saat hoparlöründen çalmanın kötü deneyim olduğunu ve kulaklığa yönlendirmeyi önerir ([Android Developers — Media](https://developer.android.com/wear/gallery/media)); saat hoparlörü aktifse "Kulaklık bağla" önerisi gösterilir.

### 7.6 Ayarlar

- Otomatik öne çık (medya başlayınca Ongoing Activity + kadran çipi): Açık
- Çift dokunuş jesti: Açık
- Döner çerçeve davranışı: Ses / Parça atlama
- Dinamik renk: Açık / Sabit vurgu (Kor turuncusu)
- Varsayılan hedef: Son kullanılan / Telefon / Saat
- Titreşim yoğunluğu: Kapalı / Hafif / Normal
- Telefon köprüsü durumu: "Bağlı · Galaxy A55" + bildirim erişimi izni durumu
- Hakkında / lisanslar

### 7.7 İlk kurulum (onboarding)

3 ekran, kaydırmalı: (1) "Telefonundaki müziği bilekten yönet", (2) "Telefonda Akış Köprü'yü kur" — telefonda Play Store'u açan buton, (3) "Bildirim erişimine izin ver" — telefonda ilgili ayar ekranını açan buton; izin gelince saat otomatik ana ekrana geçer.

## 8. Etkileşimler

Temel ilke: her kritik eylem **bakmadan** yapılabilmeli. Saatle etkileşim kısa ama sık olduğu için hız ve göz atılabilirlik önemli ([Android Developers — Media](https://developer.android.com/wear/gallery/media)).

### Jest haritası

| Ekran | Jest | Eylem |
| --- | --- | --- |
| Şimdi Çalıyor | Aşağı kaydır | Odak Modu |
| Şimdi Çalıyor | Yukarı kaydır | Kaynaklar paneli |
| Şimdi Çalıyor | Sağa kaydır (sistem) | Uygulamadan çık / geri |
| Şimdi Çalıyor | Çerçeve / döner düğme | Ses |
| Şimdi Çalıyor | Halkaya uzun bas + çerçeve | Sarma (±5 sn adım) |
| Şimdi Çalıyor | Kapağa uzun bas | Hızlı oturum değiştirici (aktif oturumlar arasında çerçeveyle geçiş) |
| Odak Modu | Tek dokunuş | Oynat / duraklat |
| Odak Modu | Çift dokunuş | Sonraki |
| Odak Modu | Uzun basış | Önceki |
| Odak Modu | Yukarı kaydır | Şimdi Çalıyor |
| Listeler | Çerçeve | Kaydırma (rotary scroll + snap) |

Yatay kaydırma uygulama içinde **kullanılmaz** — Wear OS'ta sağa kaydırma sistemin geri jestidir.

### Döner giriş ayrıntıları

- Compose: `Modifier.onRotaryScrollEvent` + `focusRequester`; ekran açılınca odak otomatik istenir, yoksa çerçeve olayları gelmez.
- Galaxy Watch'ta fiziksel çerçeve (Classic) ya da dokunmatik çerçeve olayları aynı API'den gelir; her "tık" bir ses adımı.
- Hızlı çevirmede adımlar birleştirilir: 120 ms içinde gelen tıklar tek komutta gönderilir (telefona mesaj seli olmaz, Samsung'daki ses zıplaması önlenir).
- Ses çevirme sırasında saatte **iyimser güncelleme**: gösterge hemen değişir, telefondan gerçek değer gelince düzeltilir.

### Titreşim (haptik)

| Olay | Titreşim |
| --- | --- |
| Oynat | Tek kısa tık (`CLICK`) |
| Duraklat | Tek yumuşak tık (`TICK`) |
| Sonraki | Çift tık |
| Önceki | Uzun tık (`HEAVY_CLICK`) |
| Ses adımı | Çok hafif tık, her adımda (`SEGMENT_FREQUENT_TICK` / `CLOCK_TICK`) |
| Ses sınırı (0 veya max) | Belirgin tık, tekrar etmez |
| Hata / desteklenmeyen eylem | Çift kısa titreşim |

### Animasyon zamanlamaları

| Animasyon | Süre / tür |
| --- | --- |
| Oynat ↔ duraklat şekil değişimi | Spring, damping 0.7, stiffness Medium |
| Şarkı değişimi (kapak kayması) | 300 ms, `FastOutSlowIn` |
| Dinamik renk geçişi | 400 ms tween |
| Ses göstergesi giriş/çıkış | 150 ms giriş, 1,5 sn bekleme, 300 ms çıkış |
| Odak Modu'na geçiş | Kapak ölçeği 1.0 → 1.08, karartma söner, 350 ms |

## 9. Tile, komplikasyon, Ongoing Activity

Wear OS rehberi, medya oturumunu hızlı başlatmak için tile ve komplikasyonlara yaslanmayı öneriyor ([Android Developers — Media](https://developer.android.com/wear/gallery/media)). Akış üç yüzeyde de var.

### 9.1 Medya tile'ı

- Kütüphane: `androidx.wear.protolayout` Material 3 (`material3` tile bileşenleri).
- Düzen: üstte küçük kaynak etiketi, ortada daire kırpılmış kapak (48 dp) + şarkı adı + sanatçı, altta 3 buton (önceki · oynat/duraklat · sonraki), en altta kenara yaslanan "Aç" hap butonu.
- Tile'da buton eylemi: `LoadAction` ile tıklama kimliği (`play_pause`, `next`, `prev`) → `onTileRequest` içinde `requestParams.currentState.lastClickableId` okunur → komut telefona gönderilir → güncel düzen döner.
- Güncelleme: şarkı/durum değişince `TileService.getUpdater(context).requestUpdate(...)`. Sistem kısıtlaması nedeniyle tile canlı ilerleme göstermez.
- Boş durum: "Bir şey çalmıyor" + "Son çalanı sürdür" butonu.

### 9.2 Komplikasyonlar

| Tür | İçerik | Dokununca |
| --- | --- | --- |
| SHORT\_TEXT | Oynat/duraklat ikonu + şarkı adının ilk 7 karakteri | Akış açılır |
| LONG\_TEXT | "Şarkı — Sanatçı" | Akış açılır |
| SMALL\_IMAGE | Kapak (daire) | Akış açılır |
| MONOCHROMATIC\_IMAGE | Nota ikonu, çalıyorsa dolu | Akış açılır |
| RANGED\_VALUE | Şarkı ilerlemesi (0–süre) | Akış açılır |

Eren kendi saat kadranlarını yaptığı için bu komplikasyonlar özel kadranlara doğrudan eklenebilir.

### 9.3 Ongoing Activity (Samsung'daki "otomatik açılma" karşılığı)

- Telefonda medya başlayınca saatte `OngoingActivity` içeren bir bildirim oluşturulur: kadranın altında küçük animasyonlu çip (kaynak uygulama ikonu) ve son uygulamalar listesinde en üstte Akış.
- Çipe dokunmak → Şimdi Çalıyor.
- Medya 10 dk duraklatılmış kalırsa ya da oturum kapanırsa Ongoing Activity kaldırılır.
- **Tam otomatik açılma** (Samsung gibi uygulamanın kendiliğinden öne gelmesi): Android'in arka plandan Activity başlatma kısıtlaması nedeniyle güvenilir değil. Faz 3'te "deneysel" ayar olarak denenecek; varsayılan davranış Ongoing Activity çipidir.

### 9.4 Saat kadranı uyumu

Galaxy Watch'ta sistem kendi medya kontrolünü de gösterebilir ("Medya kontrollerini göster" ayarı — [SamMobile](https://www.sammobile.com/?p=16590878)). Onboarding'de, Samsung'un kontrolcüsü ile Akış'ın aynı anda öne çıkmaması için bu ayarı kapatma önerisi gösterilir.

## 10. Teknik mimari

Akış iki uygulamadan oluşur: saatte **Akış** (arayüz), telefonda **Akış Köprü** (oturumları okur, komutları uygular). İkisi Wearable Data Layer API ile konuşur. Neden köprü şart: bir uygulamanın başka uygulamaların medya oturumlarını görmesi için ya sistem izni `MEDIA_CONTENT_CONTROL` ya da etkin bir bildirim dinleyicisi (NotificationListenerService) olması gerekir ([Android Developers — MediaSessionManager](https://developer.android.com/reference/android/media/session/MediaSessionManager)). Üçüncü taraf uygulama ancak ikincisini kullanabilir ve bu telefonda çalışmalıdır.

&#91;embedded content: mimari · telefon köprüsü, Data Layer, saat bileşenleri\]

Durum telefondan saate tek yönde akar; saat yalnızca komut gönderir ve hiçbir medya oturumuna doğrudan dokunmaz.

### Modüller

| Modül | Platform | İçerik |
| --- | --- | --- |
| `:shared` | Saf Kotlin | Protokol modelleri (`NowPlayingState`, `Command`, `SessionSummary`), kotlinx.serialization, sabit yollar |
| `:mobile` | Android telefon, minSdk 29 | `AkisNotificationListener`, `SessionTracker`, `CommandExecutor`, `VolumeController`, `ArtworkEncoder`, `WearableListenerService`, kurulum/izin ekranı (tek ekran Compose) |
| `:wear` | Wear OS, minSdk 33 | Compose for Wear OS M3, `PlayerViewModel`, ekranlar, `MediaTileService`, `NowPlayingComplicationService`, `OngoingActivityManager`, ambient desteği |

### Teknoloji yığını

- Kotlin 2.x, Coroutines + Flow, kotlinx.serialization (JSON; ileride protobuf).
- Saat: `androidx.wear.compose:compose-material3`, `wear.compose.foundation`, Navigation (Wear), `androidx.wear.tiles` + `protolayout-material3`, `wear.watchface.complications.datasource`, `wear:wear-ongoing`, `androidx.palette`.
- İsteğe bağlı: Horologist **Media UI Material 3** — hazır oynat/atla/ses butonları ve ses göstergesini yöneten `MediaPlayerScaffold` içerir; belirli bir oynatıcıya bağımlı değildir ([Horologist](https://google.github.io/horologist/media-ui-material3)). Özel tasarım için sadece ses göstergesi ve rotary yardımcıları alınabilir.
- Her iki taraf: `play-services-wearable` (DataClient, MessageClient, CapabilityClient).
- DI: elle (küçük proje); Hilt gereksiz.

### Telefon tarafı akışı

1. `AkisNotificationListener` sistem tarafından bağlanınca `MediaSessionManager.getActiveSessions(componentName)` çağrılır ve `addOnActiveSessionsChangedListener` ile değişiklikler dinlenir.
2. Her `MediaController` için `MediaController.Callback` kaydedilir: `onMetadataChanged`, `onPlaybackStateChanged`, `onQueueChanged`, `onAudioInfoChanged`, `onSessionDestroyed`.
3. "Odaklanan oturum" seçimi: kullanıcı saatten seçtiyse o; değilse listede ilk sıradaki **çalan** oturum; hiçbiri çalmıyorsa en son çalmış olan.
4. Değişiklikler 150 ms debounce ile `NowPlayingState`'e dönüştürülüp gönderilir.

### Protokol (Data Layer yolları)

| Yol | Tür | Yön | İçerik |
| --- | --- | --- | --- |
| `/akis/state` | DataItem (+ Asset `art`) | Telefon → Saat | Odaklanan oturumun tam durumu |
| `/akis/sessions` | DataItem | Telefon → Saat | Aktif oturum listesi (`SessionSummary[]`) |
| `/akis/cmd` | Message | Saat → Telefon | `Command` (aşağıda) |
| `/akis/subscribe` | Message | Saat → Telefon | `{level: FULL / TRACK_ONLY / NONE}` — saat ekranı açık/ambient/kapalı |
| `/akis/ack` | Message | Telefon → Saat | Komut sonucu: `{seq, ok, volume?}` |
| Capability `akis_bridge` | — | — | Saat, köprünün kurulu olduğunu bununla anlar |

`NowPlayingState` alanları: `sessionId`, `packageName`, `appLabel`, `title`, `artist`, `album`, `durationMs`, `positionMs`, `positionUpdatedAtEpochMs`, `speed`, `state` (PLAYING / PAUSED / BUFFERING / STOPPED / ERROR), `actions` (bit maskesi), `customActions[] {id, label, iconPng?}`, `shuffle`, `repeat`, `volume`, `volumeMax`, `isRemote`, `outputLabel`, `artHash`.

`Command` türleri: `PLAY`, `PAUSE`, `TOGGLE`, `NEXT`, `PREV`, `SEEK_TO(ms)`, `SEEK_BY(ms)`, `SET_VOLUME(v)`, `ADJUST_VOLUME(delta)`, `SKIP_TO_QUEUE_ITEM(id)`, `CUSTOM(actionId)`, `SET_SHUFFLE`, `SET_REPEAT`, `FOCUS_SESSION(sessionId)`, `RESUME_LAST`, `OPEN_ON_PHONE`, `SLEEP_TIMER(minutes)`. Her komutta artan `seq` vardır.

### Kritik uygulama detayları

- **Pozisyon hesabı (saatte):** `pos = positionMs + (now − positionUpdatedAtEpochMs) × speed` — yalnızca PLAYING iken; `durationMs` ile sınırlanır. Telefon pozisyonu sadece durum değişince ve en fazla 15 sn'de bir gönderir.
- **Kapak:** Telefonda 256×256 px WebP (%80 kalite, \~15–25 KB) olarak sıkıştırılır; `artHash` aynıysa Asset tekrar gönderilmez. Saatte bellek içi LRU önbellek (10 kapak).
- **Ses:** Oturum uzak çalıyorsa (`playbackInfo.playbackType == REMOTE`) `controller.setVolumeTo` kullanılır; değilse `AudioManager.setStreamVolume(STREAM_MUSIC, v, 0)`. Her ses komutundan sonra gerçek değer `/akis/ack` ile geri döner.
- **Son çalanı sürdür:** Aktif oturum yoksa telefonda `AudioManager.dispatchMediaKeyEvent(KEYCODE_MEDIA_PLAY)` — sistem bunu son medya uygulamasına iletir.
- **Uyku zamanlayıcısı:** Telefonda `AlarmManager` ile; son 30 sn'de ses 6 adımda kısılır, PAUSE gönderilir, ses eski değerine geri alınır.
- **Bağlantı:** `CapabilityClient` ile köprü yoksa onboarding; düğüm erişilemezse "Telefona ulaşılamıyor" durumu; komutlar 3 sn içinde ack almazsa 1 kez tekrar.
- **Saatteki yerel medya (Faz 2):** Aynı NotificationListener yaklaşımı saatte de çalışır, ancak Wear OS'ta bildirim erişimi izni verme ekranı olmayabilir. Geliştirme sırasında `adb shell cmd notification allow_listener <paket>/<servis>` ile verilir; son kullanıcı için yol açık soru (Bölüm 12).

## 11. İzinler, kenar durumlar, erişilebilirlik, pil

### İzinler

| İzin | Nerede | Neden |
| --- | --- | --- |
| Bildirim erişimi (`BIND_NOTIFICATION_LISTENER_SERVICE`) | Telefon | Diğer uygulamaların medya oturumlarını görmek; kullanıcı Ayarlar'dan elle verir |
| `POST_NOTIFICATIONS` | Saat | Ongoing Activity bildirimi |
| `WAKE_LOCK` | Saat | Gerekmez — kullanılmayacak |
| `SCHEDULE_EXACT_ALARM` | Telefon | Uyku zamanlayıcısı (izin yoksa `setWindow` ile ±1 dk sapma kabul) |
| İnternet | — | Yok. Uygulama tamamen çevrimdışı; analiz/telemetri yok |

Bildirim erişimi izni hassas bir izindir: kurulum ekranında tek cümleyle neden gerektiği yazılır ("Sadece medya oturumlarını okumak için; bildirim içeriklerin okunmaz ve saklanmaz"). Kod da buna uymalı: `onNotificationPosted` boş bırakılır.

### Kenar durumlar

| Durum | Beklenen davranış |
| --- | --- |
| İki uygulama aynı anda çalıyor | Odak ilk sıradaki çalan oturumda; Kaynaklar'da ikisi de görünür, kapağa uzun basınca hızlı geçiş |
| Kapak yok | Kaynak uygulama ikonu büyütülüp bulanıklaştırılır, renk ondan çıkarılır |
| Çok uzun şarkı adı (Türkçe karakterler dahil) | Kayan yazı; ğ, ş, İ, ı doğru görünür — Manrope Latin Extended içerir, test edilecek |
| Sağdan sola dil (Arapça şarkı adı) | Metin yönü otomatik (`TextDirection.Content`) |
| Telefon Bluetooth'u kapandı | 5 sn içinde "Telefona ulaşılamıyor" durumu; son kapak gri |
| Köprü yüklü değil | Onboarding'in 2. adımı; telefonda Play Store'u açan buton (`RemoteActivityHelper`) |
| Bildirim erişimi geri alındı | Köprü bunu `onListenerDisconnected` ile algılar, saate `PERMISSION_MISSING` durumu gönderir |
| Uygulama ses kontrolüne izin vermiyor (sabit ses) | Ses göstergesinde kilit ikonu |
| Spotify reklamı / Spotify Connect | Uzak oturumsa ses `setVolumeTo` ile; atlama yasaksa butonlar soluk |
| Saat kapağı çok parlak (beyaz kapak) | Karartma katmanı metin altında min %55; metin kontrastı her zaman ≥ 4.5:1 |

### Erişilebilirlik

- Her butonun `contentDescription`'ı Türkçe ("Oynat", "Sonraki parça", "Ses: 8 / 15").
- TalkBack açıkken Odak Modu jestleri kapatılır (TalkBack çift dokunuşu kendi kullanır); Odak Modu'nda butonlar görünür kalır.
- Minimum dokunma alanı 48 dp (görünen buton küçük olsa bile).
- Sistem yazı boyutu büyütülünce başlık 2 satıra çıkabilir, sanatçı gizlenir.
- "Animasyonları kaldır" açıksa tüm geçişler anlık.

### Performans ve pil hedefleri

| Ölçüt | Hedef |
| --- | --- |
| Soğuk açılış → kapak görünür | < 700 ms (son durum diskten okunur, sonra canlı veriyle güncellenir — Wear OS rehberindeki "yer tutucu arayüzle gecikmeyi gizle" ilkesi, [Android Developers — Media](https://developer.android.com/wear/gallery/media)) |
| Butona dokunuş → telefonda eylem | < 300 ms (Bluetooth) |
| Çerçeve tıkı → ses göstergesi | < 16 ms (iyimser güncelleme) |
| Ekran açık, 1 saat aktif kullanım | Saat pilinden ≤ %3 ek tüketim |
| Ekran kapalı | Veri akışı yok (`subscribe NONE`); sadece şarkı değişince tile/komplikasyon güncellemesi |
| Kare hızı | 60 fps; recomposition sadece değişen öğede (pozisyon için ayrı `State`) |

## 12. MVP ve yol haritası

Dört faz; her faz bir öncekinin kabul kriterleri tamamlanınca başlar. Faz 1 tek başına günlük kullanılabilir bir ürün olmalı.

### Faz 1 — MVP: Telefonu kontrol et

Kapsam: köprü uygulaması, Şimdi Çalıyor ekranı, döner çerçeveyle ses, Kaynaklar paneli (hedef seçici olmadan, sadece telefon), onboarding, temel tile.

- [ ] Telefonda Spotify, YouTube Music ve YouTube çalarken saatte başlık, sanatçı, kapak 1 sn içinde görünüyor
- [ ] Oynat/duraklat, önceki, sonraki üç uygulamada da çalışıyor
- [ ] Çerçeve 15 tık çevrilince telefon sesi 15 adım değişiyor, zıplama yok
- [ ] İlerleme halkası 5 dk boyunca telefondaki süreden en fazla 1 sn sapıyor
- [ ] Telefon Bluetooth'u kapanınca 5 sn içinde bağlantı hatası durumu görünüyor
- [ ] Tile'dan oynat/duraklat çalışıyor
- [ ] Bildirim erişimi yokken onboarding doğru adımı gösteriyor

### Faz 2 — Spotify hissi

Kapsam: Odak Modu ve jestleri, dinamik renk, şekil değiştiren buton, haptik dili, ⋯ menüsü (sıra, uyku zamanlayıcısı, karıştır/tekrarla, özel eylemler), ses ve çıkış ekranı, ambient modu, komplikasyonlar.

- [ ] Odak Modu'nda tek/çift dokunuş ve uzun basış bakmadan 10 denemede 10 kez doğru çalışıyor
- [ ] Dinamik renk hiçbir kapakta 4.5:1 kontrastın altına düşmüyor
- [ ] Uyku zamanlayıcısı süre dolunca sesi kısıp duraklatıyor ve sesi geri alıyor
- [ ] Komplikasyonlar Eren'in kendi kadranında doğru görünüyor

### Faz 3 — Saatin kendi medyası ve otomatik öne çıkma

Kapsam: hedef seçici (Telefon / Saat), saatte yerel oturumların okunması, Ongoing Activity ile otomatik öne çıkma, deneysel tam otomatik açılma.

- [ ] Saatte çalan YouTube Music (çevrimdışı indirme) Akış'tan kontrol edilebiliyor
- [ ] Telefonda medya başlayınca 2 sn içinde kadranda Akış çipi beliriyor

### Faz 4 — Cila

Kapsam: Kapağa uzun basınca hızlı oturum değiştirici, halkada sarma modu, farklı ekran boyutlarında test (Pixel Watch, küçük 40 mm), Play Store hazırlığı (mağaza görselleri, gizlilik metni).

### Açık sorular

- Wear OS 6 / One UI Watch'ta son kullanıcı, saatteki bir uygulamaya bildirim erişimi izni verebiliyor mu? Veremiyorsa Faz 3 yalnızca ADB ile kurulan sürümlerde çalışır.
- Telefonun ses çıkışını (Bluetooth kulaklık ↔ hoparlör) üçüncü taraf uygulama değiştirebiliyor mu? Değiştiremiyorsa çıkış ekranı sadece gösterir.
- Galaxy Watch 9'un dokunmatik/fiziksel çerçeve olayları One UI Watch'ta `onRotaryScrollEvent`'e eksiksiz geliyor mu? İlk gün gerçek cihazda test edilmeli.
- Uygulama adı "Akış" kesin mi?

## 13. Claude Code başlangıç promptu

Bu brief'i Markdown olarak dışa aktarıp repo köküne `docs/BRIEF.md` adıyla koy, sonra Claude Code'a aşağıdaki promptu ver. Prompt yalnızca Faz 1'i ister; sonraki fazlar için aynı kalıpla "Faz 2'yi uygula" denir.

```markdown
Sen deneyimli bir Android / Wear OS geliştiricisisin. `docs/BRIEF.md` dosyası "Akış" adlı bir Wear OS medya kontrolcüsünün tam tasarım ve teknik brief'idir. Önce dosyanın tamamını oku.

Görev: Brief'teki Bölüm 12'deki **Faz 1 (MVP)** kapsamını uygula. Faz 2+ özelliklerini yazma, ama mimariyi onlara kapı açık bırakacak şekilde kur.

Kısıtlar:
- Proje yapısı: Gradle, Kotlin DSL, version catalog (`libs.versions.toml`); modüller `:shared`, `:mobile`, `:wear` (Bölüm 10).
- Saat UI: Compose for Wear OS Material 3. Phone UI: tek ekran Compose (Material 3).
- Protokol: Bölüm 10'daki Data Layer yolları, `NowPlayingState` alanları ve `Command` türleri birebir; modeller `:shared` içinde kotlinx.serialization ile.
- Tasarım: Bölüm 4'teki renk token'ları, Manrope fontu (`res/font`, OFL), Material Symbols Rounded. Spotify'ın adı, logosu, yeşili ve ikonları hiçbir yerde kullanılmayacak.
- Ana ekran yerleşimi Bölüm 6'daki tabloya göre; ölçüler ekran çapına oranla hesaplanacak.
- Döner çerçeve = ses; 120 ms içindeki tıklar birleştirilip tek `ADJUST_VOLUME` komutu gönderilecek; gösterge iyimser güncellenecek.
- Pozisyon telefondan saniyede bir istenmeyecek; saatte Bölüm 10'daki formülle hesaplanacak.
- İnternet izni yok, analiz kütüphanesi yok, `onNotificationPosted` boş.
- Tüm kullanıcıya görünen metinler Türkçe, `strings.xml` içinde.

Çalışma şekli:
1. Önce bir uygulama planı yaz (dosya listesi + her dosyanın sorumluluğu) ve onayımı bekle.
2. Onaydan sonra sırayla: `:shared` → `:mobile` köprü → `:wear` veri katmanı → Şimdi Çalıyor ekranı → Kaynaklar paneli → onboarding → tile.
3. Her adımın sonunda derlemenin geçtiğini doğrula (`./gradlew assembleDebug`).
4. Saat ve telefon için kurulum + test adımlarını `docs/TESTING.md` dosyasına yaz (ADB ile Wi-Fi üzerinden Galaxy Watch'a kurulum dahil).
5. Bölüm 12'deki Faz 1 kabul kriterlerini tek tek nasıl test edeceğimi listele.

Emin olmadığın bir API davranışında tahmin yürütme; bunu açık soru olarak `docs/OPEN_QUESTIONS.md` dosyasına yaz.
```

## Kaynaklar

- [Android Authority — Spotify Wear OS yenilemesinin yayılması (17 Mart 2026)](https://www.androidauthority.com/spotify-wear-os-app-redesign-rollout-3649744/)
- [Android Central — Spotify on Wear OS redesign](https://www.androidcentral.com/apps-software/wear-os/spotify-on-wear-os-just-got-a-big-redesign-that-makes-it-much-easier-to-use)
- [Android Authority — Şubat 2026 test sürümü](https://www.androidauthority.com/spotify-wear-os-now-playing-redesign-3640437)
- [Android Developers — Wear OS medya tasarım kalıpları](https://developer.android.com/design/ui/wear/guides/patterns/media?hl=ko)
- [Android Developers — Wear OS Media galerisi](https://developer.android.com/wear/gallery/media)
- [Android Developers — Wear OS uygulamaları ve Compose](https://developer.android.com/training/wearables/apps)
- [Android Developers — MediaSessionManager](https://developer.android.com/reference/android/media/session/MediaSessionManager)
- [Horologist — Media UI Material 3](https://google.github.io/horologist/media-ui-material3)
- [APKMirror — Samsung Media Controller (Wear OS)](https://apkmirror.com/apk/samsung-electronics-co-ltd/media-controller-wear-os/)
- [SamMobile — Galaxy Watch medya kontrolleri ayarı](https://www.sammobile.com/?p=16590878)
- [Samsung Community — Media Controller ses sorunu](https://eu.community.samsung.com/t5/wearables/media-controller-no-longer-correctly-controls-phone-volume/m-p/8338510/highlight/true)
- [Samsung Developers forumu — Saatten telefon sesi](https://forum.developer.samsung.com/t/control-phone-volume-from-watch/7650)
- [Cloudspress — Galaxy Watch ses ve Music Controller](https://www.cloudspress.com/?p=622186)
- [AT&T — Media Controller rehberi](https://www.att.com/device-support/article/wireless/000112348/Samsung/SamsungSML505U)
