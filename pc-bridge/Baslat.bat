@echo off
chcp 65001 >nul
title YEK Panel PC Koprusu
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0YekPanelBridge.ps1" %*
if errorlevel 1 (
    echo.
    echo Kopru beklenmedik bir hatayla kapandi.
    echo Windows PowerShell 5.1 kurulu mu? Ayrintilar icin README.md dosyasina bakin.
    pause
)
