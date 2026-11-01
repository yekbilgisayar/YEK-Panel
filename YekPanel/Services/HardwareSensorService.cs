using LibreHardwareMonitor.Hardware;

namespace YekPanel.Services;

public sealed record TemperatureSample(double? Cpu, double? Gpu, string? Note);

public sealed class HardwareSensorService : IDisposable
{
    private readonly object _gate = new();
    private Computer? _computer;
    private bool _failed;
    private string? _note;

    public TemperatureSample Read()
    {
        lock (_gate)
        {
            if (_failed)
                return new TemperatureSample(null, null, _note);

            try
            {
                _computer ??= CreateComputer();

                double? cpu = null;
                double? gpu = null;

                foreach (var hardware in _computer.Hardware)
                {
                    UpdateRecursive(hardware);

                    if (hardware.HardwareType == HardwareType.Cpu)
                        cpu ??= Pick(hardware, "Package", "Tctl", "Core Max", "CPU");
                    else if (hardware.HardwareType is HardwareType.GpuNvidia or HardwareType.GpuAmd or HardwareType.GpuIntel)
                        gpu ??= Pick(hardware, "GPU Core", "Core", "GPU");
                }

                if (cpu is null && gpu is null)
                {
                    _note = "Sensör okunamadı; yönetici izni gerekebilir";
                    return new TemperatureSample(null, null, _note);
                }

                _note = null;
                return new TemperatureSample(cpu, gpu, null);
            }
            catch (Exception ex)
            {
                _failed = true;
                _note = ex is UnauthorizedAccessException or System.ComponentModel.Win32Exception
                    ? "Yönetici izni gerekli"
                    : "Sensör başlatılamadı";
                return new TemperatureSample(null, null, _note);
            }
        }
    }

    private static Computer CreateComputer() => new()
    {
        IsCpuEnabled = true,
        IsGpuEnabled = true,
        IsMotherboardEnabled = false,
        IsControllerEnabled = false,
        IsStorageEnabled = false,
        IsMemoryEnabled = false,
        IsNetworkEnabled = false
    };

    private static void UpdateRecursive(IHardware hardware)
    {
        hardware.Update();
        foreach (var sub in hardware.SubHardware)
            UpdateRecursive(sub);
    }

    private static double? Pick(IHardware hardware, params string[] preferredNames)
    {
        var sensors = new List<ISensor>();
        Collect(hardware, sensors);

        var temperatures = sensors
            .Where(s => s.SensorType == SensorType.Temperature && s.Value is > 0 and < 125)
            .ToList();

        foreach (var preferred in preferredNames)
        {
            var match = temperatures.FirstOrDefault(s =>
                s.Name.Contains(preferred, StringComparison.OrdinalIgnoreCase));
            if (match?.Value is float value)
                return Math.Round(value, 0);
        }

        if (temperatures.FirstOrDefault()?.Value is float fallback)
            return Math.Round(fallback, 0);

        return null;
    }

    private static void Collect(IHardware hardware, List<ISensor> sensors)
    {
        sensors.AddRange(hardware.Sensors);
        foreach (var sub in hardware.SubHardware)
            Collect(sub, sensors);
    }

    public void Dispose()
    {
        lock (_gate)
        {
            try
            {
                _computer?.Close();
            }
            catch
            {
            }

            _computer = null;
            _failed = true;
        }
    }
}
