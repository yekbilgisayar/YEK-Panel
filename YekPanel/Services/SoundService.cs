using System.Media;

namespace YekPanel.Services;

/// <summary>Sunucu ve donanım olaylarında kısa Windows sesleri.</summary>
public static class SoundService
{
    public static void PlayWarning()
    {
        try
        {
            SystemSounds.Exclamation.Play();
        }
        catch
        {
        }
    }

    public static void PlayRecovery()
    {
        try
        {
            SystemSounds.Asterisk.Play();
        }
        catch
        {
        }
    }
}
