# Zero-Knowledge Proof Technical Documentation

This document explains the cryptographic implementation of the Zero-Knowledge Proof system used in WannaW1n Banking App.

## What is Zero-Knowledge Proof

A Zero-Knowledge Proof (ZKP) is a cryptographic method where one party (the prover) can prove to another party (the verifier) that they know a secret value, without revealing the secret itself.

### Core Properties
1. **Completeness**: If the statement is true, an honest prover can convince the verifier
2. **Soundness**: If the statement is false, no cheating prover can convince the verifier (except with negligible probability)
3. **Zero-Knowledge**: The verifier learns nothing about the secret except that the statement is true

### Why Not Hash-Based Authentication?

Traditional hash-based challenge-response requires the server to know the secret to verify:
```
Server: "Prove you know the password"
Client: response = Hash(challenge + password + commitment)
Server: expectedResponse = Hash(challenge + PASSWORD_I_KNOW + commitment)
         if response == expectedResponse: authenticate()
```

Problem: **Server must know the password**. If the server is compromised, all passwords are exposed.

### True ZKP Approach

With Schnorr Protocol:
```
Server: "Prove you know the private key corresponding to this public key"
Client: Generates proof using private key
Server: Verifies proof using ONLY public key
         Server NEVER knows the private key
```

## Schnorr Protocol Overview

The Schnorr signature scheme is a digital signature protocol that can be adapted for zero-knowledge authentication.

### Mathematical Foundation

Based on the **Discrete Logarithm Problem (DLP)** on Elliptic Curves:
- Given `P = x * G`, it's computationally infeasible to find `x` from `P` and `G`
- `G` is the generator point on the elliptic curve (secp256k1)
- `x` is the private key (secret)
- `P` is the public key (public)

### Protocol Steps

#### Key Generation
```
privateKey = random integer in [1, n-1]
publicKey = privateKey * G
```

#### Proof Generation (Prover)
```
1. Generate random k (ephemeral key)
2. Compute R = k * G (commitment)
3. Compute c = Hash(R || publicKey || message) (challenge)
4. Compute s = k + c * privateKey (mod n) (response)
5. Proof = (R, c, s)
```

#### Proof Verification (Verifier)
```
1. Recompute c' = Hash(R || publicKey || message)
2. Check if c' == c
3. Verify equation: s * G == R + c * publicKey
4. If both checks pass: proof is valid
```

### Why This is Zero-Knowledge

The verifier only sees:
- `R` (random point)
- `c` (hash)
- `s` (response)

From these values, it's mathematically impossible to derive `privateKey` due to the DLP hardness.

## Implementation Details

### Elliptic Curve: secp256k1

Same curve used by Bitcoin and Ethereum:
- **Field**: 256-bit prime field
- **Order**: ~2^256
- **Security**: 128-bit security level
- **Performance**: Fast on modern hardware

### Code Structure

```kotlin
object SchnorrZKP {
    // Curve parameters
    private val curveParams = ECNamedCurveTable.getParameterSpec("secp256k1")
    private val G: ECPoint = curveParams.g  // Generator point
    private val n: BigInteger = curveParams.n  // Curve order
    
    // Generate key pair
    fun generateKeyPair(): KeyPair {
        val privateKey = BigInteger(256, secureRandom).mod(n)
        val publicKey = G.multiply(privateKey).normalize()
        return KeyPair(privateKey, publicKey)
    }
    
    // Generate proof
    fun generateProof(privateKey: BigInteger, message: String): SchnorrProof {
        val k = BigInteger(256, secureRandom).mod(n)
        val R = G.multiply(k).normalize()
        val P = G.multiply(privateKey).normalize()
        val c = computeChallenge(R, P, message)
        val s = k.add(c.multiply(privateKey)).mod(n)
        return SchnorrProof(R, c, s)
    }
    
    // Verify proof
    fun verifyProof(publicKey: ECPoint, proof: SchnorrProof, message: String): Boolean {
        val c = computeChallenge(proof.R, publicKey, message)
        if (c != proof.c) return false
        
        val leftSide = G.multiply(proof.s).normalize()
        val rightSide = proof.R.add(publicKey.multiply(proof.c)).normalize()
        return leftSide == rightSide
    }
}
```

