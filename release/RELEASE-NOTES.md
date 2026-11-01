# YEK Panel 2.2.0

Windows 10/11 için sistem ve sunucu izleme paneli.

## Bu sürümde

- **Tek örnek koruması:** YEK Panel zaten açıkken ikinci kez başlatılırsa yeni pencere açılmaz; mevcut pencere öne getirilir (tepside gizliyse görünür olur).
- **Güncelleme kontrolü:** GitHub Releases üzerinden en son sürüm 24 saatte bir kontrol edilir. Yeni sürüm varsa alt çubukta indirme simgesi belirir ve tepsi bildirimi gösterilir (sessiz saatlerde gösterilmez).
- **Ayarlar > HAKKINDA & GÜNCELLEME:** sürüm numarası, elle kontrol, sürüm notları bağlantısı, "bu sürümü atla / atlamayı geri al", hata günlüğü ve açılışta kontrol seçeneği.
- **Hata günlüğü:** beklenmeyen hatalar `%AppData%\YekPanel\crash.log` dosyasına yazılır; 256 KB'a ulaşınca `crash.old.log` olarak döndürülür.
- **Dayanıklılık:** arayüz veya arka plan görevinde hata oluşursa uygulama kapanmaz; uyarı gösterip çalışmaya devam eder.
- **Dinamik sürüm:** sürüm numarası tek kaynaktan (proje dosyası) gelir; başlık çubuğu, Araç Merkezi ve Hakkında bölümü otomatik güncellenir.
- Tepsi menüsüne "Güncelleme kontrol et" eklendi.

Önceki sürümlerden gelenler: ana ekran bağlantı kartı, 7/30 günlük raporlar, kesintiler, bakım modu, SSL/TCP izleme, araç merkezi, mini mod, tema renkleri ve kurulum sihirbazı. Tam liste `OZELLIKLER.txt` içinde (116 özellik).

## Dosyalar

| Dosya | Açıklama |
| --- | --- |
| `YEK-Panel-Setup-2.2.0.exe` | Kurulum sihirbazı (önerilen) |
| `YEK-Panel-2.2.0-portable.exe` | Kurulum gerektirmeyen taşınabilir sürüm |
| `YEK-Panel-2.2.0-kaynak.zip` | Açık kaynak kod, dokümanlar ve kurulum betiği |
| `SHA256SUMS.txt` | SHA-256 doğrulama değerleri |

## Kurulum

1. `YEK-Panel-Setup-2.2.0.exe` dosyasını çalıştırın.
2. Kurulum yalnızca mevcut kullanıcı için yapılır; yönetici izni gerekmez.
3. İlk açılışta sunucu adresleri boştur; kendi adreslerinizi Ayarlar'dan girin.

Kurulum paketinde kişisel sunucu adresi, MAC adresi, şehir veya geçmiş bulunmaz. Kaldırma işlemi `%AppData%\YekPanel` altındaki ayarlarınıza, geçmişinize ve bildirimlerinize dokunmaz.

## Lisans

MIT Lisansı. Ayrıntılar için `LICENSE` dosyasına bakın.

## Doğrulama

- Tek örnek: ikinci başlatmada süreç sayısı 1 kaldı ve mevcut pencere öne geldi.
- Güncelleme: yerel sahte GitHub feed'i ile v2.3.0 algılandı; alt çubuk rozeti, sürüm notları, atlama ve atlamayı geri alma test edildi.
- 24 saat önbelleği: aynı gün içindeki sonraki açılışlarda feed'e yeni istek yapılmadı.
- Güncelleme kontrolü kapatıldığında hiçbir ağ isteği yapılmadı.
- Hata günlüğü dosyası ve 256 KB döndürme davranışı test edildi.
- Ayarlar, eski `settings.json` ile uyumlu; mevcut kullanıcı verileri korunur.
- Arayüz UIAutomation ve ekran görüntüleriyle doğrulandı.
