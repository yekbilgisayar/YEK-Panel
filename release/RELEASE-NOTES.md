# YEK Panel 2.1.0

Windows 10/11 için sistem ve sunucu izleme paneli.

## Bu sürümde

- Ana ekrana **SİSTEM & BAĞLANTI** kartı eklendi: pil, bağlantı türü/hızı, yerel IP, ağ geçidi, DNS, Wi-Fi ve genel IP.
- Kartta tüm diskler doluluk çubuklarıyla listelenir.
- Yerel IP ve genel IP tek tıkla panoya kopyalanır.
- Kart 30 saniyede bir arka planda yenilenir; arayüz donmaz.
- Genel IP 10 dakikada bir tazelenir.
- Yerel veya genel IP değiştiğinde tepsi bildirimi gösterilir (sessiz saatlerde gösterilmez).
- Araç merkezine "Ağ ayarları" kısayolu eklendi.
- Araç merkezindeki bilgi toplama artık arka planda çalışır.

Önceki sürümden gelenler: 100 özellikli Araç Merkezi, 7/30 günlük raporlar, kesintiler, bakım modu, SSL/TCP izleme, mini mod, tema renkleri ve kurulum sihirbazı. Tam liste `OZELLIKLER.txt` içinde (106 özellik).

## Dosyalar

| Dosya | Açıklama |
| --- | --- |
| `YEK-Panel-Setup-2.1.0.exe` | Kurulum sihirbazı (önerilen) |
| `YEK-Panel-2.1.0-portable.exe` | Kurulum gerektirmeyen taşınabilir sürüm |
| `YEK-Panel-2.1.0-kaynak.zip` | Açık kaynak kod, dokümanlar ve kurulum betiği |
| `SHA256SUMS.txt` | SHA-256 doğrulama değerleri |

## Kurulum

1. `YEK-Panel-Setup-2.1.0.exe` dosyasını çalıştırın.
2. Kurulum yalnızca mevcut kullanıcı için yapılır; yönetici izni gerekmez.
3. İlk açılışta sunucu adresleri boştur; kendi adreslerinizi Ayarlar'dan girin.

Kurulum paketinde kişisel sunucu adresi, MAC adresi, şehir veya geçmiş bulunmaz. Kaldırma işlemi `%AppData%\YekPanel` altındaki ayarlarınıza, geçmişinize ve bildirimlerinize dokunmaz.

## Lisans

MIT Lisansı. Ayrıntılar için `LICENSE` dosyasına bakın.

## Doğrulama

- v2.1 ana ekran kartı gerçek ağ bilgileriyle çalıştırılıp ekran görüntüsüyle doğrulandı.
- Kartın 30 saniyelik otomatik yenilemesi ve genel IP tazelemesi test edildi.
- Kurulum ve kaldırma sessiz modda test edildi.
- Kurulum sonrası ilk açılışta sunucu alanlarının boş geldiği doğrulandı.
- Kaldırma sonrası `%AppData%\YekPanel` kullanıcı verilerinin korunduğu doğrulandı.
