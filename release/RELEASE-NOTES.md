# YEK Panel 2.3.0

Windows 10/11 için sistem ve sunucu izleme paneli.

## Bu sürümde

- **İşlem yönetimi:** Ana ekranda en yoğun 5 işlem listelenir; her satırdaki kapat düğmesiyle süreç sonlandırılır. Sistem süreçleri, kendi süreci ve erişim izni olmayan süreçler korunur; sonuç durum çubuğunda ve bildirimlerde gösterilir.
- **GPU izleme:** NVIDIA/AMD/Intel GPU yükü (%) ve VRAM kullanımı (kullanılan/toplam) canlı gösterilir. Donanım sensörleri LibreHardwareMonitor ile okunur.
- **Eşik uyarıları:** CPU sıcaklığı (50–110 °C) ve RAM kullanımı (50–100 %) için ayrı uyarılar; histerezis, 15 dakika bekleme, bildirim geçmişi kaydı, isteğe bağlı ses ve webhook gönderimi.
- **Betik merkezi:** PowerShell, Python, BAT, CMD, PS1 ve EXE dosyalarını argümanla arka planda çalıştırma; listeleme, silme ve Ctrl+Alt+1–9 hızlı başlatma.
- **Zamanlanmış görevler:** Günün belirli saatinde betik, ping, uygulama kapatma veya bildirim çalıştırma; görev bazında etkinleştirme ve son çalışma tarihi.
- **Webhook:** Discord ve Telegram için ayrı yapılandırma, test düğmesi; sunucu, eşik ve işlem bildirimleri aynı kanala düşer.
- **Ağ araçları:** Etkin TCP bağlantıları (uygulama, uzak IP, port, durum) ve tek tıkla DNS değiştirme (Cloudflare, Google, OpenDNS, otomatik). DNS değişikliği yönetici izni ister.
- **Pano geçmişi:** Son 15 metin (en çok 5.000 karakter) bellekte tutulur; tek tıkla panoya geri kopyalanır ve istenirse temizlenir.
- **Notlar:** Birden çok not ekleme, düzenleme, silme; TXT veya Markdown olarak UTF-8 dışa aktarma.
- **Ses cihazları:** Windows varsayılan ses çıkışı ve mikrofonu tek tıkla değiştirme.
- **Kart düzeni:** Ana ekran kartlarını yukarı/aşağı taşıma, gizleme ve düzeni kalıcı saklama.
- **Araç Merkezi sekmeleri:** GENEL, OTOMASYON, AĞ ve ARAÇLAR sekmeleri; her sekme bağımsız kaydırılır.

## Android 1.2.0

Android uygulaması artık aynı yerel ağdaki Windows bilgisayara bağlanır ve GitHub
üzerinden kendini güncelleyebilir. Masaüstü YEK Panel uygulamasında değişiklik yoktur;
PC köprüsü ayrı bir araçtır ve mevcut ayarlarınıza dokunmaz.

- **PC bağlantısı:** Windows'ta çalışan YEK Panel PC Köprüsü'ne 6 haneli PIN ile bağlanma. Bilgisayar adı, Windows sürümü, çalışma süresi, yerel IP'ler, CPU/RAM/disk doluluğu, ağ ve bağlantı hızı, pil/sıcaklık ve en çok bellek kullanan 5 işlem canlı görünür.
- **Keşif ve otomatik bağlanma:** "PC'Yİ BUL" aynı ağdaki köprüyü UDP ile bulur; PC sekmesinde otomatik bağlanma isteğe bağlıdır.
- **Güvenlik:** PIN zorunlu; aynı IP'den 5 hatalı denemede 1 dakika kilit. Sanal, VPN, Tailscale, VMware ve APIPA adresleri listeden gizlenir.
- **Otomatik güncelleme:** GitHub Releases içindeki `android-*` sürümleri günlük denetlenir. Yeni APK indirilir; sürüm notları gösterilir ve kurulum Android'in kendi onay ekranıyla yapılır. Sessiz kurulum yoktur.
- **Arayüz:** Dört sekme (PANEL, PC, ARAÇLAR, AYARLAR); PC kısayolu eklendi.
- **Yedekleme:** PC bağlantı bilgileri ve güncelleme tercihi JSON yedeğine dahil edildi.
- Sürüm: 1.2.0 (versionCode 3); Android 8.0–15. APK test imzalıdır.

Android 1.2.0 doğrulaması: PC köprüsü Windows 11'de `/api/ping`, PIN'li `/api/status` ve UDP keşif ile doğrulandı. Telefonda CPU, RAM, disk, ağ, pil ve işlem listesi canlı görüntülendi; hatalı PIN ve 1 dakikalık kilit denendi. Sahte GitHub akışıyla 1.3.0 güncellemesi algılandı, APK indirildi, bilinmeyen kaynak izni verildi ve Android kurulum onayıyla kurulum tamamlandı. Test verileri temizlendi. Güncelleme denetimindeki ağ/DNS hataları Türkçe gösterilir; emülatörde internet olmadığı için gerçek GitHub akışı canlı doğrulanamadı. `assembleDebug` ve `lintDebug` hatasız; lint "No issues found."

## Android 1.1.0

Android uygulamasına, masaüstü sürümden bağımsız yeni izleme ve tanılama özellikleri eklendi. Windows uygulamasında değişiklik yoktur.

