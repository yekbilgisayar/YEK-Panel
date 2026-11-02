# YEK Panel Android 1.2.0

Android uygulaması artık aynı yerel ağdaki Windows bilgisayara bağlanıyor ve GitHub
üzerinden kendini güncelleyebiliyor. Masaüstü YEK Panel uygulaması değiştirilmedi;
PC köprüsü ayrı, taşınabilir bir araçtır ve mevcut YEK Panel ayarlarınıza dokunmaz.

## Yenilikler

- **PC bağlantısı:** Windows'ta çalışan YEK Panel PC Köprüsü'ne 6 haneli PIN ile bağlanma. Bilgisayar adı, Windows sürümü, çalışma süresi, yerel IP'ler, CPU/RAM/disk doluluğu, ağ ve bağlantı hızı, pil/sıcaklık ve en çok bellek kullanan 5 işlem canlı görünür.
- **Keşif:** "PC'Yİ BUL" aynı ağdaki köprüyü UDP ile bulur. PC sekmesinde otomatik bağlanma isteğe bağlıdır.
- **Güvenlik:** PIN zorunlu; aynı IP'den 5 hatalı denemede 1 dakika kilit. Sanal, VPN, Tailscale, VMware ve APIPA adresleri gizlenir.
- **Otomatik güncelleme:** GitHub Releases içindeki `android-*` sürümleri günlük denetlenir. Yeni APK indirilir, sürüm notları gösterilir ve kurulum Android'in kendi onay ekranıyla yapılır. Sessiz kurulum yoktur.
- **Arayüz:** Dört sekme (PANEL, PC, ARAÇLAR, AYARLAR); PC kısayolu eklendi.
- **Yedekleme:** PC bağlantı bilgileri ve güncelleme tercihi JSON yedeğine dahil edildi.
- Sürüm: 1.2.0 (versionCode 3); Android 8.0–15. APK test imzalıdır.

## Kurulum

### Telefon

1. `YEK-Panel-Android-1.2.0.apk` dosyasını indirin.
2. APK'yı açın; Android "bilinmeyen uygulama kaynakları" izni isterse YEK Panel için izin verin.
3. Kurulumdan sonra uygulamayı açın.

### Windows PC köprüsü

1. `YEK-Panel-PC-Koprusu-1.0.0.zip` dosyasını indirip bir klasöre açın.
2. `Baslat.bat` dosyasına çift tıklayın.
3. Windows Defender Güvenlik Duvarı sorarsa yalnızca **Özel ağlar** için izin verin.
4. Konsolda görünen **IP adresi** ve **6 haneli PIN**'i telefonda **PC** sekmesine girin; **BAĞLAN**'a dokunun. Aynı ağdaysanız **PC'Yİ BUL** da kullanılabilir.
5. Köprüyü kapatmak için konsol penceresini kapatın veya `Ctrl+C` basın.

İlk çalıştırmada rastgele PIN üretilir ve `%AppData%\YekPanelBridge\config.json` dosyasına yazılır. PIN değiştirmek için köprü kapalıyken bu dosyayı silin. Köprü otomatik başlatma kaydı oluşturmaz, hizmet kurmaz.

## Güvenlik notları

- Köprü yalnızca yerel ağda çalışır; veriler internete gönderilmez.
- PIN, LAN üzerinde HTTP ile gönderilir; trafik şifrelenmez. Güvenmediğiniz Wi-Fi ağlarında kullanmayın.
- Windows güvenlik duvarında 8787/8788 portlarını internete açmayın; yönlendirme yapmayın.
- Telefondan PC'ye dosya/süreç işlemi yapılamaz; köprü yalnızca sistem ölçümü sunar.
- Play Store sürümünde güncellemeler Google Play üzerinden yapılacaktır; bu APK doğrudan kurulum (sideload) içindir.

## Doğrulama

- PC köprüsü Windows 11'de `/api/ping`, PIN'li `/api/status` ve UDP keşif yanıtıyla doğrulandı.
- Android 15 emülatöründe CPU, RAM, disk, ağ, pil ve işlem listesi canlı görüntülendi.
- Hatalı PIN "PIN hatalı" olarak gösterildi; 5 hatalı denemeden sonra HTTP 429 ve "1 dakika bekleyin" mesajı doğrulandı.
- Sahte GitHub akışıyla güncelleme uçtan uca denendi: 1.3.0 algılandı, APK indirildi, bilinmeyen kaynak izni verildi ve Android kurulum onayıyla kurulum tamamlandı. Test akışı ve test APK'sı temizlendi.
- Güncelleme denetiminde ağ/DNS hataları Türkçe gösteriliyor: "Sunucuya ulaşılamadı. İnternet bağlantısını kontrol edin." Emülatörde internet olmadığı için gerçek GitHub akışı canlı doğrulanamadı.
- `assembleDebug` ve `lintDebug` hatasız; lint "No issues found."

## Dosyalar

| Dosya | Açıklama |
| --- | --- |
| `YEK-Panel-Android-1.2.0.apk` | Android 8.0+ için test imzalı APK |
| `YEK-Panel-Android-1.2.0.sha256` | APK SHA-256 değeri |
| `YEK-Panel-PC-Koprusu-1.0.0.zip` | Windows 10/11 PC köprüsü (PowerShell + başlatıcı + README + lisans) |
| `YEK-Panel-PC-Koprusu-1.0.0.sha256` | ZIP SHA-256 değeri |

## SHA-256

```text
3a8b8811acff60e81f42ee8364b7ed1e436b0d1cea028e7a29a096d2ed46d174  YEK-Panel-Android-1.2.0.apk
2976689639c80df5005db00c3ce474ed446907975773168b6ddf094e5c985a27  YEK-Panel-PC-Koprusu-1.0.0.zip
```

## Bağlantılar

- [Android kaynak ve belgeler](https://github.com/yekbilgisayar/YEK-Panel/tree/main/android)
- [PC köprüsü belgeleri](https://github.com/yekbilgisayar/YEK-Panel/tree/main/pc-bridge)
- [Masaüstü sürüm 2.3.0](https://github.com/yekbilgisayar/YEK-Panel/releases/tag/v2.3.0) — bu sürümde değişiklik yoktur.

**Google Play Store sürümü yakında.** MIT lisansı.
