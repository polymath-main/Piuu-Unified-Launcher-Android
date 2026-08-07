# 🧠 Project Memory & Architecture Log: Piuu Unified Launcher

## 📌 Project Overview
* **Repository**: [`polymath-main/Piuu-Unified-Launcher-Android`](https://github.com/polymath-main/Piuu-Unified-Launcher-Android)
* **Primary Target**: Production-Grade Android 16 (API 36) Unified Launcher with POSIX C Native Engine & Electron Desktop Studio.

---

## 🌿 Branch Operating & Synchronization Rules
- **`piuu`** (`main` branch): Production-stable baseline launcher.
- **`zen-piuu`** (`master` branch): Extension architecture & core master branch.
- **Branch Synchronization Directive**: Both branches MUST be kept 100% merged and fast-forwarded at all times.
- **No Auto-Push Rule**: Keep code edits local until explicit push instruction from user. CI/CD workflow builds are manual-dispatch (`workflow_dispatch`) only.

---

## ⚙️ Native C Core Architecture (`libpiuu_core.so`)
- **Headers & Sources**: [`app/src/main/cpp/piuu_core.h`](file:///data/data/com.termux/files/home/repo/Piuu-Unified-Launcher-Android/app/src/main/cpp/piuu_core.h) & [`piuu_core.c`](file:///data/data/com.termux/files/home/repo/Piuu-Unified-Launcher-Android/app/src/main/cpp/piuu_core.c).
- **16KB Page Alignment**: Enforced via `-Wl,-z,max-page-size=16384` in [`CMakeLists.txt`](file:///data/data/com.termux/files/home/repo/Piuu-Unified-Launcher-Android/app/src/main/cpp/CMakeLists.txt) for Android 16 API 36 hardware compatibility.
- **Thread Safety**: POSIX mutex `pthread_mutex_t g_mem_mutex` protects all native heap allocations and total memory statistics.
- **Zero-Copy Arena Buffer**: `allocateArena(size)` exposes direct `NewDirectByteBuffer` to Kotlin JNI without Garbage Collection (GC) latency overhead.

---

## 📱 UI & PiP Side Edge Assist
- **Raw Wallpaper View**: Default transparency `0.0f` (raw wallpaper view). Toggle `enableWallpaperMask` in Launcher Settings for glass masking.
- **PiP Side Edge Assist**: Floating overlay service ([`FloatingOverlayService.kt`](file:///data/data/com.termux/files/home/repo/Piuu-Unified-Launcher-Android/app/src/main/java/com/piuu/launcher/repository/FloatingOverlayService.kt)) with vertical edge docking, auto-hide to 6dp bar, top drop removal zone (`🗑️ Drop to remove`), Quick App Switcher, and persistent Notes & Quick Memo widget ([`NotesRepository.kt`](file:///data/data/com.termux/files/home/repo/Piuu-Unified-Launcher-Android/app/src/main/java/com/piuu/launcher/repository/NotesRepository.kt)).

---

## 🔒 Credentials & CI/CD Workflow
- **GitHub Primary Account**: `polymath-main`
- **Workflow File**: [`.github/workflows/android.yml`](file:///data/data/com.termux/files/home/repo/Piuu-Unified-Launcher-Android/.github/workflows/android.yml)
- **Permissions**: `permissions: contents: write` declared under `jobs.build`.
