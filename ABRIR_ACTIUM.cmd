@echo off
title ACTIUM v2
where node >nul 2>nul
if errorlevel 1 (
  echo Necesitas Node.js 22 o superior. Consulta INICIO_RAPIDO.md.
  pause
  exit /b 1
)
node "%~dp0iniciar.mjs"
if errorlevel 1 pause
