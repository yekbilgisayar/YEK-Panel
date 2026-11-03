# YEK Panel PC Köprüsü

> **Bu araç artık gerekli değildir.** PC bağlantısı YEK Panel 2.3.1 masaüstü
> uygulamasına gömülmüştür. Windows uygulamasında **Ayarlar > PC BAĞLANTISI**
> bölümünü kullanın; Android 1.2.1 aynı bağlantıyı doğrudan kurar. Bu klasör
> yalnızca eski sürümlerle geriye dönük uyumluluk için tutulmaktadır.

Windows bilgisayarın sistem verilerini **yalnızca yerel ağdaki** YEK Panel Android
uygulamasına açar. Masaüstü YEK Panel uygulamasının ayarlarına, dosyalarına veya
otomatik başlatmasına dokunmaz; ayrı ve bağımsız bir araçtır.

- Köprü sürümü: `1.0.0`
- Gereksinim: Windows 10/11, Windows PowerShell 5.1 (Windows ile birlikte gelir)
- Varsayılan HTTP portu: `8787`
- Varsayılan keşif portu (UDP): `8788`
- Veri yönü: PC → telefon; telefondan PC'ye komut gönderilmez
- İnternet bağlantısı kullanmaz, telemetri göndermez

## Hızlı başlangıç

1. `Baslat.bat` dosyasına çift tıklayın.
2. Windows Defender Güvenlik Duvarı sorarsa **Özel ağlar** için izin verin.
   Ortak/kafe Wi-Fi ağlarında izin vermeyin.
3. Konsolda yazan **PIN** ve bilgisayarın yerel IP adresini not edin.
4. Telefonda YEK Panel > **PC** sekmesine IP ve PIN'i girin, **BAĞLAN**'a dokunun.
   Aynı ağdaysanız **PC'Yİ BUL** düğmesi köprüyü UDP ile bulabilir.
5. Kapatmak için konsol penceresini kapatın veya `Ctrl+C` basın.

İlk çalıştırmada rastgele 6 haneli bir PIN üretilir ve
`%AppData%\YekPanelBridge\config.json` dosyasına yazılır. Sonraki açılışlarda aynı
PIN ve port kullanılır. PIN'i değiştirmek için köprüyü kapatıp bu dosyayı silin;
yeni PIN bir sonraki açılışta üretilir.

## Seçenekler

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\YekPanelBridge.ps1 `
    -Port 8787 -DiscoveryPort 8788 -Pin 482913
```

| Parametre | Açıklama |
| --- | --- |
| `-Port` | HTTP portu (varsayılan 8787) |
| `-DiscoveryPort` | UDP keşif portu (varsayılan 8788) |
| `-Pin` | 6 haneli PIN; verilmezse kayıtlı PIN veya yeni rastgele PIN |
| `-NoDiscovery` | UDP keşif yanıtlayıcısını kapatır |

`-Pin` ile verilen değer yapılandırma dosyasına kaydedilir. `Baslat.bat` bu
parametreleri olduğu gibi PowerShell betiğine aktarır.

## Telefonda görünenler

- Bilgisayar adı, Windows sürümü ve çalışma süresi
- Yerel IP adresleri (sanal/VPN/Tailscale/VMware adaptörleri gizlenir)
- CPU yükü, RAM kullanımı, disk doluluğu
- Ağ indirme/yükleme hızı ve bağlantı hızı
- Pil yüzdesi/şarj durumu ve okunabiliyorsa sıcaklık
- En çok bellek kullanan 5 işlem (PID ve MB)
- Köprü sürümü ve son güncelleme saati

Telefon 2 saniyede bir yeniler; bağlantı koparsa otomatik bağlanma açıksa
yeniden dener.

## Güvenlik

- Her `/api/status` isteği `X-Yek-Token` başlığında doğru PIN ister.
- Aynı IP'den 5 hatalı PIN denemesinden sonra 1 dakika kilit uygulanır (HTTP 429).
- PIN yalnızca yerel ağda, HTTP üzerinden gönderilir; trafik şifrelenmez.
  Güvenmediğiniz ağlarda (kafe, otel, ortak Wi-Fi) köprüyü çalıştırmayın.
