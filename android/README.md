# YEK Panel — Android

YEK Panel'in Android sürümü. Windows panelindeki sistem izleme fikrini telefona taşır:
pil, bellek, depolama, CPU ve ağ durumu tek ekranda; ayrıca ping testi, notlar ve
günlük hatırlatıcılar.

- Paket: `com.yekbilgisayar.yekpanel`
- Sürüm: `1.0.0` (versionCode 1)
- Min. Android: 8.0 (API 26)
- Hedef: Android 15 (API 35)
- Dil: Java 17, harici UI kütüphanesi yok (framework View'ları)

## Özellikler

- **Canlı panel:** pil yüzdesi, şarj durumu, sıcaklık, voltaj ve sağlık; CPU yükü,
  çekirdek sayısı ve ortalama frekans; RAM ve depolama çubukları; bağlantı türü,
  yerel IP, çalışma süresi ve yonga bilgisi.
- **Uyarılar:** pil alt/üst eşiği ve sıcaklık eşiği; panel açıkken izlenir,
  30 dakika bekleme ve bildirim kanalı. Android 13+ bildirim izni isteğe bağlıdır.
- **Ağ testi:** Cloudflare, Google veya Quad9 hedefine ICMP ping; izin yoksa TCP:53
  ölçümüne düşer. Sonuç ortalama/en iyi/en kötü gecikme olarak gösterilir.
- **Notlar:** başlık + gövde, düzenleme, silme, kopyalama, TXT/Markdown dışa aktarma
  (SAF) ve paylaşma.
- **Hatırlatıcı:** günlük saat seçimi, AlarmManager ile bildirim, cihaz yeniden
  başladığında otomatik yeniden zamanlama.

## Gizlilik

- Notlar, hatırlatıcılar ve ayarlar yalnızca cihazdaki uygulama alanında tutulur.
- Sunucu, hesap veya telemetri yoktur.
- İnternet izni yalnızca ping testi içindir; sonuçlar cihazda kalır.

## Doğrulama

Android 15 (API 35) x86_64 emülatöründe çalıştırıldı:

- APK kuruldu ve uygulama hatasız açıldı; üç sayfa (PANEL, ARAÇLAR, AYARLAR) ve kart kaydırma kontrol edildi.
- Ping testi 1.1.1.1 hedefinde 3/3 yanıt, ortalama 35 ms verdi.
- Not kaydedildi, uygulama yeniden başlatıldıktan sonra not ve hatırlatıcı korundu.
- Not TXT olarak `Downloads/yek-panel-notlar.txt` dosyasına aktarıldı.
- 09:00 hatırlatıcısı AlarmManager'a `ReminderReceiver` olarak zamanlandı.
- Durum çubuğu/çentik ve gezinme çubuğu boşlukları edge-to-edge olarak düzeltildi.

Bilinen sınırlama: Android 8 ve üzerinde `/proc/stat` uygulamalara kapalıdır; CPU yükü
okunamadığında kart "yük kısıtlı" gösterir, uygulama çökmez.

## Derleme

Gereksinimler: JDK 17+ ve Android SDK 35 (platform + build-tools).

```bash
cd android
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

`local.properties` içinde SDK yolunu belirtin:

```properties
sdk.dir=C:/Users/KULLANICI/AppData/Local/Android/Sdk
```

Bu depodaki `gradlew` sarmalayıcısı Gradle 8.11.1 kullanır; Android Gradle Plugin
8.7.3'tür.

## Play Store

**Google Play Store sürümü yakında.** Yayın için imzalı Android App Bundle (AAB)
gerekir:

```bash
./gradlew bundleRelease
```

Debug derlemesi yalnızca test içindir; Play Store'a yüklemeyin. Yayın anahtarınızı
depoya eklemeyin; `keystore.properties` veya CI sırrı olarak saklayın.

## Kaynak yapısı

- `app/src/main/java/com/yekbilgisayar/yekpanel/` — `MainActivity`, `DeviceStats`,
  `NoteStore`, `ReminderStore`, bildirim ve önyükleme alıcıları
- `app/src/main/res/` — tema, renkler, vektör simgeler ve uyarlanabilir uygulama simgesi
- `app/src/main/java/com/yekbilgisayar/yekpanel/ui/` — pil halkası, sparkline ve
  doluluk çubuğu çizimleri
