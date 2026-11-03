# YEK Panel — Google Play hazırlık paketi (Android 1.2.1)

Bu klasör, YEK Panel 1.2.1 sürümünün Google Play'e yüklenmesi için gereken
dosyaları içerir. Play Console'a yükleme adımları aşağıdadır.

## Dosyalar

| Dosya | Açıklama |
| --- | --- |
| `YEK-Panel-1.2.1-play.aab` | İmzalı Android App Bundle (Play'e yüklenecek dosya) |
| `magaza-metni.txt` | Uygulama adı, kısa ve tam açıklama |
| `gizlilik-politikasi.html` | Gizlilik politikası (herkese açık URL'de yayımlanmalı) |
| `veri-guvenligi.txt` | Veri güvenliği, içerik derecelendirmesi ve erişim yanıtları |
| `gorseller/ikon-512.png` | 512×512 uygulama simgesi |
| `gorseller/ozellik-grafik-1024x500.png` | 1024×500 öne çıkan grafik |
| `gorseller/ekran-0*.png` | 864×1920 (9:16) telefon ekran görüntüleri |

## 1. İmza ve Play App Signing

- AAB, depo dışında tutulan **yükleme anahtarı** ile imzalanmıştır.
- Play Console'da ilk yüklemede **Play App Signing** etkinleştirilmelidir.
  Google, uygulama imzalama anahtarını oluşturur; yükleme anahtarı yalnızca
  güncelleme yüklemek için kullanılır.
- GitHub'daki `sideload` APK'sı farklı bir anahtarla (mevcut 1.2.x kurulumlarıyla
  aynı) imzalanır. Bu iki dağıtım kanalı birbirinin üzerine kurulmaz; Play
  sürümü Play'den, GitHub APK'sı GitHub Releases'ten güncellenir.

## 2. Gizlilik politikası URL'si

Play, herkese açık ve düzenlenemeyen bir gizlilik politikası adresi ister.
Bu depo public ise GitHub Pages ile yayımlanabilir:

1. `gizlilik-politikasi.html` dosyasını depoda `docs/` klasörüne kopyalayın.
2. GitHub > Settings > Pages bölümünde kaynağı `main` / `docs` seçin.
3. Play Console > Mağaza girişi > Gizlilik politikası alanına
   `https://<kullanıcı>.github.io/<depo>/gizlilik-politikasi.html` adresini girin.

Politika sayfasındaki iletişim bölümüne, Play Console'da kullanacağınız
geliştirici e-posta adresini ekleyin (dosyada bilerek boş bırakılmıştır).

## 3. Play Console adımları

1. **Uygulama oluştur:** Ad `YEK Panel`, varsayılan dil Türkçe, uygulama türü
   "Uygulama", ücretsiz.
2. **Mağaza girişi:** `magaza-metni.txt` içeriğini kopyalayın; simge, öne çıkan
   grafik ve ekran görüntülerini yükleyin.
3. **Uygulama içeriği:** `veri-guvenligi.txt` yanıtlarını girin.
   - Veri toplama: yok
   - Reklam: yok
   - Uygulama erişimi: tüm özellikler hesapsız kullanılabilir
   - İçerik derecelendirmesi: yardımcı program, 3+/Herkes
4. **Hedef kitle:** 13+; çocuklara yönelik değil.
5. **Üretim sürümü:** `YEK-Panel-1.2.1-play.aab` dosyasını yükleyin,
   sürüm notlarını girin ve incelemeye gönderin.

## 4. Teknik doğrulamalar

- `versionCode 4`, `versionName 1.2.1`
- `compileSdk 36`, `targetSdk 36` (Android 16), `minSdk 26`
- Play flavor'ında `REQUEST_INSTALL_PACKAGES` izni yok
- Play flavor'ında güncelleme kontrolü Google Play'e yönlendirilir
- AAB `jarsigner -verify` ile doğrulandı
- Ekran görüntüleri 864×1920 (9:16), simge 512×512, grafik 1024×500

## 5. Notlar

- Play, 31 Ağustos 2026 sonrası yeni uygulama ve güncellemelerde API 36 hedefini
  zorunlu tutar; bu paket API 36 ile derlenmiştir.
- AAB içinde kişisel IP, makine adı, PIN veya test hesabı bulunmaz.
- Yükleme anahtarı ve parolası depoya eklenmez; `YEK_KEYSTORE_PROPERTIES`
  ortam değişkeniyle derleme sırasında okunur.
