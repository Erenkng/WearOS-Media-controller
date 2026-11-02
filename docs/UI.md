# Akış — saat arayüzü

Kullanıcının referans ekranlarına (Spotify'ın 2026 Wear OS arayüzü) göre düzenlendi. Brief'teki kural
geçerli: **yerleşim ve etkileşim alınır, görsel kimlik Akış'ındır.** Spotify yeşili, logosu ve ikon seti
kullanılmaz. Vurgu rengi çalan şarkının kapağından çıkar (`ColorRules`, `AccentController`). İkonlar
Material Symbols Rounded, yazı tipi Manrope.

## Sayfalar (dikey kaydırma)

| Sayfa | Ne var |
|---|---|
| **Odak** (aşağı kaydır) | Tam ekran kapak. Üstte vurgu renginde saat. Altta şarkı adı, uygulama ikonu ve sanatçı. Buton yok. |
| **Şimdi Çalıyor** (açılış) | Kapak arka planda karartılmış. Düz saat, büyük şarkı adı, uygulama ikonu + vurgu renginde sanatçı. Önceki / oynat (çevresinde ilerleme halkası) / sonraki. Alt sırada beğen · Kaynaklar · çıkış. |
| **Kaynaklar** (yukarı kaydır) | Aktif oturumlar, son kullanılanlar, çıkış, ayarlar |

## Şimdi Çalıyor yerleşimi (ekran çapının yüzdesi)

| Öğe | Konum | Ayrıntı |
|---|---|---|
| Saat | y %8 | Düz metin, 17 sp. Sistem `TimeText`'i oynatıcıda gizli. |
| Şarkı adı | y %18 | 22 sp ExtraBold, kayan yazı |
| Uygulama ikonu + sanatçı | y %28,5 | 16 dp ikon, 15 sp, vurgu rengi |
| Önceki / Sonraki | y %50, x %18 / %82 | Arka plansız beyaz ikon, 38 dp |
| Oynat / duraklat | y %50, merkez | 66 dp: gri iz, vurgu renginde ilerleme, konumda beyaz nokta |
| Beğen | y %75,5, x %25 | Yalnızca uygulama "beğen" özel eylemi sunuyorsa. Beğenilmişse dolu ve vurgu renginde. |
| Kaynaklar | y %83, merkez | Kaynaklar sayfasına kaydırır |
| Çıkış | y %75,5, x %75 | Kulaklık / uzak cihazda vurgu renginde ve altı çizili. Dokununca ses göstergesi açılır. |

## Odak Modu jestleri

| Jest | Eylem | Titreşim |
|---|---|---|
| Tek dokunuş | Oynat / duraklat | CLICK / TICK |
| Çift dokunuş (250 ms içinde) | Sonraki | Çift tık |
| Uzun basış (600 ms) | Önceki | HEAVY_CLICK |
| Döner çerçeve | Ses | Her adımda hafif tık |

Ekrana bakan kullanıcıya ortada kısa bir ikon geri bildirimi gösterilir. TalkBack'te aynı üç eylem
erişilebilirlik eylemi olarak sunulur.

## Ses göstergesi

Çerçeve çevrilince sağ kenarda 60°'lik bir yay görünür. Saatin yerinde de küçük bir "🔊 8/15" hapı
çıkar. Kontrolleri kapatmaz; 1,5 sn sonra söner.

## Brief'ten farklar (kullanıcının referansına göre)

- İlerleme halkası ekran kenarında değil, oynat butonunun çevresinde. Alt kavisteki süre metni kaldırıldı.
- Ayrı "KAYNAK UYGULAMA" etiketi yerine uygulama ikonu sanatçının yanında.
- Oynat butonu dolu bir şekil değil; çevresinde ilerleme halkası olan beyaz ikon (Faz 2'deki şekil
  değiştiren buton bunun yerine geçmez).
- Odak Modu ve kapaktan dinamik renk Faz 2'den öne alındı.
- Alt ortadaki buton Faz 2'ye kadar Kaynaklar'ı açıyor. Sıra / ⋯ menüsü Faz 2'de buraya bağlanabilir.
