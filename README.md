# YEK Panel

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

Windows 10/11 için açık kaynak sistem ve sunucu izleme paneli. Tek dosya olarak çalışır; kurulum gerektirmez. İsteğe bağlı kurulum sihirbazı, masaüstü ve Başlat menüsü kısayolları, kaldırma kaydı içerir.

![YEK Panel kurulum ekranı](installer/setup-welcome.png)

## Özellikler

- CPU, RAM, disk, çalışma süresi, ağ hızı ve süreç takibi; en yoğun 5 işlemi tek tıkla kapatma.
- GPU yükü (%) ve VRAM kullanımı; CPU sıcaklığı ve RAM eşik uyarıları, isteğe bağlı ses.
- Ana ekranda pil, bağlantı türü/hızı, yerel IP, ağ geçidi, DNS, Wi-Fi, genel IP ve tüm diskler; 30 saniyede bir otomatik yenileme, tek tıkla IP kopyalama.
- Web sitesi, sunucu paneli, Home Assistant ve Nextcloud için durum, gecikme, SSL ve TCP port izleme.
- Hava durumu, internet gecikmesi ve dış IP.
- Araç Merkezi sekmeleri: pil, ağ adaptörü, DNS değiştirici, etkin bağlantılar, diskler, ping, port testi, kronometre, geri sayım, pano geçmişi, notlar ve ses cihazları.
- Otomasyon: PowerShell/Python/BAT/PS1 betikleri, Ctrl+Alt+1–9 hızlı başlatma ve zamanlanmış görevler.
- Gömülü PC bağlantısı: Android telefon aynı ağdan CPU, RAM, disk, ağ, pil ve işlem bilgilerini görür; 6 haneli PIN, port seçimi ve UDP keşif. Ayrı yardımcı uygulama gerekmez.
- 7/30 günlük çevrimiçi raporu, kesinti listesi, bildirim geçmişi ve CSV dışa aktarma.
- Sunucu bazında bakım modu, yavaş yanıt eşiği ve SSL bitiş uyarısı.
- Mini mod, sistem tepsisi, sessiz saatler, günlük özet ve tema renkleri; ana ekran kartlarını sıralama/gizleme.
- Tek örnek koruması: ikinci kez açılırsa mevcut pencere öne gelir.
- GitHub Releases üzerinden güncelleme kontrolü, sürüm notları ve "bu sürümü atla" seçeneği.
- Beklenmeyen hatalar `%AppData%\YekPanel\crash.log` dosyasına yazılır; sürüm numarası tek kaynaktan gelir.

Tam liste: [OZELLIKLER.txt](OZELLIKLER.txt) — 134 özellik.

## Android

YEK Panel'in Android sürümü de aynı depoda: pil detayları, bellek, depolama, CPU ve ağ paneli; sunucu izleme (HTTP/SSL), indirme hız testi, DNS/port tanılama, dış IP, canlı sensörler, eşik uyarıları, sessiz saatler, notlar, günlük hatırlatıcılar ve JSON yedekleme.

Yeni **Android 1.2.1** ile telefon, aynı yerel ağdaki Windows bilgisayara bağlanır: CPU/RAM/disk, ağ hızı, pil/sıcaklık ve en yoğun 5 işlem canlı görünür. Bağlantı **YEK Panel 2.3.1 masaüstü uygulamasına gömülüdür**; ayrı bir yardımcı çalıştırmanız gerekmez. Windows'ta **Ayarlar > PC BAĞLANTISI** bölümünü açın, ekrandaki IP ve 6 haneli PIN'i telefona girin. İsteğe bağlı UDP keşif ("PC'Yİ BUL") aynı ağdaki bilgisayarı bulur. Android uygulaması ayrıca GitHub Releases üzerinden güncelleme denetler, APK'yı indirir ve kurulum için Android onayı ister.

APK **Releases** bölümünden indirilebilir (`YEK-Panel-Android-1.2.1.apk`). Android 17 (API 37) hedefli imzalı AAB, mağaza metni, gizlilik politikası, ekran görüntüleri ve yükleme adımları `release/play-store/` altında hazırdır; mağaza sürümünde güncellemeler Play üzerinden yapılır. Kaynak kod `android/` klasöründedir; eski `pc-bridge/` aracı yalnızca geriye dönük uyumluluk için depoda tutulur ve yeni sürümde gerekmez.

### Google Play paketi

