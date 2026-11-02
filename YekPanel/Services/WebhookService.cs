using System.Net.Http;
using System.Text;
using System.Text.Json;
using YekPanel.Models;

namespace YekPanel.Services;

/// <summary>Discord webhook ve Telegram bot üzerinden bildirim gönderir.</summary>
public static class WebhookService
{
    private static readonly HttpClient Http = new()
    {
        Timeout = TimeSpan.FromSeconds(8)
    };

    public static bool HasChannel(AppSettings settings) =>
        !string.IsNullOrWhiteSpace(settings.DiscordWebhookUrl) ||
        (!string.IsNullOrWhiteSpace(settings.TelegramBotToken) &&
         !string.IsNullOrWhiteSpace(settings.TelegramChatId));

    public static bool IsConfigured(AppSettings settings) =>
        settings.WebhookEnabled && HasChannel(settings);

    public static async Task<(bool Ok, string Message)> SendAsync(AppSettings settings, string text)
    {
        if (!settings.WebhookEnabled)
            return (false, "Webhook kapalı (Ayarlar > Webhook)");

        var results = new List<string>();
        var delivered = false;

        if (!string.IsNullOrWhiteSpace(settings.DiscordWebhookUrl))
        {
            var (ok, message) = await DiscordAsync(settings.DiscordWebhookUrl, text);
            results.Add($"Discord {(ok ? "✓" : "✗")} {message}".Trim());
            delivered |= ok;
        }

        if (!string.IsNullOrWhiteSpace(settings.TelegramBotToken) &&
            !string.IsNullOrWhiteSpace(settings.TelegramChatId))
        {
            var (ok, message) = await TelegramAsync(
                settings.TelegramBotToken, settings.TelegramChatId, text);
            results.Add($"Telegram {(ok ? "✓" : "✗")} {message}".Trim());
            delivered |= ok;
        }

        if (results.Count == 0)
            return (false, "Discord adresi veya Telegram bilgileri girilmedi");

        return (delivered, string.Join(" · ", results));
    }

    private static async Task<(bool Ok, string Message)> DiscordAsync(string url, string text)
    {
        if (!Uri.TryCreate(url, UriKind.Absolute, out var uri) || uri.Scheme != Uri.UriSchemeHttps)
            return (false, "adres https:// olmalı");

        try
        {
            var body = JsonSerializer.Serialize(new
            {
                content = text.Length > 1900 ? text[..1900] : text,
                allowed_mentions = new { parse = Array.Empty<string>() }
            });

            using var content = new StringContent(body, Encoding.UTF8, "application/json");
            using var response = await Http.PostAsync(uri, content);
            return response.IsSuccessStatusCode
                ? (true, "")
                : (false, $"HTTP {(int)response.StatusCode}");
        }
        catch (Exception ex)
        {
            AppLog.Error("Webhook.Discord", ex);
            return (false, "gönderilemedi");
        }
    }

    private static async Task<(bool Ok, string Message)> TelegramAsync(
        string token, string chatId, string text)
    {
        try
        {
            var url = $"https://api.telegram.org/bot{token}/sendMessage";
            using var content = new FormUrlEncodedContent(new Dictionary<string, string>
            {
                ["chat_id"] = chatId,
                ["text"] = text.Length > 3900 ? text[..3900] : text,
                ["disable_web_page_preview"] = "true"
            });

            using var response = await Http.PostAsync(url, content);
            return response.IsSuccessStatusCode
                ? (true, "")
                : (false, $"HTTP {(int)response.StatusCode}");
        }
        catch (Exception ex)
        {
            AppLog.Error("Webhook.Telegram", ex);
            return (false, "gönderilemedi");
        }
    }
}