- **Pil detayları:** Voltaj, akım, şarj sayacı, teknoloji ve güç kaynağı panelde.
- **Ağ detayları:** Sinyal gücü (dBm), bağlantı hızı, ağ geçidi ve DNS; istenince sorgulanan ve panoya kopyalanan dış IP.
- **Sunucu izleme:** En fazla 8 adres; HTTP durum kodu, gecikme ve SSL kalan gün; 60 saniyede bir otomatik kontrol ve "Tümünü kontrol et".
- **Hız testi:** OVH, Cloudflare ve Hetzner üzerinden yaklaşık 8 MB indirme; Mbps/MB/süre/sunucu.
- **Ağ tanılama:** DNS kayıtları ve çözümleme süresi; TCP port testi.
- **Canlı sensörler:** İvmeölçer, jiroskop, manyetometre, ışık, yakınlık, basınç, nem, ortam sıcaklığı, adım sayacı ve dönüş vektörü; yalnızca ARAÇLAR sekmesi açıkken okunur.
- **Sessiz saatler ve yenileme:** Gece aralığında pil/sıcaklık bildirimlerini susturma; 2/5/10 saniye yenileme seçimi.
- **Yedekleme:** Ayarlar, notlar, hatırlatıcılar ve sunucular tek JSON dosyasına yedeklenip geri yüklenebilir.
- **Tanılama ve kısayollar:** Cihaz özetini kopyalama/paylaşma; launcher'da "Araçlar" ve "Ayarlar" kısayolları.
- **Cihaz bilgisi:** Güvenlik yaması, çekirdek, ABI, ekran, yenileme hızı, yonga, RAM ve depolama.
- Sürüm: 1.1.0 (versionCode 2); Android 8.0–15. APK test imzalıdır.

Android 1.1.0 doğrulaması: DNS (4 kayıt / 44 ms), TCP 443 (açık / 24 ms), hız testi (26.9 Mbps), sunucu kontrolü (HTTP 200, SSL 88 gün), dış IP, canlı sensörler, sessiz saatler, yenileme seçimi, yedek dışa/içe aktarma, tanılama paylaşımı ve kısayol sekmeleri emülatörde çalıştı. `assembleDebug` ve `lintDebug` hatasız.

## Android 1.0.0

YEK Panel artık Android'de de var. Aynı depodan indirilebilen APK; Windows sürümünden bağımsız, kendi sürüm numarasıyla yayınlanır.

- Pil yüzdesi, sıcaklık, voltaj, sağlık ve şarj durumu; CPU yükü ve frekansı; RAM, depolama ve ağ kartları.
- Pil/sıcaklık eşik uyarıları, 30 dakika bekleme ve bildirim kanalı.
- ICMP ping testi (izin yoksa TCP:53 ölçümü), notlar ve TXT/Markdown dışa aktarma, günlük hatırlatıcılar.
- Android 8.0 (API 26) – Android 15 (API 35); harici UI kütüphanesi yok.
- APK, Android 15 emülatöründe kuruldu; panel, ping (3/3), not kaydetme/dışa aktarma, hatırlatıcı ve ayarlar çalışır durumda doğrulandı. APK test imzasıyla üretilmiştir; mağaza yayını için imzalı AAB gerekir.
- **Google Play Store sürümü yakında.**

## Doğrulama

- Proje 0 uyarı, 0 hata ile derlendi.
- GPU/VRAM: LibreHardwareMonitor oturumu açıldıktan sonra RTX 5060 ve Intel UHD sensörleri okundu; GPU yükü ve VRAM değerleri arayüzde göründü.
- Araç sekmeleri, DNS mevcut değeri, bağlantı listesi, pano yakalama, ses cihazları ve notlar UIAutomation ve ekran görüntüleriyle kontrol edildi.
- Ana ekranda 5 işlem satırı, kapat düğmeleri ve GPU satırları görsel olarak doğrulandı.
- Ayarlar geriye uyumlu: v2.2 `settings.json` dosyası yeni alanlarla otomatik tamamlanır; mevcut kullanıcı verileri korunur.
- CPU sıcaklığı için yönetici izni gerekebilir; bu durumda sıcaklık satırı "yönetici gerekli" ipucu gösterir.

## Dosyalar

| Dosya | Açıklama |
| --- | --- |
| `YEK-Panel-Setup-2.3.0.exe` | Kurulum sihirbazı (önerilen) |
| `YEK-Panel-2.3.0-portable.exe` | Kurulum gerektirmeyen taşınabilir sürüm |
| `YEK-Panel-2.3.0-kaynak.zip` | Açık kaynak kod, dokümanlar ve kurulum betiği |
| `YEK-Panel-Android-1.2.0.apk` | Android 8.0+ için test imzalı APK (PC bağlantısı + otomatik güncelleme; Play Store sürümü yakında) |
| `YEK-Panel-PC-Koprusu-1.0.0.zip` | Windows 10/11 PC köprüsü (PowerShell + başlatıcı + README) |
| `SHA256SUMS.txt` | SHA-256 doğrulama değerleri |

## Kurulum

1. `YEK-Panel-Setup-2.3.0.exe` dosyasını çalıştırın.
2. Kurulum yalnızca mevcut kullanıcı için yapılır; yönetici izni gerekmez.
3. İlk açılışta sunucu adresleri boştur; kendi adreslerinizi Ayarlar'dan girin.

Kurulum paketinde kişisel sunucu adresi, MAC adresi, şehir veya geçmiş bulunmaz. Kaldırma işlemi `%AppData%\YekPanel` altındaki ayarlarınıza, geçmişinize ve bildirimlerinize dokunmaz.

## Lisans

MIT Lisansı. Ayrıntılar için `LICENSE` dosyasına bakın.
