# YEK Panel — Android

YEK Panel'in Android sürümü. Windows panelindeki sistem izleme fikrini telefona taşır:
pil, bellek, depolama, CPU ve ağ durumu tek ekranda; ayrıca sunucu izleme, hız testi,
ağ tanılama, canlı sensörler, notlar, hatırlatıcılar ve yedekleme.

- Paket: `com.yekbilgisayar.yekpanel`
- Sürüm: `1.1.0` (versionCode 2)
- Min. Android: 8.0 (API 26)
- Hedef: Android 15 (API 35)
- Dil: Java 17, harici UI kütüphanesi yok (framework View'ları)

## Özellikler

- **Canlı panel:** pil yüzdesi, şarj durumu, sıcaklık, voltaj, akım, şarj sayacı,
  teknoloji ve güç kaynağı; CPU yükü, çekirdek sayısı ve ortalama frekans; RAM ve
  depolama çubukları; bağlantı türü, yerel IP, sinyal gücü, bağlantı hızı, ağ geçidi,
  DNS, çalışma süresi ve yonga bilgisi.
- **Sunucular:** En fazla 8 web sitesi/panel adresi; HTTP durum kodu, gecikme ve SSL
  bitişine kalan gün. Panelde 60 saniyede bir otomatik kontrol ve "Tümünü kontrol et".
- **Uyarılar:** pil alt/üst eşiği ve sıcaklık eşiği; panel açıkken izlenir,
  30 dakika bekleme ve bildirim kanalı. Android 13+ bildirim izni isteğe bağlıdır.
- **Sessiz saatler:** Gece aralığı seçimi; bu aralıkta yalnızca pil/sıcaklık
  bildirimleri susturulur, hatırlatıcılar çalışmaya devam eder.
- **Yenileme sıklığı:** 2, 5 veya 10 saniye; seçim kalıcıdır.
- **Ağ testi:** Cloudflare, Google veya Quad9 hedefine ICMP ping; izin yoksa TCP:53
  ölçümüne düşer. Sonuç ortalama/en iyi/en kötü gecikme olarak gösterilir.
- **Hız testi:** OVH, Cloudflare ve Hetzner sırasıyla yaklaşık 8 MB indirme; Mbps,
  MB, süre ve sunucu bilgisi. Test yalnızca kullanıcı başlatınca çalışır.
- **Ağ tanılama:** DNS çözümleme (kayıtlar ve süre) ve TCP port testi.
- **Dış IP:** Yalnızca dokunulduğunda sorgulanır; sonuç panoya kopyalanır.
- **Canlı sensörler:** İvmeölçer, jiroskop, manyetometre, ışık, yakınlık, basınç,
  nem, ortam sıcaklığı, adım sayacı ve dönüş vektörü. Yalnızca ARAÇLAR sekmesi
  açıkken okunur; geçersiz/olmaya değer sensör çıktıları "—" gösterilir.
- **Notlar:** başlık + gövde, düzenleme, silme, kopyalama, TXT/Markdown dışa aktarma
  (SAF) ve paylaşma.
- **Hatırlatıcı:** günlük saat seçimi, AlarmManager ile bildirim, cihaz yeniden
  başladığında otomatik yeniden zamanlama.
- **Yedekleme:** Ayarlar, notlar, hatırlatıcılar ve sunucular tek JSON dosyasında
  dışa/içe aktarılır (SAF). İçe aktarma onay ister ve eski hatırlatıcıları iptal eder.
- **Tanılama:** Cihaz/pil/ağ/CPU özetini panoya kopyalama veya paylaşma.
- **Kısayollar:** Launcher'da "Araçlar" ve "Ayarlar" kısayolları doğrudan ilgili
  sekmeyi açar.
- **Cihaz bilgisi:** Model, Android sürümü, güvenlik yaması, çekirdek, ABI, ekran
  çözünürlüğü/Yoğunluk, yenileme hızı, yonga, RAM ve depolama.

## Gizlilik

- Notlar, hatırlatıcılar, sunucular ve ayarlar yalnızca cihazdaki uygulama alanında tutulur.
- Sunucu, hesap veya telemetri yoktur.
- İnternet izni yalnızca ping, hız testi, DNS/port tanılama, sunucu kontrolü ve
  kullanıcı istediğinde dış IP sorgusu için kullanılır; sonuçlar cihazda kalır.
- Dış IP otomatik sorgulanmaz; yalnızca "Dış IP" satırına dokununca bir kez sorgulanır.

## Doğrulama

Android 15 (API 35) x86_64 emülatöründe çalıştırıldı:

- APK kuruldu ve uygulama hatasız açıldı; üç sayfa (PANEL, ARAÇLAR, AYARLAR), kart kaydırma ve alt gezinme kontrol edildi.
- Pil detayları (5.00 V, 900 mA · şarj, 10 mAh, Li-ion, AC adaptör), ağ geçidi/DNS ve sinyal/bağlantı hızı rozetleri doğru göründü.
- DNS sorgusu `cloudflare.com` için 4 kayıt / 44 ms; TCP testi `cloudflare.com:443` için "AÇIK · 24 ms" verdi.
- Hız testi OVH üzerinden 26.9 Mbps (7.6 MB · 2.4 sn) ölçtü.
- Sunucu eklendi; panelde HTTP 200, gecikme ve "SSL 88 gün" göründü; "Tümünü kontrol et" değeri yeniledi.
- Dış IP yalnızca dokununca sorgulandı, panoya kopyalandı ve bildirim gösterildi.
- Canlı sensörler açıldı; ivmeölçer/jiroskop/manyetometre/ışık/yakınlık/basınç/nem/dönüş vektörü değerleri aktı. Emülatörün geçersiz ortam sıcaklığı değeri "—" olarak gösterildi.
- Sessiz saatler anahtarı, başlangıç/bitiş saat seçicisi ve 2/5/10 sn yenileme çipleri çalıştı; seçimler uygulama yeniden başlatıldıktan sonra korundu.
- Yedek `Downloads/yek-panel-yedek.json` olarak dışa aktarıldı; ayarlar değiştirilip geri yüklendi ve "1 not · 1 hatırlatıcı · 1 sunucu" geri geldi.
- Tanılama panoya kopyalandı ve Android paylaşım sayfası açıldı.
- "Araçlar" ve "Ayarlar" kısayol niyetleri hem soğuk hem sıcak başlatmada doğru sekmeyi açtı; uygulama yeniden başlatıldığında not, hatırlatıcı ve sunucu korundu.
- `assembleDebug` ve `lintDebug` hatasız; lint "No issues found."

Bilinen sınırlama: Android 8 ve üzerinde `/proc/stat` uygulamalara kapalıdır; CPU yükü
okunamadığında kart "yük kısıtlı" gösterir, uygulama çökmez. Bazı cihazlar ortam
sıcaklığı/nem gibi sensörler için geçersiz değer döndürür; bu değerler filtrelenir.

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
  `EndpointStore`, `EndpointChecker`, `SpeedTestService`, `NetworkTools`,
  `SettingsBackup`, `NoteStore`, `ReminderStore`, bildirim ve önyükleme alıcıları
- `app/src/main/res/` — tema, renkler, vektör simgeler, kısayollar ve uyarlanabilir
  uygulama simgesi
- `app/src/main/java/com/yekbilgisayar/yekpanel/ui/` — pil halkası, sparkline ve
  doluluk çubuğu çizimleri
