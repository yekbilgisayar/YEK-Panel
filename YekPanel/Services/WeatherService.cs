using System.Globalization;
using System.Net.Http;
using System.Text.Json;

namespace YekPanel.Services;

public sealed class WeatherInfo
{
    public string City { get; init; } = "";
    public string? Ip { get; init; }
    public double Temp { get; init; }
    public double FeelsLike { get; init; }
    public int Humidity { get; init; }
    public double Wind { get; init; }
    public double Min { get; init; }
    public double Max { get; init; }
    public double TomorrowMin { get; init; }
    public double TomorrowMax { get; init; }
    public string TomorrowDescription { get; init; } = "";
    public double DayAfterMin { get; init; }
    public double DayAfterMax { get; init; }
    public string DayAfterDescription { get; init; } = "";
    public string Description { get; init; } = "";
    public string Glyph { get; init; } = "";
}

public static class WeatherService
{
    public static async Task<WeatherInfo?> FetchAsync(HttpClient http, string? city, CancellationToken ct)
    {
        try
        {
            double latitude;
            double longitude;
            string cityName;
            string? ip = null;

            if (!string.IsNullOrWhiteSpace(city))
            {
                var geoUrl = "https://geocoding-api.open-meteo.com/v1/search?name=" +
                             Uri.EscapeDataString(city.Trim()) + "&count=1&language=tr&format=json";
                using var geoDoc = JsonDocument.Parse(await http.GetStringAsync(geoUrl, ct));
                var root = geoDoc.RootElement;

                if (!root.TryGetProperty("results", out var results) || results.GetArrayLength() == 0)
                    return null;

                var place = results[0];
                latitude = place.GetProperty("latitude").GetDouble();
                longitude = place.GetProperty("longitude").GetDouble();
                cityName = place.TryGetProperty("name", out var name) ? name.GetString() ?? city : city;
            }
            else
            {
                using var doc = JsonDocument.Parse(await http.GetStringAsync("https://ipwho.is/", ct));
                var root = doc.RootElement;

                if (!root.TryGetProperty("success", out var success) || !success.GetBoolean())
                    return null;

                latitude = root.GetProperty("latitude").GetDouble();
                longitude = root.GetProperty("longitude").GetDouble();
                cityName = root.TryGetProperty("city", out var cityEl) ? cityEl.GetString() ?? "" : "";
                if (root.TryGetProperty("ip", out var ipEl))
                    ip = ipEl.GetString();
            }

            var lat = latitude.ToString("0.####", CultureInfo.InvariantCulture);
            var lon = longitude.ToString("0.####", CultureInfo.InvariantCulture);
            var forecastUrl =
                $"https://api.open-meteo.com/v1/forecast?latitude={lat}&longitude={lon}" +
                "&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,weather_code,wind_speed_10m" +
                "&daily=weather_code,temperature_2m_min,temperature_2m_max&timezone=auto&forecast_days=3";

            using var forecastDoc = JsonDocument.Parse(await http.GetStringAsync(forecastUrl, ct));
            var current = forecastDoc.RootElement.GetProperty("current");
            var daily = forecastDoc.RootElement.GetProperty("daily");

            var code = current.GetProperty("weather_code").GetInt32();
            var isDay = current.GetProperty("is_day").GetInt32() == 1;
            var (text, glyph) = Describe(code, isDay);

            var minArray = daily.GetProperty("temperature_2m_min");
            var maxArray = daily.GetProperty("temperature_2m_max");
            var codeArray = daily.GetProperty("weather_code");

            DayForecast(1, minArray, maxArray, codeArray, out var tomorrowMin, out var tomorrowMax, out var tomorrowDesc);
            DayForecast(2, minArray, maxArray, codeArray, out var dayAfterMin, out var dayAfterMax, out var dayAfterDesc);

            return new WeatherInfo
            {
                City = cityName,
                Ip = ip,
                Temp = current.GetProperty("temperature_2m").GetDouble(),
                FeelsLike = current.GetProperty("apparent_temperature").GetDouble(),
                Humidity = current.GetProperty("relative_humidity_2m").GetInt32(),
                Wind = current.GetProperty("wind_speed_10m").GetDouble(),
                Min = minArray[0].GetDouble(),
                Max = maxArray[0].GetDouble(),
                TomorrowMin = tomorrowMin,
                TomorrowMax = tomorrowMax,
                TomorrowDescription = tomorrowDesc,
                DayAfterMin = dayAfterMin,
                DayAfterMax = dayAfterMax,
                DayAfterDescription = dayAfterDesc,
                Description = text,
                Glyph = glyph
            };
        }
        catch
        {
            return null;
        }
    }

    private static void DayForecast(int index, JsonElement min, JsonElement max, JsonElement codes,
        out double minValue, out double maxValue, out string description)
    {
        minValue = 0;
        maxValue = 0;
        description = "";

        try
        {
            if (min.GetArrayLength() <= index || max.GetArrayLength() <= index || codes.GetArrayLength() <= index)
                return;

            minValue = min[index].GetDouble();
            maxValue = max[index].GetDouble();
            description = Describe(codes[index].GetInt32(), true).Text;
        }
        catch
        {
        }
    }

    private static (string Text, string Glyph) Describe(int code, bool isDay) => code switch
    {
        0 => isDay ? ("Açık", "☀") : ("Açık", "☽"),
        1 => ("Az bulutlu", "⛅"),
        2 => ("Parçalı bulutlu", "⛅"),
        3 => ("Bulutlu", "☁"),
        45 or 48 => ("Sisli", "☁"),
        51 or 53 or 55 => ("Çisenti", "☔"),
        56 or 57 => ("Dondurucu çisenti", "☔"),
        61 or 63 or 65 => ("Yağmurlu", "☔"),
        66 or 67 => ("Dondurucu yağmur", "☔"),
        71 or 73 or 75 => ("Kar yağışlı", "❄"),
        77 => ("Kar taneleri", "❄"),
        80 or 81 or 82 => ("Sağanak", "☔"),
        85 or 86 => ("Kar sağanağı", "❄"),
        95 => ("Fırtına", "⚡"),
        96 or 99 => ("Dolu fırtınası", "⚡"),
        _ => ("Bilinmiyor", "☁")
    };
}
