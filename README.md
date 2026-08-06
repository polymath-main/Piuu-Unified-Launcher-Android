# Piuu Launcher — Native Android App & Extension System

A modular, schema-driven, hybrid Android launcher built with **Kotlin**, **Jetpack Compose**, and a POSIX C Native Shared Core (`libpiuu_core.so`).

---

## 📱 Quick Start & Automated Versioned Downloads

Download the latest versioned release APKs automatically using the helper script:

```bash
# Automatically fetch & download latest versioned APK assets (PiuuLauncher-v1.0.0-universal-release.apk)
./scripts/download_release.sh

# Or download a specific version tag:
./scripts/download_release.sh v1.0.0-release
```

---

## ⚡ Key Features

- **POSIX C Native Core (`libpiuu_core.so`)**: 16KB Page Aligned zero-copy JNI shared memory arena for sub-millisecond telemetry.
- **Automated Artifact Versioning**: Releases built with standardized asset names (`PiuuLauncher-${VERSION}-${ARCH}-${BUILD_TYPE}.apk`).
- **2D Matrix Grid Resizing (1x1 to 4x4)**: Interactive widget width/height handles and long-press Context Hub.
- **Electron Extension Studio**: Cross-platform extension builder in `piuu-studio-desktop/`.
- **1-Tap Theme Transformer**: Preset glassmorphic, OLED, pastel, and retro arcade design themes.
- **60fps App Drawer**: 250-item `ImageBitmap` LruCache icon loader with Smart Usage row and Android status bar margin fit.

---

## 🌿 Branching Conventions

* **`piuu`** (`main` Branch): Production-stable baseline launcher.
* **`zen-piuu`** (`master` Branch): Extension architecture & core planned master branch.

See [`BRANCHING_RULES.md`](file:///data/data/com.termux/files/home/repo/Piuu-Unified-Launcher-Android/BRANCHING_RULES.md) and [`AGENTS.md`](file:///data/data/com.termux/files/home/repo/Piuu-Unified-Launcher-Android/AGENTS.md) for complete developer documentation.