- Yükleme dosyası: `release/play-store/YEK-Panel-1.2.1-play.aab` (yükleme anahtarıyla imzalı)
- Mağaza metni, veri güvenliği yanıtları ve görseller: `release/play-store/`
- Gizlilik politikası: `release/play-store/gizlilik-politikasi.html` (Play için herkese açık URL'de yayımlanmalıdır)
- Play sürümünde harici APK kurma izni yoktur; güncellemeler Google Play'den gelir.

## Kaynak kod

- GitHub: [yekbilgisayar/YEK-Panel](https://github.com/yekbilgisayar/YEK-Panel)
- Kaynak ZIP: [YEK-Panel-2.3.1-kaynak.zip](https://github.com/yekbilgisayar/YEK-Panel/releases/download/v2.3.1/YEK-Panel-2.3.1-kaynak.zip)

Kaynak ZIP içinde uygulama kodu, servisler, kurulum betiği, README ve lisans bulunur; derlenmiş exe veya kişisel ayar dosyası bulunmaz.

## İndirme ve kurulum

En son sürümü **Releases** bölümünden indirin.

- **Kurulum:** `YEK-Panel-Setup-2.3.1.exe`
  - Yalnızca mevcut kullanıcı için kurulur; yönetici izni gerekmez.
  - Varsayılan konum: `%LocalAppData%\Programs\YekPanel`
  - Masaüstü ve Başlat menüsü kısayolları oluşturulur.
  - Kaldırma: Başlat > YEK Panel > Kaldır YEK Panel veya Windows Ayarlar > Uygulamalar.
  - Kaldırma ayarlara ve geçmişe dokunmaz; veriler `%AppData%\YekPanel` altında kalır.
- **Taşınabilir:** `YEK-Panel-2.3.1-portable.exe`
  - Kurulum gerekmez; çift tıklayarak çalıştırın.
- **Android:** `YEK-Panel-Android-1.2.1.apk`
  - Android 8.0 ve üzeri; APK'yı telefona kopyalayıp kurun (bilinmeyen kaynak izni gerekir).
  - Test imzalıdır; Google Play paketi `release/play-store/` altında hazırdır (mağaza yayını başvuruya bağlıdır).
  - Güncellemeler GitHub Releases içindeki `android-*` sürümlerinden denetlenir;
    yeni APK indirilir, kurulumu Android'in onay ekranı tamamlar.
- **PC bağlantısı:** Ayrı paket yoktur; YEK Panel 2.3.1 içinde gelir.
  - Windows uygulamasında **Ayarlar > PC BAĞLANTISI** bölümünü açın.
  - İlk açılışta rastgele 6 haneli PIN üretilir; IP ve PIN ekranda görünür.
  - Windows güvenlik duvarında yalnızca **Özel ağ** izni verin.

İlk açılışta sunucu adresleri **boş** gelir. Kendi adreslerinizi **Ayarlar** ekranından girin. Kurulum paketinde kişisel sunucu adresi, MAC adresi, şehir veya geçmiş bulunmaz.

## Veri konumları

| Veri | Konum |
| --- | --- |
| Ayarlar | `%AppData%\YekPanel\settings.json` |
| Sunucu geçmişi | `%AppData%\YekPanel\history.json` |
| Bildirim geçmişi | `%AppData%\YekPanel\notifications.json` |
| Hata günlüğü | `%AppData%\YekPanel\crash.log` |
| CSV raporları | `%AppData%\YekPanel\rapor-*.csv` |
| PC bağlantısı ayarı (PIN/port) | `%AppData%\YekPanel\settings.json` |

Uygulama klasörüne kullanıcı verisi yazılmaz. Kurulumu kaldırmak bu klasörü varsayılan olarak silmez.

## Geliştirme

Gereksinimler: .NET 10 SDK ve kurulum sihirbazını derlemek için Inno Setup 6 veya üzeri.

```powershell
# Tek dosya yayınla
cd YekPanel
dotnet publish -c Release -o publish

# Kurulum sihirbazını derle (Inno Setup kuruluysa)
cd ..\installer
ISCC.exe YekPanel.iss
```

Kaynak yapısı:

- `YekPanel/` — WPF uygulaması ve servisler
- `installer/` — Inno Setup betiği ve sihirbaz görseli
- `release/` — derlenmiş setup ve taşınabilir exe (GitHub Releases'e yükleyin)

## GitHub'a yükleme

Bu klasör doğrudan bir Git deposu olarak kullanılabilir. `release/*.exe` ve taşınabilir exe `.gitignore` ile hariç tutulur; bunları **Releases** bölümüne yükleyin.

```powershell
git init
git add .
git commit -m "YEK Panel v2.3.1"
git branch -M main
git remote add origin https://github.com/KULLANICI/REPO.git
git push -u origin main
```

Ardından GitHub'da **Releases > Draft a new release** ile `v2.3.1` etiketi oluşturun ve şu dosyaları ekleyin:

- `release/YEK-Panel-Setup-2.3.1.exe`
- `release/YEK-Panel-2.3.1-portable.exe`
- `release/YEK-Panel-2.3.1-kaynak.zip`
- `release/SHA256SUMS.txt`

Android için ayrı bir `android-1.2.1` etiketi oluşturun (Latest olarak işaretlemeyin) ve
`release/YEK-Panel-Android-1.2.1.apk` ile `release/YEK-Panel-Android-1.2.1.sha256`
dosyalarını ekleyin. Google Play AAB'si yalnızca Play Console'a yüklenir.

## Gizlilik

- Uygulama telemetri göndermez.
- Güncelleme kontrolü yalnızca GitHub Releases API'sine sürüm sorgusu yapar; kişisel veri göndermez. Ayarlardan kapatılabilir.
- PC bağlantısı verileri yalnızca yerel ağda, PIN doğrulamasıyla sunar; internete gönderilmez. 5 hatalı PIN denemesinde 1 dakika kilit uygulanır.
- Hava durumu Open-Meteo, konum ipwho.is servislerinden ücretsiz alınır.
- Hız testi herkese açık Cloudflare, OVH, ThinkBroadband ve Hetzner sunucularını dener.
- Sunucu kontrolleri yalnızca sizin girdiğiniz adreslere yapılır.

## Notlar

- EXE dosyaları dijital olarak imzalı değildir; Windows SmartScreen uyarı gösterebilir.
- CPU/GPU sıcaklığı için uygulamayı yönetici olarak çalıştırmak gerekebilir.
- Wake-on-LAN için aynı ağda olmak ve BIOS'ta WOL'un açık olması gerekir.

## Lisans

Bu proje **MIT Lisansı** altında açık kaynak olarak yayınlanmıştır. Ayrıntılar için [LICENSE](LICENSE) dosyasına bakın. MIT; kullanım, değiştirme, dağıtma ve ticari kullanım için izin verir; yazılım olduğu gibi sunulur.

---

**English:** YEK Panel is a Turkish Windows desktop dashboard for system and server monitoring. It runs as a single portable EXE and includes an optional per-user installer. Server fields are empty on first run; no personal configuration is bundled.
