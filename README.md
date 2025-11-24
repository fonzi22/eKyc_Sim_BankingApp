# WannaW1n Banking App - eKYC with Zero-Knowledge Proof

A privacy-preserving electronic Know Your Customer (eKYC) Android application that implements **True Zero-Knowledge Proof (Schnorr Protocol)** for secure identity verification without compromising user privacy.

![License](https://img.shields.io/badge/license-MIT-blue.svg)
![Platform](https://img.shields.io/badge/platform-Android-green.svg)
![Kotlin](https://img.shields.io/badge/kotlin-1.9+-purple.svg)

## Overview

WannaW1n Banking App demonstrates a cutting-edge approach to digital identity verification by combining:
- **On-device OCR** for ID card information extraction
- **Biometric face verification** with liveness detection
- **True Zero-Knowledge Proof** using Schnorr Protocol on Elliptic Curve Cryptography
- **End-to-end PII encryption** with AES-256-GCM

The system ensures that sensitive user data (PII, biometric images, private keys) **never leaves the device in plaintext**, while still allowing the server to verify user authenticity.

## Key Features

### Privacy-First Architecture
- **On-Device Processing**: Face scan, OCR, and cryptographic operations run entirely on the mobile device
- **No Raw Data Transmission**: Server never receives plaintext PII, face images, or private keys
- **True ZKP**: Server can verify user identity without knowing the secret (private key)
- **Hardware-Backed Security**: Uses Android Keystore for key storage

### Security Features
- **Schnorr Protocol ZKP**: Mathematically proven zero-knowledge authentication
- **AES-256-GCM Encryption**: Military-grade encryption for PII data
- **Liveness Detection**: Prevents spoofing with photo/video replay attacks
- **Replay Attack Prevention**: Nullifier mechanism ensures one-time proof validity
- **1 ID = 1 Account**: Hash-based uniqueness check prevents duplicate registrations

### Technical Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Camera**: CameraX API
- **OCR**: ML Kit Text Recognition
- **Cryptography**: BouncyCastle (secp256k1 Elliptic Curve)
- **Storage**: Android Keystore, Encrypted SharedPreferences

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    MOBILE DEVICE (Client)                    │
├─────────────────────────────────────────────────────────────┤
│  1. OCR Module          → Extract ID info from card photo   │
│  2. Face Scan Module    → Capture selfie + liveness check   │
│  3. ZKP Engine          → Generate Schnorr proof            │
│  4. PII Encryption      → AES-256-GCM with Keystore         │
│  5. Payload Generator   → Create enrollment/verification    │
└─────────────────────────────────────────────────────────────┘
                              ↓ (Encrypted Payload)
┌─────────────────────────────────────────────────────────────┐
│                      SERVER (Verifier)                       │
├─────────────────────────────────────────────────────────────┤
│  1. Proof Verification  → Verify ZKP without private key    │
│  2. Uniqueness Check    → Ensure 1 ID = 1 account          │
│  3. Encrypted Storage   → Store encrypted PII + proofs      │
│  4. Audit Logging       → Compliance & fraud detection      │
└─────────────────────────────────────────────────────────────┘
```

## User Flow

### Enrollment (First-time Registration)
1. User captures ID card photo
2. OCR extracts information (ID number, name, DOB, address)
3. User confirms/edits extracted data
4. Face scan with liveness detection → `approval = 1`
5. System generates:
   - Key pair `(privateKey, publicKey)`
   - Encrypted PII
   - Schnorr ZKP enrollment proof
6. Payload sent to server (contains only: `publicKey`, `encrypted_PII`, `id_hash`, `proof`)
7. Server verifies proof and stores data

### Verification (Subsequent Login)
1. Server sends `sessionId` challenge
2. User performs face scan (biometric unlock)
3. Device generates new ZKP proof bound to `sessionId`
4. Server verifies proof using stored `publicKey`
5. Authentication successful without exposing private key

## ZKP Technical Details

For detailed explanation of the Zero-Knowledge Proof implementation, see [ZKP_TECHNICAL.md](ZKP_TECHNICAL.md).

**Quick Summary:**
- **Protocol**: Schnorr Signature Scheme
- **Curve**: secp256k1 (same as Bitcoin)
- **Proof Size**: ~200 bytes
- **Verification**: O(1) - constant time
- **Security**: Based on Discrete Logarithm Problem (DLP)

## Installation

### Prerequisites
- Android Studio Hedgehog or later
- Android SDK 24+ (Android 7.0+)
- Kotlin 1.9+
- Gradle 8.0+

### Setup
1. Clone the repository:
   ```bash
   git clone https://github.com/Kh4ngTh41/eKyc_Sim_BankingApp.git
   cd eKyc_Sim_BankingApp
   ```

2. Open in Android Studio

3. Sync Gradle dependencies

4. Run on emulator or physical device

## Project Structure

```
app/src/main/java/com/example/ekycsimulate/
├── zkp/
│   ├── SchnorrZKP.kt              # Core ZKP implementation
│   ├── PIIEncryption.kt           # AES-256-GCM encryption
│   └── ZKPEnrollmentManager.kt    # Orchestrator
├── ui/
│   ├── auth/
│   │   ├── LandingScreen.kt       # Home screen
│   │   ├── ConfirmInfoScreen.kt   # OCR verification
│   │   └── FaceScanScreen.kt      # Biometric + ZKP
│   └── theme/                     # Gold/Black/White theme
├── ocr/
│   └── OcrUtils.kt                # ML Kit integration
└── utils/
    └── ImageProcessor.kt          # Image preprocessing
```

## API Payload Examples

### Enrollment Payload
```json
{
  "publicKey": "03a1b2c3d4e5f6...",
  "commitment": "sha256_hash_of_identity",
  "idNumberHash": "sha256_of_id_number",
  "encryptedPII": "{\"ciphertext\":\"...\",\"iv\":\"...\"}",
  "proof": {
    "commitmentR": "03x9y8z7w6v5...",
    "challenge": "a1b2c3d4e5f6...",
    "response": "f6g7h8i9j0k1..."
  },
  "timestamp": 1700000000000
}
```

### Verification Payload
```json
{
  "publicKey": "03a1b2c3d4e5f6...",
  "proof": {
    "commitmentR": "03m4n5o6p7q8...",
    "challenge": "b2c3d4e5f6g7...",
    "response": "g7h8i9j0k1l2..."
  },
  "nullifier": "unique_session_hash",
  "timestamp": 1700000001000
}
```

## Security Considerations

### What the Server Stores
- ✅ Public key
- ✅ Encrypted PII (AES-256-GCM)
- ✅ ID number hash (SHA-256)
- ✅ ZKP proofs
- ✅ Timestamps & nullifiers

### What the Server NEVER Knows
- ❌ Private key
- ❌ Face images
- ❌ PII plaintext (unless decrypted with HSM key)
- ❌ Biometric embeddings

### Threat Model
- **Protects Against**: Man-in-the-middle attacks, server compromise, replay attacks, credential stuffing
- **Assumes**: Device is not compromised, user protects biometric unlock
- **Future Work**: Implement TEE (Trusted Execution Environment) for enhanced key protection

## Compliance & Auditing

The system supports regulatory compliance by:
- Logging all enrollment/verification events with timestamps
- Storing encrypted PII that can be decrypted by authorized parties (with HSM key)
- Providing cryptographic proof of user actions (ZKP logs)
- Enabling fraud detection through ID hash uniqueness checks

## Roadmap

- [ ] Implement server-side ZKP verification API
- [ ] Add real AI-based liveness detection model
- [ ] Integrate with TEE/StrongBox for private key storage
- [ ] Support multiple ID document types
- [ ] Add biometric template protection (cancelable biometrics)
- [ ] Implement zk-SNARKs for more complex proofs




## Contact

For questions or collaboration contact me.

---

**Disclaimer**: This is a demonstration project for educational purposes. Do not use in production without proper security audit and compliance review.
