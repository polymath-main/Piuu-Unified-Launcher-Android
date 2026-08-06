#!/usr/bin/env bash
# ==============================================================================
# Piuu Unified Launcher — Automated Versioned Release Downloader
# ==============================================================================
# Usage: ./scripts/download_release.sh [TAG_NAME]
# If TAG_NAME is omitted, automatically resolves and downloads the latest release.
# ==============================================================================

set -eo pipefail

REPO="polymath-main/Piuu-Unified-Launcher-Android"
OUTPUT_DIR="downloads"

mkdir -p "$OUTPUT_DIR"

TAG_ARG="$1"

if [ -n "$TAG_ARG" ]; then
    TARGET_TAG="$TAG_ARG"
    echo "🔍 Target release tag specified: $TARGET_TAG"
else
    echo "🔍 Fetching latest version tag from GitHub..."
    if command -v gh &>/dev/null; then
        TARGET_TAG=$(gh release view --repo "$REPO" --json tagName -q ".tagName" 2>/dev/null || echo "v1.0.0-release")
    else
        TARGET_TAG=$(curl -s "https://api.github.com/repos/$REPO/releases/latest" | grep '"tag_name":' | sed -E 's/.*"([^"]+)".*/\1/' || echo "v1.0.0-release")
    fi
    echo "✨ Latest release tag detected: $TARGET_TAG"
fi

echo "🚀 Downloading versioned APK assets for release '$TARGET_TAG' into './$OUTPUT_DIR/'..."

if command -v gh &>/dev/null; then
    gh release download "$TARGET_TAG" --repo "$REPO" --dir "$OUTPUT_DIR" --clobber
else
    echo "📥 Downloading via GitHub API HTTP..."
    DOWNLOAD_URLS=$(curl -s "https://api.github.com/repos/$REPO/releases/tags/$TARGET_TAG" | grep "browser_download_url" | cut -d '"' -f 4)
    for url in $DOWNLOAD_URLS; do
        filename=$(basename "$url")
        echo "  ↳ Downloading $filename..."
        curl -sL "$url" -o "$OUTPUT_DIR/$filename"
    done
fi

echo ""
echo "=============================================================================="
echo "✅ Download Complete! Versioned Release Artifacts in './$OUTPUT_DIR/':"
echo "=============================================================================="
ls -lh "$OUTPUT_DIR"/*.apk 2>/dev/null || echo "No .apk files found in $OUTPUT_DIR"
echo ""
echo "🔐 SHA-256 Checksums:"
if command -v sha256sum &>/dev/null; then
    sha256sum "$OUTPUT_DIR"/*.apk 2>/dev/null || true
fi
