namespace YekPanel.Models;

/// <summary>Panelden çalıştırılabilen bir betik veya program.</summary>
public sealed class ScriptEntry
{
    public string Name { get; set; } = "";
    public string Path { get; set; } = "";
    public string Arguments { get; set; } = "";
    public bool Enabled { get; set; } = true;
}

/// <summary>Günde bir kez çalışan zamanlanmış görev.</summary>
public sealed class ScheduledTask
{
    public string Name { get; set; } = "";
    public string Time { get; set; } = "09:00";
    public string Action { get; set; } = "notify";
    public string Target { get; set; } = "";
    public bool Enabled { get; set; } = true;
    public string LastRunDate { get; set; } = "";

    public static readonly string[] Actions = { "script", "ping", "kill", "notify" };

    public static string ActionLabel(string action) => action switch
    {
        "script" => "Betik",
        "ping" => "Ping",
        "kill" => "Uygulama kapat",
        _ => "Bildirim"
    };
}
