using System.Runtime.InteropServices;

namespace YekPanel.Services;

public sealed record AudioDevice(string Id, string Name, bool IsDefault);

/// <summary>Windows varsayılan ses çıkışı/girişini Core Audio üzerinden listeler ve değiştirir.</summary>
public static class AudioService
{
    private const int DeviceStateActive = 1;
    private const int StgmRead = 0;

    private enum EDataFlow
    {
        Render = 0,
        Capture = 1,
        All = 2
    }

    private enum ERole
    {
        Console = 0,
        Multimedia = 1,
        Communications = 2
    }

    public static IReadOnlyList<AudioDevice> GetOutputs() => GetDevices(EDataFlow.Render);

    public static IReadOnlyList<AudioDevice> GetInputs() => GetDevices(EDataFlow.Capture);

    public static (bool Ok, string Message) SetDefault(string deviceId, bool output)
    {
        if (string.IsNullOrWhiteSpace(deviceId))
            return (false, "Cihaz seçilmedi");

        IPolicyConfig? policy = null;
        try
        {
            policy = (IPolicyConfig)new CPolicyConfigClient();
            foreach (var role in new[] { ERole.Console, ERole.Multimedia, ERole.Communications })
            {
                var result = policy.SetDefaultEndpoint(deviceId, role);
                if (result != 0)
                    return (false, $"Windows ayarı reddetti (0x{result:X8})");
            }

            return (true, output ? "Varsayılan çıkış değiştirildi" : "Varsayılan giriş değiştirildi");
        }
        catch (Exception ex)
        {
            AppLog.Error("AudioService.SetDefault", ex);
            return (false, "Ses cihazı değiştirilemedi; Windows sürümü desteklemiyor olabilir");
        }
        finally
        {
            if (policy != null)
                Marshal.ReleaseComObject(policy);
        }
    }

    private static IReadOnlyList<AudioDevice> GetDevices(EDataFlow flow)
    {
        var result = new List<AudioDevice>();
        IMMDeviceEnumerator? enumerator = null;
        IMMDeviceCollection? collection = null;

        try
        {
            enumerator = (IMMDeviceEnumerator)new MMDeviceEnumeratorComObject();

            string? defaultId = null;
            IMMDevice? defaultDevice = null;
            try
            {
                if (enumerator.GetDefaultAudioEndpoint(flow, ERole.Console, out defaultDevice) == 0)
                    defaultDevice.GetId(out defaultId);
            }
            catch
            {
            }
            finally
            {
                if (defaultDevice != null)
                    Marshal.ReleaseComObject(defaultDevice);
            }

            if (enumerator.EnumAudioEndpoints(flow, DeviceStateActive, out collection) != 0 || collection == null)
                return result;

            collection.GetCount(out var count);
            for (var i = 0; i < count; i++)
            {
                IMMDevice? device = null;
                IPropertyStore? store = null;
                try
                {
                    if (collection.Item(i, out device) != 0 || device == null)
                        continue;

                    device.GetId(out var id);
                    var name = "Ses cihazı";

                    if (device.OpenPropertyStore(StgmRead, out store) == 0 && store != null)
                    {
                        var key = PkeyDeviceFriendlyName;
                        if (store.GetValue(ref key, out var value) == 0)
                        {
                            try
                            {
                                if (value.Vt == 31 && value.Pointer != IntPtr.Zero)
                                    name = Marshal.PtrToStringUni(value.Pointer) ?? name;
                            }
                            finally
                            {
                                PropVariantClear(ref value);
                            }
                        }
                    }

                    result.Add(new AudioDevice(id ?? "", name, id == defaultId));
                }
                catch
                {
                }
                finally
                {
                    if (store != null)
                        Marshal.ReleaseComObject(store);
                    if (device != null)
                        Marshal.ReleaseComObject(device);
                }
            }
        }
        catch (Exception ex)
        {
            AppLog.Error("AudioService.GetDevices", ex);
        }
        finally
        {
            if (collection != null)
                Marshal.ReleaseComObject(collection);
            if (enumerator != null)
                Marshal.ReleaseComObject(enumerator);
        }

        return result
            .OrderByDescending(device => device.IsDefault)
            .ThenBy(device => device.Name, StringComparer.CurrentCultureIgnoreCase)
            .ToList();
    }

    private static PROPERTYKEY PkeyDeviceFriendlyName => new()
    {
        FormatId = new Guid("a45c254e-df1c-4efd-8020-67d146a850e0"),
        PropertyId = 14
    };

    [StructLayout(LayoutKind.Sequential)]
    private struct PROPERTYKEY
    {
        public Guid FormatId;
        public int PropertyId;
    }

    [StructLayout(LayoutKind.Sequential)]
    private struct PropVariant
    {
        public ushort Vt;
        public ushort Reserved1;
        public ushort Reserved2;
        public ushort Reserved3;
        public IntPtr Pointer;
        public IntPtr Pointer2;
    }

