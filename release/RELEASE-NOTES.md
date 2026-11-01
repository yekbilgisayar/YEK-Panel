# YEK Panel 2.0.0

Windows 10/11 için sistem ve sunucu izleme paneli.

## Bu sürümde

- 100 özellik: 38'i yeni, 62'si önceki sürümlerden.
- Araç Merkezi: pil, ağ/DNS, dış IP, diskler, ping, port testi, kronometre, geri sayım ve not.
- 7/30 günlük rapor, kesinti listesi ve CSV dışa aktarma.
- Sunucu bazında bakım modu, yavaş yanıt eşiği ve SSL bitiş uyarısı.
- Opaklık, 12/24 saat, °C/°F, gizli başlatma, mini mod hatırlama ve günlük özet.
- Kurulum sihirbazı: kullanıcı bazlı kurulum, masaüstü/Başlat menüsü kısayolları ve kaldırma kaydı.

## Dosyalar

| Dosya | Açıklama |
| --- | --- |
| `YEK-Panel-Setup-2.0.0.exe` | Kurulum sihirbazı (önerilen) |
| `YEK-Panel-2.0.0-portable.exe` | Kurulum gerektirmeyen taşınabilir sürüm |
| `YEK-Panel-2.0.0-kaynak.zip` | Açık kaynak kod, dokümanlar ve kurulum betiği |
| `SHA256SUMS.txt` | SHA-256 doğrulama değerleri |

## Kurulum

1. `YEK-Panel-Setup-2.0.0.exe` dosyasını çalıştırın.
2. Kurulum yalnızca mevcut kullanıcı için yapılır; yönetici izni gerekmez.
3. İlk açılışta sunucu adresleri boştur; kendi adreslerinizi Ayarlar'dan girin.

Kurulum paketinde kişisel sunucu adresi, MAC adresi, şehir veya geçmiş bulunmaz. Kaldırırken verilerinizi silmek isteyip istemediğiniz sorulur; varsayılan seçenek korur.

## Lisans

MIT Lisansı. Ayrıntılar için `LICENSE` dosyasına bakın.

## Doğrulama

- Kurulum ve kaldırma sessiz modda test edildi.
- Kurulum sonrası ilk açılışta sunucu alanlarının boş geldiği doğrulandı.
- Kaldırma sonrası `%AppData%\YekPanel` kullanıcı verilerinin korunduğu doğrulandı.
