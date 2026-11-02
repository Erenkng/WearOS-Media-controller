# İmza anahtarı

`debug.keystore` **yalnızca debug derlemeleri** içindir. Hem `:mobile` hem `:wear` modülünün debug
derlemesi bu anahtarla imzalanır (`*/build.gradle.kts` → `signingConfigs.debug`).

| Alan | Değer |
|---|---|
| Dosya | `signing/debug.keystore` (PKCS12) |
| Alias | `androiddebugkey` |
| Parola (store + key) | `android` |
| Sertifika | `CN=Android Debug, O=Akis, C=TR`, 2056'ya kadar geçerli |
| SHA-256 | `25:93:6C:29:6B:13:85:13:39:6C:B6:AC:5E:21:BD:7A:F0:FB:98:E7:81:9C:7A:65:09:75:57:E4:11:03:F0:92` |

## Neden repoda?

- **Data Layer şartı:** Saat ve telefon uygulaması ancak aynı paket adına ve **aynı imzaya** sahipse
  birbirini görür. Android'in varsayılan debug anahtarı her bilgisayarda (ve her CI çalıştırmasında)
  farklıdır.
- **Güncelleme:** Aynı imzayla derlenen yeni bir APK, eskisinin üstüne kaldırmadan kurulur.
  Anahtar değişseydi her seferinde önce eski uygulamayı silmek gerekirdi.

CI (`.github/workflows/build.yml`) iki APK'nın bu anahtarla imzalandığını her derlemede doğrular.

## Güvenlik notu

Repo herkese açık olduğu için bu anahtar da herkese açık. Herkes bu imzayla `com.erenkng.akis`
paketli bir APK üretebilir. Bu yüzden:

- Bu imzayı taşıyan APK'ları **yalnızca kendi derlediğin ya da bu repodaki CI'dan indirdiğin**
  haliyle kur. Başka bir yerden gelen böyle bir APK, Akış Köprü'nün üstüne kurulup onun bildirim
  erişimi iznini devralabilir.
- Uygulamayı başkalarıyla paylaşmadan ya da Play Store'a yüklemeden önce **ayrı, gizli bir release
  anahtarı** gerekir (Faz 4). O anahtar repoya konmaz; CI'ya GitHub Secrets ile verilir. Sertifikadaki
  `CN=Android Debug`, bu anahtarın yanlışlıkla Play Store'a gönderilmesine karşı da bir işarettir.
