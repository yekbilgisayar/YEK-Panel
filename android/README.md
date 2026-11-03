# YEK Panel — Android

YEK Panel'in Android sürümü. Windows panelindeki sistem izleme fikrini telefona taşır:
pil, bellek, depolama, CPU ve ağ durumu tek ekranda; ayrıca sunucu izleme, hız testi,
ağ tanılama, canlı sensörler, notlar, hatırlatıcılar ve yedekleme. Yeni sürümde
yerel ağdaki Windows PC'ye bağlanır ve GitHub üzerinden kendini güncelleyebilir.

- Paket: `com.yekbilgisayar.yekpanel`
- Sürüm: `1.2.1` (versionCode 4)
- Min. Android: 8.0 (API 26)
- Hedef: Android 16 (API 36)
- Dil: Java 17, harici UI kütüphanesi yok (framework View'ları)

## Özellikler

- **PC bağlantısı:** Aynı yerel ağdaki Windows bilgisayarda çalışan YEK Panel
  2.3.1 uygulamasına doğrudan bağlantı (ayrı yardımcı gerekmez). Bilgisayarın adı,
  Windows sürümü, çalışma süresi, yerel IP'leri, CPU/RAM/disk doluluğu, ağ hızı,
  bağlantı hızı, pil/sıcaklık ve en çok bellek kullanan 5 işlem canlı izlenir.
  Bağlantı 6 haneli PIN ile korunur; 5 hatalı denemede 1 dakika kilit vardır.
  "PC'Yİ BUL" aynı ağdaki bilgisayarı UDP ile bulur; otomatik bağlanma isteğe bağlıdır.
  Windows'ta **YEK Panel > Ayarlar > PC BAĞLANTISI** bölümünden IP ve PIN görünür.
- **Otomatik güncelleme:** GitHub Releases içindeki `android-*` sürümleri denetlenir;
  yeni APK indirilir ve kurulum için Android'in kendi onay ekranı açılır. Günlük
  otomatik kontrol veya "GÜNCELLEME KONTROL ET" düğmesi; sürüm notları görüntülenir.
  Sessiz kurulum yapılmaz, bilinmeyen kaynak izni gerekir.
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
- **Kısayollar:** Launcher'da "PC", "Araçlar" ve "Ayarlar" kısayolları doğrudan
  ilgili sekmeyi açar (dört sekme: PANEL, PC, ARAÇLAR, AYARLAR).
- **Cihaz bilgisi:** Model, Android sürümü, güvenlik yaması, çekirdek, ABI, ekran
  çözünürlüğü/Yoğunluk, yenileme hızı, yonga, RAM ve depolama.

## Gizlilik

- Notlar, hatırlatıcılar, sunucular, PC bağlantı bilgileri ve ayarlar yalnızca
  cihazdaki uygulama alanında tutulur.
- Sunucu, hesap veya telemetri yoktur.
- İnternet izni yalnızca ping, hız testi, DNS/port tanılama, sunucu kontrolü,
  güncelleme denetimi ve kullanıcı istediğinde dış IP sorgusu için kullanılır;
  sonuçlar cihazda kalır.
- PC bağlantısı yalnızca yerel ağdaki YEK Panel uygulamasına gider; PC verileri
  internete yüklenmez. PIN telefonda saklanır, yalnızca LAN isteğinde gönderilir.
- Güncelleme denetimi GitHub Releases API'sine sürüm sorgusu gönderir; hesap veya
  kişisel veri gönderilmez. APK indirmesi de GitHub üzerinden yapılır.
- Dış IP otomatik sorgulanmaz; yalnızca "Dış IP" satırına dokununca bir kez sorgulanır.
- Hata ayıklama derlemelerinde yerel test akışı için `yek_update_feed` sistem ayarı
  okunabilir; yayın derlemesinde bu yol kapalıdır.

## Doğrulama

Android 16 (API 36) hedefiyle derlendi; Android 15 (API 35) x86_64 emülatöründe çalıştırıldı:

- APK kuruldu ve uygulama hatasız açıldı; dört sekme (PANEL, PC, ARAÇLAR, AYARLAR), kart kaydırma ve alt gezinme kontrol edildi.
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
- Gömülü PC bağlantısı Windows 11'de YEK Panel 2.3.1 içinde doğrulandı; `/api/ping`, PIN'li `/api/status` ve UDP keşif yanıtı alındı. Telefondan bağlantı kuruldu; bilgisayar adı, Windows sürümü, CPU (%21–41), RAM (20/31,6 GB, %63), disk (C: 541/952 GB), ağ hızı, 144 Mbps bağlantı, pil (%82) ve ilk 5 işlem görüntülendi.
- Hatalı PIN "PIN hatalı" olarak gösterildi; 5 hatalı denemeden sonra sunucu HTTP 429 döndürdü ve uygulama "1 dakika bekleyin" mesajını verdi. Doğru PIN ile yeniden bağlanıldı.
- PC IP listesinde yalnızca kullanılabilir yerel adres kaldı; sanal, VPN, Tailscale, VMware ve APIPA adresleri gizlendi.
- Sahte GitHub akışıyla güncelleme akışı uçtan uca denendi: 1.3.0 sürümü algılandı, sürüm notları ve APK boyutu gösterildi, APK indirildi, bilinmeyen kaynak izni verildi, Android kurulum onayı çıktı ve kurulum tamamlandı. Test akışı ve test APK'sı temizlendi.
- `assembleDebug` ve `lintDebug` hatasız; lint "No issues found."

Bilinen sınırlama: Android 8 ve üzerinde `/proc/stat` uygulamalara kapalıdır; CPU yükü
okunamadığında kart "yük kısıtlı" gösterir, uygulama çökmez. Bazı cihazlar ortam
sıcaklığı/nem gibi sensörler için geçersiz değer döndürür; bu değerler filtrelenir.

## Derleme

Gereksinimler: JDK 17+ ve Android SDK 36 (platform + build-tools).

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
8.10.1'dir. Play Store, 31 Ağustos 2026'dan itibaren yeni uygulama ve güncellemelerde
Android 16 (API 36) hedefini zorunlu tutar.

## Play Store

**Google Play Store yüklemesi için imzalı Android App Bundle (AAB) gerekir.**
Yükleme anahtarı depo dışında (kullanıcı profilinde) tutulur; parola komut
satırına yazılmaz. Derleme sırasında `YEK_KEYSTORE_PROPERTIES` ortam değişkeniyle
`keystore.properties` dosyasının yolu verilir.

```bash
# GitHub (sideload) APK: mevcut 1.2.x test anahtarıyla imzalanır, otomatik güncelleme içerir
./gradlew assembleSideloadRelease

# Google Play AAB: yükleme anahtarıyla imzalanır, harici APK izni içermez
export YEK_KEYSTORE_PROPERTIES=/yol/keystore.properties
./gradlew bundlePlayRelease
```

Debug derlemesi yalnızca test içindir; Play Store'a yüklemeyin. Yayın anahtarınızı
depoya eklemeyin; `keystore.properties` veya CI sırrı olarak saklayın.
Play paketinin mağaza metni, görselleri ve yükleme adımları kök dizindeki
`release/play-store/` klasöründedir.

Play Store sürümünde uygulama içi güncellemeler Google Play In-App Updates ile
yapılmalıdır. Bu depodaki GitHub'dan APK indirme akışı, doğrudan APK olarak
kurulan (sideload) sürüm içindir; Play politikası gereği mağaza sürümünde
`REQUEST_INSTALL_PACKAGES` ve harici APK kurulumu kullanılmamalıdır.

## Kaynak yapısı

- `app/src/main/java/com/yekbilgisayar/yekpanel/` — `MainActivity`, `DeviceStats`,
  `EndpointStore`, `EndpointChecker`, `SpeedTestService`, `NetworkTools`,
  `SettingsBackup`, `NoteStore`, `ReminderStore`, `PcClient`, `UpdateChecker`,
  `UpdateFileProvider`, bildirim ve önyükleme alıcıları
- `app/src/main/res/` — tema, renkler, vektör simgeler, kısayollar ve uyarlanabilir
  uygulama simgesi
- `app/src/main/java/com/yekbilgisayar/yekpanel/ui/` — pil halkası, sparkline ve
  doluluk çubuğu çizimleri
