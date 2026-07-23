# Build Instructions

## Prerequisites
- Android Studio Hedgehog+ or command line
- Android SDK 34
- NDK 25.2.9519653+
- CMake 3.22.1+

## Steps

1. **Clone with submodule:**
```bash
git clone --recursive <repo-url>
# or: git submodule update --init --recursive
```

2. **Build:**
```bash
chmod +x gradlew
./gradlew assembleDebug
```

3. **Install:**
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## GitHub Actions (No Computer Needed)

Push to GitHub, Actions auto-builds APK. Download from artifacts.

## Model Downloads

The app downloads models automatically on first launch:
- SD-Turbo Q4_0: ~1.8GB from HuggingFace
- TAESD: ~4MB from HuggingFace

Models stored in app-private storage, deletable in Settings.
