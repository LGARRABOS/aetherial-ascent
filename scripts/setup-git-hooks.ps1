# Définit core.hooksPath pour utiliser les hooks du dépôt (.githooks)
$ErrorActionPreference = "Stop"
Set-Location (Join-Path $PSScriptRoot "..")
git config core.hooksPath .githooks
Write-Host "core.hooksPath=.githooks est configuré pour ce dépôt."
