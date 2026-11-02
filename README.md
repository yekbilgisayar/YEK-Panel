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
- Otomasyon: PowerShell/Python/BAT/PS1 betikleri, Ctrl+Alt+1–9 hızlı başlatma, zamanlanmış görevler, Discord/Telegram webhook bildirimleri.
- 7/30 günlük çevrimiçi raporu, kesinti listesi, bildirim geçmişi ve CSV dışa aktarma.
- Sunucu bazında bakım modu, yavaş yanıt eşiği ve SSL bitiş uyarısı.
- Mini mod, sistem tepsisi, sessiz saatler, günlük özet ve tema renkleri; ana ekran kartlarını sıralama/gizleme.
- Tek örnek koruması: ikinci kez açılırsa mevcut pencere öne gelir.
- GitHub Releases üzerinden güncelleme kontrolü, sürüm notları ve "bu sürümü atla" seçeneği.
- Beklenmeyen hatalar `%AppData%\YekPanel\crash.log` dosyasına yazılır; sürüm numarası tek kaynaktan gelir.

Tam liste: [OZELLIKLER.txt](OZELLIKLER.txt) — 134 özellik.

## Android

YEK Panel'in Android sürümü de aynı depoda: pil detayları, bellek, depolama, CPU ve ağ paneli; sunucu izleme (HTTP/SSL), indirme hız testi, DNS/port tanılama, dış IP, canlı sensörler, eşik uyarıları, sessiz saatler, notlar, günlük hatırlatıcılar ve JSON yedekleme.

Yeni **Android 1.2.0** ile telefon, aynı yerel ağdaki Windows bilgisayara bağlanır: CPU/RAM/disk, ağ hızı, pil/sıcaklık ve en yoğun 5 işlem canlı görünür. Bağlantı 6 haneli PIN ve isteğe bağlı UDP keşif ile korunur. Windows tarafında ayrı çalışan [PC Köprüsü](pc-bridge/README.md) bulunur; masaüstü YEK Panel'e dokunmaz. Android uygulaması ayrıca GitHub Releases üzerinden güncelleme denetler, APK'yı indirir ve kurulum için Android onayı ister.

APK **Releases** bölümünden indirilebilir (`YEK-Panel-Android-1.2.0.apk`); PC köprüsü `YEK-Panel-PC-Koprusu-1.0.0.zip` olarak aynı sürümde yayınlanır. **Google Play Store sürümü yakında** (mağaza sürümünde güncellemeler Play üzerinden yapılır). Kaynak kod `android/` ve `pc-bridge/` klasörlerinde.

## Kaynak kod

- GitHub: [yekbilgisayar/YEK-Panel](https://github.com/yekbilgisayar/YEK-Panel)
- Kaynak ZIP: [YEK-Panel-2.3.0-kaynak.zip](https://github.com/yekbilgisayar/YEK-Panel/releases/download/v2.3.0/YEK-Panel-2.3.0-kaynak.zip)

Kaynak ZIP içinde uygulama kodu, servisler, kurulum betiği, README ve lisans bulunur; derlenmiş exe veya kişisel ayar dosyası bulunmaz.

## İndirme ve kurulum

En son sürümü **Releases** bölümünden indirin.

- **Kurulum:** `YEK-Panel-Setup-2.3.0.exe`
  - Yalnızca mevcut kullanıcı için kurulur; yönetici izni gerekmez.
  - Varsayılan konum: `%LocalAppData%\Programs\YekPanel`
  - Masaüstü ve Başlat menüsü kısayolları oluşturulur.
  - Kaldırma: Başlat > YEK Panel > Kaldır YEK Panel veya Windows Ayarlar > Uygulamalar.
  - Kaldırma ayarlara ve geçmişe dokunmaz; veriler `%AppData%\YekPanel` altında kalır.
- **Taşınabilir:** `YEK-Panel-2.3.0-portable.exe`
  - Kurulum gerekmez; çift tıklayarak çalıştırın.
- **Android:** `YEK-Panel-Android-1.2.0.apk`
  - Android 8.0 ve üzeri; APK'yı telefona kopyalayıp kurun (bilinmeyen kaynak izni gerekir).
  - Test imzalıdır; Google Play Store sürümü yakında.
  - Güncellemeler GitHub Releases içindeki `android-*` sürümlerinden denetlenir;
    yeni APK indirilir, kurulumu Android'in onay ekranı tamamlar.
- **PC Köprüsü:** `YEK-Panel-PC-Koprusu-1.0.0.zip`
  - Windows 10/11; ZIP'i açın ve `Baslat.bat` dosyasına çift tıklayın.
  - İlk çalıştırmada rastgele 6 haneli PIN üretir; konsolda IP ve PIN görünür.
  - Windows güvenlik duvarında yalnızca **Özel ağ** izni verin. Ayrıntılar:
    [pc-bridge/README.md](pc-bridge/README.md).

İlk açılışta sunucu adresleri **boş** gelir. Kendi adreslerinizi **Ayarlar** ekranından girin. Kurulum paketinde kişisel sunucu adresi, MAC adresi, şehir veya geçmiş bulunmaz.

## Veri konumları

| Veri | Konum |
| --- | --- |
| Ayarlar | `%AppData%\YekPanel\settings.json` |
| Sunucu geçmişi | `%AppData%\YekPanel\history.json` |
| Bildirim geçmişi | `%AppData%\YekPanel\notifications.json` |
| Hata günlüğü | `%AppData%\YekPanel\crash.log` |
| CSV raporları | `%AppData%\YekPanel\rapor-*.csv` |
| PC köprüsü ayarı (PIN/port) | `%AppData%\YekPanelBridge\config.json` |

Uygulama klasörüne kullanıcı verisi yazılmaz. Kurulumu kaldırmak bu klasörü varsayılan olarak silmez.

## Geliştirme

Gereksinimler: .NET 10 SDK ve kurulum sihirbazını derlemek için Inno Setup 6.

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
git commit -m "YEK Panel v2.3"
git branch -M main
git remote add origin https://github.com/KULLANICI/REPO.git
git push -u origin main
```

Ardından GitHub'da **Releases > Draft a new release** ile `v2.3.0` etiketi oluşturun ve şu dosyaları ekleyin:

- `release/YEK-Panel-Setup-2.3.0.exe`
- `release/YEK-Panel-2.3.0-portable.exe`
- `release/YEK-Panel-2.3.0-kaynak.zip`
- `release/SHA256SUMS.txt`

## Gizlilik

- Uygulama telemetri göndermez.
- Güncelleme kontrolü yalnızca GitHub Releases API'sine sürüm sorgusu yapar; kişisel veri göndermez. Ayarlardan kapatılabilir.
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
