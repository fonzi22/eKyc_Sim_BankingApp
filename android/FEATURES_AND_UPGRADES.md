# Android App Features & Upgrade Guide

This document outlines the current features implemented in the Android application and provides technical guidance on how to upgrade or extend them.

## 1. Zero-Knowledge Proof (ZKP) Authentication

### Current Implementation
- **Protocol**: Schnorr Protocol on `secp256k1` elliptic curve.
- **Library**: Custom implementation in `SchnorrZKP.kt` using `Bouncy Castle` for curve math.
- **Flow**:
    - **Enrollment**: Generates KeyPair -> Hashes ID/PII -> Creates Proof -> Sends Public Key + Proof to Server.
    - **Login**: Challenges with SessionID -> Generates Proof using Private Key -> Sends Proof to Server.
- **Security**: Private Key never leaves the device.

### Upgrade Path
- **Switch to Standard Library**: Replace custom `SchnorrZKP` with a Rust-compiled library (via JNI) like `libsecp256k1` for better performance and auditability.
- **MPC (Multi-Party Computation)**: Implement Threshold Signatures where the key is split between Device and Server, requiring both to sign a transaction.
- **Post-Quantum Cryptography**: Research zk-STARKs or Lattice-based cryptography to replace Schnorr for future-proofing.

---

## 2. Security & Key Management

### Current Implementation
- **Storage**: Android Keystore System (`ZKPEnrollmentManager.kt`).
- **Encryption**: AES-256-GCM for PII and Private Key wrapping (`PIIEncryption.kt`).
- **Key Protection**: Keys are encrypted at rest and only accessible by the app.

### Upgrade Path
- **Biometric CryptoObject**: Bind the Keystore key to `BiometricPrompt`. This ensures the key *cannot* be used unless the user successfully authenticates with FaceID/Fingerprint (hardware level enforcement).
    - *Current*: App checks FaceID -> App asks Keystore for key.
    - *Upgrade*: App asks BiometricPrompt -> Hardware unlocks Key -> App uses Key.
- **TEE (Trusted Execution Environment)**: Ensure all ZKP math happens inside the TEE (StrongBox) if supported by the device.

---

## 3. Liveness Detection (Face ID)

### Current Implementation
- **Architecture**: Modular `FaceDetector` interface (`domain/FaceDetector.kt`).
- **Engine**: Google ML Kit Face Detection (`data/MLKitFaceDetector.kt`).
- **Check**: Passive check (Head Euler Angles) to ensure the user is looking at the camera.

### Upgrade Path
- **Active Liveness Challenge**:
    - Implement a "Challenge-Response" UI: "Turn head left", "Smile", "Blink".
    - Verify these actions in real-time using `FaceResult` landmarks.
- **Anti-Spoofing Model**:
    - Integrate a custom TensorFlow Lite model trained to distinguish real faces from screens/photos (texture analysis, depth map).
    - Implement `FaceDetector` interface with this new model.

---

## 4. Networking

### Current Implementation
- **Client**: Ktor Client (`NetworkModule.kt`).
- **Serialization**: Kotlinx Serialization.
- **Logging**: Ktor Logging plugin.
- **Error Handling**: Custom `Result` wrapper parsing server error messages.

### Upgrade Path
- **Certificate Pinning**: Configure Ktor to pin the Server's SSL certificate to prevent Man-in-the-Middle (MitM) attacks.
- **Token Management**: Implement JWT Access/Refresh Token rotation flow using Ktor's `Auth` plugin.
- **Retrofit**: If the team prefers, migrate `ApiService` to Retrofit, but Ktor is recommended for KMP (Kotlin Multiplatform) compatibility.

---

## 5. UI/UX (Jetpack Compose)

### Current Implementation
- **Framework**: Jetpack Compose (Modern, Declarative).
- **Navigation**: Navigation Compose with typed routes (`AppRoutes`).
- **Screens**: Landing, Camera (OCR), Confirm, Face Scan, Login, Home.

### Upgrade Path
- **Real Data Integration**: Connect `HomeScreen` to a real backend API for transactions and balance.
- **Animations**: Add shared element transitions between screens (e.g., ID card flying from Camera to Confirm screen).
- **Theme System**: Expand `ui/theme` to support Dark Mode and dynamic coloring based on user preferences.

---

## Directory Structure Overview

```
android/app/src/main/java/com/example/ekycsimulate/
├── data/               # Data Layer (API, Network, ML Kit impl)
├── domain/             # Domain Layer (Interfaces, Models)
├── ui/                 # UI Layer (Screens, ViewModel, Theme)
│   ├── auth/           # Authentication Screens (Login, Enroll)
│   ├── home/           # Banking Dashboard
│   └── viewmodel/      # State Management
├── zkp/                # Core ZKP Logic & Security
└── MainActivity.kt     # App Entry & Navigation
```