### Fiat-Shamir Heuristic

To make the protocol non-interactive, we use the Fiat-Shamir transform:
```kotlin
fun computeChallenge(R: ECPoint, P: ECPoint, message: String): BigInteger {
    val digest = SHA256()
    digest.update(R.getEncoded())
    digest.update(P.getEncoded())
    digest.update(message.toByteArray())
    return BigInteger(1, digest.digest()).mod(n)
}
```

This eliminates the need for back-and-forth communication between prover and verifier.

## Enrollment Flow

### Step-by-Step Process

```
1. OCR Extraction
   Input: ID card photo
   Output: {idNumber, fullName, dob, address}

2. Face Scan + Liveness Detection
   Input: Selfie video/photo
   Output: approval = 1 (if liveness passed)

3. Key Generation
   privateKey = random(256 bits)
   publicKey = privateKey * G

4. PII Encryption
   piiJson = JSON({idNumber, fullName, ...})
   encryptedPII = AES-256-GCM(piiJson, keystore_key)

5. Commitment Generation
   idHash = SHA256(idNumber)
   commitment = SHA256(publicKey || idHash)

6. ZKP Proof Generation
   message = "ENROLL:" + commitment + ":" + timestamp
   proof = SchnorrProof(privateKey, message)

7. Payload Assembly
   payload = {
     publicKey: hex(publicKey),
     commitment: commitment,
     idNumberHash: idHash,
     encryptedPII: encryptedPII,
     proof: {R, c, s},
     timestamp: now()
   }

8. Send to Server
   POST /api/enroll
   Body: JSON(payload)
```

### What Gets Stored

**On Device:**
- `privateKey` (in Android Keystore, encrypted)

**On Server:**
- `publicKey`
- `commitment`
- `idNumberHash`
- `encryptedPII`
- `proof`
- `timestamp`

## Verification Flow

### Step-by-Step Process

When a user needs to authenticate again (subsequent login), they do NOT need to repeat the enrollment process. Instead, they prove they are the same person who enrolled before.

```
1. Server Challenge
   Server generates sessionId (random nonce)
   Server sends: {sessionId}

2. User Opens App
   User clicks "Login" or "Verify Identity"

3. Biometric Unlock (On Device)
   App prompts: "Scan your face to authenticate"
   User performs face scan (biometric authentication)
   
   If face matches enrolled template:
   → Android Keystore releases privateKey
   
   If face does NOT match:
   → Authentication fails, cannot proceed

4. Nullifier Generation (On Device)
   nullifier = SHA256(privateKey || sessionId)
   Purpose: Prevent replay attacks (each sessionId creates unique nullifier)

5. ZKP Proof Generation (On Device)
   message = "VERIFY:" + sessionId + ":" + timestamp
   proof = SchnorrProof(privateKey, message)
   
   This proves: "I know the private key that was used during enrollment"

6. Payload Assembly (On Device)
   payload = {
     publicKey: hex(publicKey),
     proof: {R, c, s},
     nullifier: nullifier,
     timestamp: now()
   }

7. Send to Server
   POST /api/verify
   Body: JSON(payload)

8. Server Verification
   a. Load user by publicKey from database
   b. Check nullifier has NOT been used before
      → If used: reject (replay attack)
   c. Rebuild message: "VERIFY:{sessionId}:{timestamp}"
   d. Verify ZKP proof: SchnorrVerify(publicKey, proof, message)
      → If invalid: reject
   e. Mark nullifier as used in database
   f. Generate session token for user
   
   Return: {success: true, sessionToken: "..."}
```

### What User Experiences

#### First Time (Enrollment)
1. Take photo of ID card
2. Confirm OCR data
3. Scan face (liveness check)
4. Wait for ZKP generation (~2 seconds)
5. Account created ✅

#### Subsequent Logins (Verification)
1. Click "Login"
2. Scan face (biometric unlock)
3. Wait for ZKP generation (~1 second)
4. Logged in ✅

**No password needed. No SMS OTP. Just face scan.**

### Security Guarantees

**What Server Verifies:**
- ✅ User owns the private key from enrollment
- ✅ Proof is fresh (bound to current sessionId)
- ✅ Proof has not been replayed (nullifier check)

