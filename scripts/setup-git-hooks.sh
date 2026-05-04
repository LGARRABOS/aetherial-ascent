#!/usr/bin/env sh
# Définit core.hooksPath pour utiliser les hooks du dépôt (.githooks).
cd "$(dirname "$0")/.." || exit 1
git config core.hooksPath .githooks
echo "core.hooksPath=.githooks est configuré pour ce dépôt."
