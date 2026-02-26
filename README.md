# Privacy Display — Android App

A **Samsung-style privacy screen filter** for any Android phone.  
Simulates a narrow-viewing-angle physical filter using a gradient overlay that works across **all apps**.

---

## Features

| Feature | Detail |
|---|---|
| 🔒 Side-angle darkening | Gradient overlay darkens screen edges to block side views |
| 👁 Auto face detection | Front camera + ML Kit detects a 2nd face → auto-activates filter |
| 🔘 Manual toggle | Big power button in app + Quick Settings tile |
| ⚙️ Adjustable intensity | How dark the edges appear (0–100%) |
| ↔ Adjustable width | How far the gradient reaches inward |
| 🔔 Persistent notification | Quick ON/OFF toggle without opening app |

---

## Requirements

- Android 8.0 (API 26) or higher
- Device with a **front-facing camera** (for face detection; optional)

---

## Build Instructions

### Option A — Android Studio (Recommended)
1. Install [Android Studio Hedgehog](https://developer.android.com/studio) or newer
2. Open this folder as a project
3. Let Gradle sync complete
4. Click **Run ▶** or **Build → Generate Signed APK**

### Option B — Command Line
```bash
# On Linux/Mac
./gradlew assembleDebug

# On Windows
gradlew.bat assembleDebug

# Output APK location:
# app/build/outputs/apk/debug/app-debug.apk
```

---

## First-Time Setup (on device)

When you first tap **PRIVACY ON**, the app will ask for:

1. **Notification permission** (Android 13+) — for the persistent toggle notification  
2. **Camera permission** — for face detection (tap "Allow" for full functionality)  
3. **Display over other apps** — *required* for the filter to work across all apps  
   - This opens Settings → find "Privacy Display" → enable the toggle

After granting permissions, the filter activates immediately.

---

## Quick Settings Tile

1. Pull down your notification shade twice
2. Tap the **pencil/edit** icon
3. Find **"Privacy Filter"** tile and drag it to your active tiles
4. Done — one tap to toggle from anywhere!

---

## How the Effect Works

The app adds a **transparent overlay** on top of everything using `WindowManager TYPE_APPLICATION_OVERLAY`. It draws dark linear gradients from the left and right screen edges toward the center using `Canvas` + `LinearGradient`.

- Viewed **head-on** → center is fully clear, edges are dark  
- Viewed **from the side** → the dark gradient dominates the visible area  

This mimics a physical privacy screen filter without any hardware modification.

---

## File Structure

```
app/src/main/kotlin/com/privacydisplay/
├── MainActivity.kt          — Main UI + permission flow
├── PrivacyService.kt        — Foreground service, overlay + camera management
├── PrivacyOverlayView.kt    — Custom View drawing the gradient filter
├── FaceDetectionAnalyzer.kt — CameraX + ML Kit face counting
└── PrivacyTileService.kt    — Quick Settings tile
```

---

## Permissions Explained

| Permission | Why |
|---|---|
| `CAMERA` | Front camera for face detection |
| `SYSTEM_ALERT_WINDOW` | Draw overlay on top of other apps |
| `FOREGROUND_SERVICE` | Keep service alive in background |
| `POST_NOTIFICATIONS` | Show persistent control notification |
