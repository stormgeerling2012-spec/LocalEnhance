# ZoomEnhance

On-device AI image enhancement using SD-Turbo + TAESD with Vulkan GPU acceleration.

## Features

- **No models in APK** — Downloads ~1.8GB on first launch (~15MB APK)
- **Local inference** — No cloud, fully private
- **SD-Turbo** — 4-step distilled diffusion
- **TAESD** — Tiny VAE for fast decode
- **Vulkan GPU** — Hardware accelerated on Pixel 9
- **Settings** — Delete models to free storage

## Quick Start

### Option 1: GitHub Actions (No local build)
1. Fork this repo on GitHub
2. Go to Actions tab → "Build APK" → Run workflow
3. Download APK from artifacts (after ~20 min)

### Option 2: Local Build
```bash
git submodule update --init --recursive
./gradlew assembleDebug
```

## Screens

| Screen | Description |
|--------|-------------|
| Download | One-time model download (~1.8GB) with progress bar |
| Enhance | Pick photo → tap "Enhance Image" → see result |
| Settings | View storage used, delete models, about info |

## Architecture

```
Download Screen → models saved to filesDir
       ↓
Enhance Screen → NativeLib.initModel() → img2img inference
       ↓
Settings Screen → ModelManager.deleteModels()
```

## License

MIT — Model weights subject to Stability AI Community License.