    [DllImport("ole32.dll")]
    private static extern int PropVariantClear(ref PropVariant value);

    [ComImport]
    [Guid("BCDE0395-E52F-467C-8E3D-C4579291692E")]
    private class MMDeviceEnumeratorComObject
    {
    }

    [ComImport]
    [Guid("870AF99C-171D-4F9E-AF0D-E63DF40C2BC9")]
    private class CPolicyConfigClient
    {
    }

    [ComImport]
    [Guid("A95664D2-9614-4F35-A746-DE8DB63617E6")]
    [InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
    private interface IMMDeviceEnumerator
    {
        [PreserveSig]
        int EnumAudioEndpoints(EDataFlow dataFlow, int stateMask, out IMMDeviceCollection devices);

        [PreserveSig]
        int GetDefaultAudioEndpoint(EDataFlow dataFlow, ERole role, out IMMDevice endpoint);

        [PreserveSig]
        int GetDevice([MarshalAs(UnmanagedType.LPWStr)] string id, out IMMDevice device);

        [PreserveSig]
        int RegisterEndpointNotificationCallback(IntPtr client);

        [PreserveSig]
        int UnregisterEndpointNotificationCallback(IntPtr client);
    }

    [ComImport]
    [Guid("0BD7A1BE-7A1A-44DB-8397-CC5392387B5E")]
    [InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
    private interface IMMDeviceCollection
    {
        [PreserveSig]
        int GetCount(out int count);

        [PreserveSig]
        int Item(int index, out IMMDevice device);
    }

    [ComImport]
    [Guid("D666063F-1587-4E43-81F1-B948E807363F")]
    [InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
    private interface IMMDevice
    {
        [PreserveSig]
        int Activate(ref Guid iid, int clsCtx, IntPtr activationParams,
            [MarshalAs(UnmanagedType.IUnknown)] out object iface);

        [PreserveSig]
        int OpenPropertyStore(int access, out IPropertyStore properties);

        [PreserveSig]
        int GetId([MarshalAs(UnmanagedType.LPWStr)] out string id);

        [PreserveSig]
        int GetState(out int state);
    }

    [ComImport]
    [Guid("886D8EEB-8CF2-4446-8D02-CDBA1DBDCF99")]
    [InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
    private interface IPropertyStore
    {
        [PreserveSig]
        int GetCount(out uint count);

        [PreserveSig]
        int GetAt(uint index, out PROPERTYKEY key);

        [PreserveSig]
        int GetValue(ref PROPERTYKEY key, out PropVariant value);

        [PreserveSig]
        int SetValue(ref PROPERTYKEY key, ref PropVariant value);

        [PreserveSig]
        int Commit();
    }

    [ComImport]
    [Guid("F8679F50-850A-41CF-9C72-430F290290C8")]
    [InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
    private interface IPolicyConfig
    {
        [PreserveSig]
        int GetMixFormat([MarshalAs(UnmanagedType.LPWStr)] string deviceName, IntPtr format);

        [PreserveSig]
        int GetDeviceFormat([MarshalAs(UnmanagedType.LPWStr)] string deviceName, bool isDefault, IntPtr format);

        [PreserveSig]
        int ResetDeviceFormat([MarshalAs(UnmanagedType.LPWStr)] string deviceName);

        [PreserveSig]
        int SetDeviceFormat([MarshalAs(UnmanagedType.LPWStr)] string deviceName, IntPtr endpointFormat, IntPtr mixFormat);

        [PreserveSig]
        int GetProcessingPeriod([MarshalAs(UnmanagedType.LPWStr)] string deviceName, bool isDefault,
            IntPtr defaultPeriod, IntPtr minimumPeriod);

        [PreserveSig]
        int SetProcessingPeriod([MarshalAs(UnmanagedType.LPWStr)] string deviceName, IntPtr period);

        [PreserveSig]
        int GetShareMode([MarshalAs(UnmanagedType.LPWStr)] string deviceName, IntPtr mode);

        [PreserveSig]
        int SetShareMode([MarshalAs(UnmanagedType.LPWStr)] string deviceName, IntPtr mode);

        [PreserveSig]
        int GetPropertyValue([MarshalAs(UnmanagedType.LPWStr)] string deviceName, bool fxStore,
            IntPtr key, IntPtr value);

        [PreserveSig]
        int SetPropertyValue([MarshalAs(UnmanagedType.LPWStr)] string deviceName, bool fxStore,
            IntPtr key, IntPtr value);

        [PreserveSig]
        int SetDefaultEndpoint([MarshalAs(UnmanagedType.LPWStr)] string deviceName, ERole role);

        [PreserveSig]
        int SetEndpointVisibility([MarshalAs(UnmanagedType.LPWStr)] string deviceName, bool visible);
    }
}
