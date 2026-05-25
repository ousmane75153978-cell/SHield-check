#!/bin/bash

# Script pour créer le fichier GitHub Actions
# Usage: bash setup-workflow.sh

set -e

echo "🔧 Configuration du workflow GitHub Actions..."

# Créer le répertoire .github/workflows s'il n'existe pas
mkdir -p .github/workflows

# Créer le fichier android.yml
cat > .github/workflows/android.yml << 'EOF'
name: Build APK

on:
  push:
    branches:
      - main
  pull_request:
    branches:
      - main
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 11
        uses: actions/setup-java@v4
        with:
          java-version: '11'
          distribution: 'temurin'

      - name: Make gradlew executable
        run: chmod +x gradlew

      - name: Build Debug APK
        run: ./gradlew assembleDebug

      - name: Build Release APK
        run: ./gradlew assembleRelease

      - name: Upload Debug APK
        uses: actions/upload-artifact@v4
        with:
          name: app-debug
          path: app/build/outputs/apk/debug/app-debug.apk

      - name: Upload Release APK
        uses: actions/upload-artifact@v4
        with:
          name: app-release
          path: app/build/outputs/apk/release/app-release-unsigned.apk
EOF

echo "✅ Fichier .github/workflows/android.yml créé avec succès!"
echo ""
echo "📋 Commandes à exécuter:"
echo "  git add .github/workflows/android.yml"
echo "  git commit -m 'Add GitHub Actions workflow for Android build'"
echo "  git push origin main"
echo ""
echo "🚀 Le workflow sera automatiquement disponible dans l'onglet Actions!"