**What Server Does NOT Know:**
- ❌ The private key itself
- ❌ Face biometric data
- ❌ Any PII (unless decrypted with HSM key)

### Example Code (Client-Side)

```kotlin
// User clicks "Login"
fun performLogin(sessionId: String) {
    // Step 1: Prompt biometric
    biometricPrompt.authenticate(
        onSuccess = {
            // Step 2: Generate verification proof
            val enrollmentManager = ZKPEnrollmentManager(context)
            val verificationPayload = enrollmentManager.performVerification(sessionId)
            
            if (verificationPayload != null) {
                // Step 3: Send to server
                val json = enrollmentManager.verificationPayloadToJson(verificationPayload)
                apiService.verify(json) { response ->
                    if (response.success) {
                        // Logged in!
                        navigateToHome()
                    }
                }
            }
        },
        onError = {
            showError("Biometric authentication failed")
        }
    )
}
```

### Example Code (Server-Side)

```python
def verify_login(payload, session_id):
    # Step 1: Load user
    user = db.get_by_public_key(payload['publicKey'])
    if not user:
        return {"error": "User not found"}
    
    # Step 2: Check nullifier (prevent replay)
    if db.nullifier_used(payload['nullifier']):
        return {"error": "Replay attack detected"}
    
    # Step 3: Rebuild message
    message = f"VERIFY:{session_id}:{payload['timestamp']}"
    
    # Step 4: Verify ZKP proof
    public_key = decode_point(payload['publicKey'])
    proof = decode_proof(payload['proof'])
    
    if not schnorr_verify(public_key, proof, message):
        return {"error": "Invalid proof"}
    
    # Step 5: Mark nullifier as used
    db.mark_nullifier_used(payload['nullifier'])
    
    # Step 6: Create session
    session_token = create_session(user.id)
    
    return {
        "success": True,
        "sessionToken": session_token,
        "userId": user.id
    }
```

### Verification Payload Example

```json
{
  "publicKey": "03a1b2c3d4e5f6...",
  "proof": {
    "commitmentR": "03m4n5o6p7q8...",
    "challenge": "b2c3d4e5f6g7...",
    "response": "g7h8i9j0k1l2..."
  },
  "nullifier": "8f3e2a1b9c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1",
  "timestamp": 1700000001000
}
```

### Nullifier Mechanism

The nullifier prevents replay attacks:

```
Enrollment:
- User enrolls with privateKey_A
- Server stores publicKey_A

Login Session 1:
- Server sends sessionId_1 = "abc123"
- Client generates nullifier_1 = SHA256(privateKey_A + "abc123")
- Server marks nullifier_1 as used

Replay Attack Attempt:
- Attacker captures payload with nullifier_1
- Attacker replays it to server
- Server checks: nullifier_1 already used → REJECT

Login Session 2 (Legitimate):
- Server sends sessionId_2 = "def456" (different)
- Client generates nullifier_2 = SHA256(privateKey_A + "def456")
- nullifier_2 ≠ nullifier_1 → Server accepts
```

### Comparison: Traditional vs ZKP Login

| Aspect | Traditional (Password) | ZKP (This System) |
|--------|----------------------|-------------------|
| **What user does** | Type password | Scan face |
| **What gets sent** | Password hash | ZKP proof |
| **Server knows** | Password hash | Public key only |
| **If server hacked** | All passwords leaked | Cannot impersonate users |
| **Phishing risk** | High (user types password) | Low (no password to steal) |
| **Replay attack** | Possible | Prevented (nullifier) |
| **Biometric data sent** | N/A | Never (stays on device) |


### Security Assumptions

1. **Discrete Logarithm Problem is hard**: No efficient algorithm to solve DLP on secp256k1
2. **SHA-256 is collision-resistant**: No two inputs produce same hash
3. **Random number generator is secure**: Android's SecureRandom is cryptographically strong
4. **Device is not compromised**: Malware cannot extract keys from Keystore
5. **User protects biometric**: Attacker cannot unlock device

## Server-Side Verification

### Pseudocode (Python)

