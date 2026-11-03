# YEK Panel Android 1.2.1

PC bağlantısı artık Windows uygulamasının içinde. Ayrı köprü aracı indirmeye gerek
yok; YEK Panel > Ayarlar > PC BAĞLANTISI bölümünü açıp telefondan bağlanın.

## Yenilikler

- **Gömülü PC köprüsü:** Android uygulaması, Windows YEK Panel 2.3.1 içindeki
  köprüye 6 haneli PIN ile bağlanır. Bilgisayar adı, Windows sürümü, çalışma süresi,
  yerel IP'ler, CPU/RAM/disk, ağ ve bağlantı hızı, pil/sıcaklık ve en çok bellek
  kullanan 5 işlem canlı görünür.
- **Yeni yönlendirme metni:** PC ekranı, IP ve PIN'in Windows uygulamasında nerede
  olduğunu adım adım açıklar.
- **Keşif:** "PC'Yİ BUL" aynı ağdaki bilgisayarı UDP 8788 ile bulur; otomatik
  bağlanma isteğe bağlıdır.
- **Güvenlik:** PIN zorunlu; 5 hatalı denemede 1 dakika kilit. Yalnızca yerel ağ.
- **Google Play hedefi:** `compileSdk`/`targetSdk` 36 (Android 16).
- **İki dağıtım kanalı:**
  - GitHub APK'sı (bu dosya): GitHub Releases üzerinden otomatik güncelleme,
    mevcut 1.2.x kurulumlarıyla aynı imza.
  - Google Play AAB'si: güncellemeler Play üzerinden; harici APK kurma izni yok.
- Sürüm: **1.2.1** (versionCode 4); Android 8.0 ve üzeri.

## Kurulum

1. `YEK-Panel-Android-1.2.1.apk` dosyasını indirin.
2. APK'yı açın; Android "bilinmeyen uygulama kaynakları" izni isterse YEK Panel
   için izin verin.
3. Windows'ta YEK Panel 2.3.1'i açın: **Ayarlar > PC BAĞLANTISI (ANDROID)**,
   köprüyü etkinleştirin ve görünen IP/PIN'i not edin.
4. Telefonda **PC** sekmesine IP, port (8787) ve PIN'i girin; **BAĞLAN**'a dokunun
   veya **PC'Yİ BUL** ile arayın.

## Güvenlik notları

- Köprü yalnızca yerel ağda çalışır; veriler internete gönderilmez.
- PIN ve sistem verileri LAN üzerinde HTTP ile taşınır; trafik şifrelenmez.
  Güvenmediğiniz Wi-Fi ağlarında kullanmayın.
- Windows güvenlik duvarında 8787/8788 portlarını internete açmayın, yönlendirme
  yapmayın.
- Telefondan PC'ye dosya/süreç işlemi yapılamaz; köprü yalnızca sistem ölçümü sunar.
- Play sürümünde uygulama içi APK indirme akışı yoktur; güncellemeler Google Play'den
  gelir.

## Doğrulama

- 1.2.1, Android 15 emülatörüne 1.2.0'ın üzerine kuruldu; sürüm, sekmeler ve yeni
  PC ekranı doğrulandı.
- Telefon → gömülü köprü bağlantısı `10.0.2.2:8787` üzerinden kuruldu; bilgisayar
  adı, CPU, RAM, disk, ağ ve pil değerleri canlı görüntülendi.
- `/api/ping`, PIN'li `/api/status` ve UDP keşif yanıtı ayrıca sınandı; hatalı PIN
  "PIN hatalı", 5 hatalı denemede 1 dakika kilit davranışı korunuyor.
- Play flavor manifestinde `REQUEST_INSTALL_PACKAGES` yok; AAB imzası doğrulandı.
- `lint` hatasız; emülatör test verileri temizlendi.

## Dosyalar

| Dosya | Açıklama |
| --- | --- |
| `YEK-Panel-Android-1.2.1.apk` | Android 8.0+ test imzalı APK (GitHub güncellemesi) |
| `YEK-Panel-Android-1.2.1.sha256` | APK SHA-256 değeri |
| `play-store/YEK-Panel-1.2.1-play.aab` | Google Play için imzalı App Bundle |

## SHA-256

```text
22eda6784b2fe8bd1aeead2af28033570abe009137eb20a115b3df4a2f27d2cd  YEK-Panel-Android-1.2.1.apk
```

## Bağlantılar

- [Android kaynak ve belgeler](https://github.com/yekbilgisayar/YEK-Panel/tree/main/android)
- [Masaüstü sürüm 2.3.1](https://github.com/yekbilgisayar/YEK-Panel/releases/tag/v2.3.1)

**Google Play sürümü hazırlanıyor.** MIT lisansı.