- Köprü yalnızca dinler; telefondan dosya/süreç işlemi yapılamaz.
- Windows güvenlik duvarında portu internete açmayın; yalnızca özel ağ izni verin.
- Otomatik başlatma kaydı oluşturmaz, hizmet kurmaz, mevcut YEK Panel'i değiştirmez.

Güvenlik duvarı penceresi hiç çıkmadıysa ve telefon bağlanamıyorsa, yönetici
PowerShell'de yalnızca özel ağ için kural ekleyebilirsiniz:

```powershell
New-NetFirewallRule -DisplayName "YEK Panel PC Koprusu" -Direction Inbound `
    -Protocol TCP -LocalPort 8787 -Action Allow -Profile Private
New-NetFirewallRule -DisplayName "YEK Panel PC Koprusu Kesif" -Direction Inbound `
    -Protocol UDP -LocalPort 8788 -Action Allow -Profile Private
```

Kaldırmak için:

```powershell
Remove-NetFirewallRule -DisplayName "YEK Panel PC Koprusu"
Remove-NetFirewallRule -DisplayName "YEK Panel PC Koprusu Kesif"
```

## Yerel API

| Adres | Yetki | Açıklama |
| --- | --- | --- |
| `GET /api/ping` | Yok | Uygulama adı, sürüm, bilgisayar adı, port |
| `GET /api/status` | PIN | Sistem ölçümleri |
| `GET /` | Yok | Tarayıcıda kısa bilgi sayfası |

`/api/status` örnek yanıtı (kısaltılmış):

```json
{
  "ok": true,
  "app": "YEK Panel PC Köprüsü",
  "version": "1.0.0",
  "name": "PC-ADI",
  "os": "Windows 11 Pro 10.0.26100",
  "uptimeSec": 11786,
  "cpuPercent": 18.5,
  "ramTotal": 33970810880,
  "ramUsed": 21222596608,
  "ramPercent": 62,
  "disks": [{"name": "C:", "total": 1023040024576, "free": 441211445248, "percent": 57}],
  "netDown": 5444,
  "netUp": 3852,
  "linkMbps": 144,
  "ips": ["192.168.1.50"],
  "batteryPercent": 82,
  "batteryCharging": false,
  "tempC": null,
  "processes": [{"pid": 17640, "name": "chrome", "memMb": 627}],
  "ts": 1793640723
}
```

UDP keşif: `8788` portuna `YEK-PANEL-DISCOVER` metni gönderilir; köprü
`{"ok":true,"app":...,"version":...,"name":...,"port":8787,"protocol":1}`
yanıtı döndürür.

## Sorun giderme

- **Telefon bağlanamıyor:** Aynı Wi-Fi/LAN'da olun; IP'yi köprü konsolundan
  doğrulayın. Windows ağ profili "Özel" olmalı. Misafir ağı istemci izolasyonu
  kullanıyorsa cihazlar birbirini göremez.
- **"PIN hatalı":** Konsoldaki 6 haneli PIN'i girin. 5 hatalı denemeden sonra
  1 dakika bekleyin.
- **"PC yanıt vermedi":** Güvenlik duvarı kuralını ve 8787 portunu kontrol edin.
- **Port dolu:** `-Port 8790` gibi boş bir TCP portu seçin; telefondaki port
  alanını aynı değere getirin.
- **Türkçe karakterler bozuk:** `Baslat.bat` yerine doğrudan PowerShell betiğini
  çalıştırın; Windows PowerShell 5.1 kullanın (PowerShell 7 gerekmez).

## Dosyalar

| Dosya | Açıklama |
| --- | --- |
| `YekPanelBridge.ps1` | Köprü sunucusu (HTTP + UDP keşif) |
| `Baslat.bat` | Çift tıkla başlatma |
| `README.md` | Bu belge |

Köprü, Android uygulamasıyla aynı depoda açık kaynak olarak yayınlanır. Lisans: MIT.
