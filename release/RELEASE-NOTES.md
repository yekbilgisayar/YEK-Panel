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
| `YEK-Panel-Android-1.0.0.apk` | Android 8.0+ için test imzalı APK (Play Store sürümü yakında) |
| `SHA256SUMS.txt` | SHA-256 doğrulama değerleri |

## Kurulum

1. `YEK-Panel-Setup-2.3.0.exe` dosyasını çalıştırın.
2. Kurulum yalnızca mevcut kullanıcı için yapılır; yönetici izni gerekmez.
3. İlk açılışta sunucu adresleri boştur; kendi adreslerinizi Ayarlar'dan girin.

Kurulum paketinde kişisel sunucu adresi, MAC adresi, şehir veya geçmiş bulunmaz. Kaldırma işlemi `%AppData%\YekPanel` altındaki ayarlarınıza, geçmişinize ve bildirimlerinize dokunmaz.

## Lisans

MIT Lisansı. Ayrıntılar için `LICENSE` dosyasına bakın.
