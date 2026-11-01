; YEK Panel kurulum betiği (Inno Setup 6)
; Derlemek için: ISCC.exe YekPanel.iss

#define AppName "YEK Panel"
#define AppVersion "2.0.0"
#define AppPublisher "YEK BİLGİSAYAR"
#define AppExeName "YekPanel.exe"
#define SourceExe "..\YEK-Panel.exe"

[Setup]
AppId={{8F3C2A71-6B4D-4E9A-9C21-7D5E0B8A1F44}
AppName={#AppName}
AppVersion={#AppVersion}
AppVerName={#AppName} {#AppVersion}
AppPublisher={#AppPublisher}
AppCopyright=YEK BİLGİSAYAR
VersionInfoVersion=2.0.0.0
VersionInfoCompany=YEK BİLGİSAYAR
VersionInfoDescription=YEK Panel Kurulum
VersionInfoProductName=YEK Panel
VersionInfoProductVersion=2.0.0
DefaultDirName={localappdata}\Programs\YekPanel
DefaultGroupName={#AppName}
DisableProgramGroupPage=yes
PrivilegesRequired=lowest
OutputDir=..\release
OutputBaseFilename=YEK-Panel-Setup-{#AppVersion}
SetupIconFile=..\YekPanel\Assets\app.ico
UninstallDisplayIcon={app}\{#AppExeName}
UninstallDisplayName={#AppName} {#AppVersion}
Compression=lzma2/max
SolidCompression=yes
WizardStyle=modern
WizardImageFile=wizard.bmp
WizardImageStretch=yes
ArchitecturesAllowed=x64compatible
MinVersion=10.0
CloseApplications=yes
RestartApplications=no
SetupLogging=yes
AllowNoIcons=yes
DisableWelcomePage=no

[Languages]
Name: "turkish"; MessagesFile: "compiler:Languages\Turkish.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: checkedonce

[Files]
Source: "{#SourceExe}"; DestDir: "{app}"; DestName: "{#AppExeName}"; Flags: ignoreversion
Source: "..\KURULUM.txt"; DestDir: "{app}"; Flags: ignoreversion
Source: "..\OZELLIKLER.txt"; DestDir: "{app}"; Flags: ignoreversion

[Icons]
Name: "{group}\{#AppName}"; Filename: "{app}\{#AppExeName}"; Comment: "YEK Panel'i aç"
Name: "{group}\Kullanım Kılavuzu"; Filename: "{app}\KURULUM.txt"
Name: "{group}\Kaldır {#AppName}"; Filename: "{uninstallexe}"
Name: "{autodesktop}\{#AppName}"; Filename: "{app}\{#AppExeName}"; Tasks: desktopicon

[Registry]
Root: HKCU; Subkey: "Software\YekPanel"; ValueType: string; ValueName: "InstallPath"; ValueData: "{app}"; Flags: uninsdeletekey
Root: HKCU; Subkey: "Software\YekPanel"; ValueType: string; ValueName: "Version"; ValueData: "{#AppVersion}"; Flags: uninsdeletekey

[Run]
Filename: "{app}\{#AppExeName}"; Description: "{cm:LaunchProgram,{#StringChange(AppName, '&', '&&')}}"; Flags: nowait postinstall skipifsilent

[UninstallDelete]
Type: filesandordirs; Name: "{app}\*.log"

[Messages]
turkish.WelcomeLabel1=YEK Panel Kurulum Sihirbazı
turkish.WelcomeLabel2=Bu sihirbaz YEK Panel {#AppVersion} sürümünü bilgisayarınıza kuracak.%n%nUygulama yalnızca bu kullanıcı için kurulur; yönetici izni gerekmez.%n%nKişisel sunucu adresleri, MAC adresleri ve şehir bilgisi kurulum paketine dahil edilmez.
turkish.FinishedHeadingLabel=Kurulum tamamlandı
turkish.FinishedLabel=YEK Panel kullanıma hazır. Ayarlar penceresinden kendi sunucularınızı ekleyebilirsiniz.

[Code]
function InitializeUninstall(): Boolean;
var
  Response: Integer;
begin
  Result := True;

  if UninstallSilent then
    Exit;

  Response := MsgBox(
    'YEK Panel ayarlarınız, geçmişiniz ve notlarınız da silinsin mi?' + #13#10 +
    ExpandConstant('{userappdata}\YekPanel') + #13#10#13#10 +
    'Hayır seçerseniz ayarlarınız korunur.',
    mbConfirmation, MB_YESNO);

  if Response = IDYES then
    DelTree(ExpandConstant('{userappdata}\YekPanel'), True, True, True);
end;
