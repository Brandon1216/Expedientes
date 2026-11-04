@echo off
rem Abre la ventana de instalación de Expedientes.
start "" powershell -NoProfile -STA -WindowStyle Hidden -ExecutionPolicy Bypass -File "%~dp0Instalar.ps1"