```python
from ecdsa import SECP256k1, VerifyingKey
import hashlib

def verify_enrollment(payload):
    # 1. Parse payload
    public_key_hex = payload['publicKey']
    commitment = payload['commitment']
    id_hash = payload['idNumberHash']
    encrypted_pii = payload['encryptedPII']
    proof = payload['proof']
    timestamp = payload['timestamp']
    
    # 2. Check ID uniqueness
    if db.exists(id_hash):
        return {"error": "ID already enrolled"}
    
    # 3. Decode public key
    public_key_bytes = bytes.fromhex(public_key_hex)
    vk = VerifyingKey.from_string(public_key_bytes, curve=SECP256k1)
    
    # 4. Reconstruct message
    message = f"ENROLL:{commitment}:{timestamp}"
    
    # 5. Verify Schnorr proof
    R = decode_point(proof['commitmentR'])
    c = int(proof['challenge'], 16)
    s = int(proof['response'], 16)
    
    # Recompute challenge
    c_prime = compute_challenge(R, vk.pubkey.point, message)
    if c != c_prime:
        return {"error": "Invalid challenge"}
    
    # Verify equation: s*G == R + c*P
    G = SECP256k1.generator
    left = s * G
    right = R + c * vk.pubkey.point
    
    if left != right:
        return {"error": "Invalid proof"}
    
    # 6. Store data
    db.save({
        "public_key": public_key_hex,
        "commitment": commitment,
        "id_hash": id_hash,
        "encrypted_pii": encrypted_pii,
        "proof": proof,
        "timestamp": timestamp
    })
    
    return {"success": True}

def verify_login(payload, session_id):
    # Similar process but checks nullifier
    public_key_hex = payload['publicKey']
    proof = payload['proof']
    nullifier = payload['nullifier']
    timestamp = payload['timestamp']
    
    # Load user
    user = db.get_by_public_key(public_key_hex)
    if not user:
        return {"error": "User not found"}
    
    # Check nullifier
    if db.nullifier_used(nullifier):
        return {"error": "Replay attack detected"}
    
    # Verify proof
    message = f"VERIFY:{session_id}:{timestamp}"
    if not schnorr_verify(public_key_hex, proof, message):
        return {"error": "Invalid proof"}
    
    # Mark nullifier as used
    db.mark_nullifier_used(nullifier)
    
    return {"success": True, "user_id": user.id}
```

### Database Schema

```sql
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    public_key TEXT UNIQUE NOT NULL,
    commitment TEXT NOT NULL,
    id_hash TEXT UNIQUE NOT NULL,
    encrypted_pii TEXT NOT NULL,
    enrollment_proof JSONB NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE verification_logs (
    id SERIAL PRIMARY KEY,
    user_id INTEGER REFERENCES users(id),
    nullifier TEXT UNIQUE NOT NULL,
    proof JSONB NOT NULL,
    verified_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_id_hash ON users(id_hash);
CREATE INDEX idx_public_key ON users(public_key);
CREATE INDEX idx_nullifier ON verification_logs(nullifier);
```

## Performance Metrics

### Mobile Device (Typical Android Phone)
- **Key Generation**: ~50ms
- **Proof Generation**: ~100ms
- **Proof Verification**: ~80ms
- **Total Enrollment**: ~2-3 seconds (including UI)

### Server
- **Proof Verification**: ~10ms (Python)
- **Proof Verification**: ~2ms (C/Rust with libsecp256k1)

### Payload Size
- **Enrollment**: ~1.5 KB
- **Verification**: ~0.5 KB


## **Why Schnorr for eKYC:**
- No trusted setup required
- Fast enough for mobile devices
- Simple to implement and audit
- Well-studied security properties

## Future Enhancements

1. **Batch Verification**: Verify multiple proofs simultaneously
2. **Threshold Signatures**: Require multiple parties to authenticate
3. **Revocation**: Add proof of non-revocation
4. **Anonymous Credentials**: Prove attributes without revealing identity
5. **zk-SNARKs Integration**: For more complex statements (e.g., "age > 18" without revealing exact age)


## Conclusion

The Schnorr-based ZKP system provides a practical, secure, and privacy-preserving authentication mechanism for eKYC applications. By ensuring that private keys never leave the device and servers can verify identity without knowing secrets, we achieve true zero-knowledge authentication suitable for financial applications.
