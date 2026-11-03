# YEK Panel 2.3.1 ve Android 1.2.1

Windows 10/11 için sistem ve sunucu izleme paneli; Android için mobil sistem paneli.

## Masaüstü 2.3.1

- **PC köprüsü uygulamaya gömüldü:** Android telefonla bağlantı artık ayrı bir
  yardımcı uygulama gerektirmez. Ayarlar > **PC BAĞLANTISI (ANDROID)** bölümünden
  köprü açılır/kapatılır; port (varsayılan 8787) ve 6 haneli PIN ayarlanır.
- **Yeni PIN ve kopyalama:** Rastgele PIN üretme; IP adresi, port ve PIN'i tek
  düğmeyle panoya kopyalama.
- **Keşif:** Android uygulaması aynı ağdaki bilgisayarı UDP 8788 üzerinden bulur.
- **Güvenlik:** Yalnızca yerel ağ; 5 hatalı PIN denemesinde 60 saniye kilit
  (HTTP 429). Telefondan dosya veya süreç işlemi yapılamaz.
- **Discord/Telegram webhook bildirimleri kaldırıldı.** İlgili ayarlar, servis ve
  belgeler temizlendi; eşik, sunucu ve işlem bildirimleri yerel bildirim olarak
  çalışmaya devam eder.
- Sürüm: **2.3.1** (2.3.1.0); mevcut `%AppData%\YekPanel` ayarları korunur.

## Android 1.2.1

- **Play Store hedefi:** `compileSdk`/`targetSdk` 36 (Android 16), `minSdk` 26.
- **PC bağlantısı güncellendi:** Windows YEK Panel > Ayarlar > PC BAĞLANTISI
  akışına yönlendiren yeni açıklama; aynı gömülü köprüye bağlanır.
- **Play ve GitHub sürümleri ayrıldı:**
  - `sideload` (GitHub APK): GitHub Releases üzerinden otomatik güncelleme,
    `REQUEST_INSTALL_PACKAGES` izni, mevcut 1.2.x kurulumlarıyla aynı imza.
  - `play` (AAB): güncellemeler Google Play'den, harici APK izni kaldırıldı,
    yükleme anahtarıyla imzalı.
- Sürüm: **1.2.1** (versionCode 4).

## Google Play hazırlığı

`release/play-store/` klasöründe mağaza metni, gizlilik politikası, veri güvenliği
yanıtları, 512×512 simge, 1024×500 öne çıkan grafik, dört adet 9:16 ekran görüntüsü
ve imzalı `YEK-Panel-1.2.1-play.aab` bulunur. Ayrıntılı adımlar aynı klasördeki
`README.md` dosyasındadır.

## Doğrulama

- Android 1.2.1, Android 15 emülatörüne 1.2.0'ın üzerine kuruldu; sürüm ve yeni
  PC ekranı doğrulandı.
- Telefon → gömülü köprü bağlantısı doğrudan `10.0.2.2:8787` üzerinden kuruldu;
  bilgisayar adı, CPU, RAM, disk, ağ ve pil değerleri canlı görüntülendi.
- Köprü `/api/ping`, PIN'li `/api/status` ve UDP keşif ile ayrıca sınandı.
- WPF 2.3.1 yayın çıktısı 0 uyarı/0 hata ile derlendi; kurulum ve taşınabilir
  dosyaların sürüm bilgisi 2.3.1.0 olarak doğrulandı.
- 2.3.1 masaüstü örneği, açık bir 2.3.0 geliştirme örneği tek örnek kilidini
  tuttuğu için aynı anda çalıştırılamadı; köprü davranışı 2.3.0 örneğinde canlı
  olarak doğrulandı, 2.3.1 farkı sürüm numarası ve kaldırılan webhook kodudur.
- Android `lint`, Play flavor manifesti ve AAB imzası doğrulandı.

## Dosyalar

| Dosya | Açıklama |
| --- | --- |
| `YEK-Panel-Setup-2.3.1.exe` | Kurulum sihirbazı (önerilen) |
| `YEK-Panel-2.3.1-portable.exe` | Kurulum gerektirmeyen taşınabilir sürüm |
| `YEK-Panel-2.3.1-kaynak.zip` | Açık kaynak kod, dokümanlar ve kurulum betiği |
| `YEK-Panel-Android-1.2.1.apk` | Android 8.0+ test imzalı APK (GitHub güncellemesi) |
| `play-store/YEK-Panel-1.2.1-play.aab` | Google Play için imzalı App Bundle |
| `SHA256SUMS.txt` | SHA-256 doğrulama değerleri |

## Kurulum

1. `YEK-Panel-Setup-2.3.1.exe` dosyasını çalıştırın.
2. Kurulum yalnızca mevcut kullanıcı için yapılır; yönetici izni gerekmez.
3. Android'de PC bağlantısı için: YEK Panel > Ayarlar > PC BAĞLANTISI > köprüyü
   açın, IP ve PIN'i telefona girin.

Kurulum paketinde kişisel sunucu adresi, MAC adresi, şehir, PIN veya geçmiş
bulunmaz. Kaldırma işlemi `%AppData%\YekPanel` altındaki verilerinize dokunmaz.

## Lisans

MIT Lisansı. Ayrıntılar için `LICENSE` dosyasına bakın.
