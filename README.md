# StatusSplitter — Lossless Social Video Splitter for Android

<div align="center">

![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%2B)-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Language](https://img.shields.io/badge/Language-Kotlin%202.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Pipeline](https://img.shields.io/badge/Engine-Lossless%20GOP%20Sync%20Remux-orange?style=for-the-badge)
![License](https://img.shields.io/badge/License-MIT-green.svg?style=for-the-badge)
[![WhatsApp](https://img.shields.io/badge/WhatsApp-Contact%20letmese-25D366?style=for-the-badge&logo=whatsapp&logoColor=white)](https://wa.me/923171616345?text=Hi%20letmese%2C%20I%20am%20interested%20in%20your%20StatusSplitter%20Android%20app.)

### *Precision Lossless Video Splitting for WhatsApp Status, Instagram Stories, and Short-Form Social Feeds*

</div>

---

## 📖 Overview

**StatusSplitter** is a native Android application designed to split long-form videos into precise, sequential 30-second clips for WhatsApp Status, Instagram Stories, Facebook Stories, and TikTok feeds.

Built with a **lossless sync-frame GOP remuxing engine**, StatusSplitter partitions video files without re-encoding, preserving 100% of original visual and audio quality while completing exports in fractions of a second.

---

## 📱 App Preview (Live Firebase Test Lab Pixel 5 Screenshots)

<div align="center">
  <img src="screenshots/0.png" width="220" alt="Home Screen" />
  <img src="screenshots/1.png" width="220" alt="Split Screen" />
  <img src="screenshots/2.png" width="220" alt="Settings & Picker" />
  <img src="screenshots/3.png" width="220" alt="Processing View" />
</div>

<div align="center">
  <img src="screenshots/4.png" width="220" alt="Playback Preview" />
  <img src="screenshots/5.png" width="220" alt="Segment List" />
  <img src="screenshots/7.png" width="220" alt="Export Flow" />
  <img src="screenshots/10.png" width="220" alt="Batch Sharing" />
</div>

---

- ⚡ **Lossless Remuxing (Zero Quality Loss):** Operates on H.264 + AAC MP4 streams using sync-frame-aligned cuts (`planRanges`). Zero generational compression or re-encoding artifacts.
- 🎯 **Accurate 30-Second Boundaries:** Dynamically detects Keyframes (I-frames) to cut at clean boundaries without audio sync drift, freeze frames, or corruption.
- 🛡️ **Zero-Permission Privacy Architecture:** Leverages Android's native system file picker (`ACTION_GET_CONTENT`); requires no broad storage, contacts, or background accessibility permissions.
- 🎬 **Seamless In-App Video Previews:** Built-in review player utilizing secure `content://` URIs with granular read-grant flags.
- 🔄 **Sequential Story Posting:** Guides users through posting story segments in exact chronological order without duplicating or skipping clips.
- 🧪 **Test-Driven Architecture:** Complete unit and integration test suite (JUnit4 + Robolectric) ensuring regression resistance and UI stability.

---

## 🛠️ Architecture & Tech Stack

- **Platform:** Android 8.0+ (compileSdk / targetSdk 35)
- **Language:** Kotlin 2.0 (Android Gradle Plugin 8.5)
- **Core Engine:** Android `MediaExtractor` + `MediaMuxer` with custom sync-frame GOP planner
- **Storage & State:** `AtomicFile` JSON-backed state repository with sandbox confinement

---

## 🚀 Building from Source

### Prerequisites
* Android Studio (2024+) or Gradle 8.10
* JDK 17
* Android SDK 35

### Build Instructions
```bash
git clone https://github.com/letmese/StatusSplitter.git
cd StatusSplitter

# Run automated test suite
./gradlew testDebugUnitTest

# Assemble debug APK
./gradlew assembleDebug
```

---

## 📦 Pre-Built Downloads

To download the tested standalone Android APK (`StatusSplitter-v1.5.2-debug.apk`), check the releases:
* **[GitHub Releases](https://github.com/letmese/StatusSplitter/releases)**

---

## 👨‍💻 Developer & Inquiries

Designed and developed by **letmese**.

* **GitHub Profile:** [@letmese](https://github.com/letmese)
* **Location:** Lahore, Pakistan (Available for mobile app contracts worldwide)
* **WhatsApp Direct:** [**+92 317 1616345**](https://wa.me/923171616345?text=Hi%20letmese%2C%20I%20am%20interested%20in%20your%20StatusSplitter%20Android%20app.)

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).
