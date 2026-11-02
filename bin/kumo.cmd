@echo off
rem Delega en PowerShell sin cambiar el directorio del corpus.
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0kumo.ps1" %*
exit /b %errorlevel%
