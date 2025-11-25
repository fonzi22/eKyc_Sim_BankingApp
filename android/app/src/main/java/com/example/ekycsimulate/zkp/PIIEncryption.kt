package com.example.ekycsimulate.zkp

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.util.Base64

/**
 * Encryption utility using AES-256-GCM with Android Keystore
 * Keys are stored in hardware-backed keystore (HSM equivalent on Android)
 */
object PIIEncryption {
    
    const val PII_KEY_ALIAS = "ekyc_pii_encryption_key"
    const val ZKP_KEY_ALIAS = "ekyc_zkp_key_wrapping_key"
    
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128
    
    /**
     * Encrypted data structure
     */
    data class EncryptedData(
        val ciphertext: String,  // Base64 encoded
        val iv: String,          // Base64 encoded IV
        val tag: String          // Base64 encoded authentication tag (included in GCM)
    )

    /**
     * Get or create encryption key from Android Keystore
     */
    private fun getOrCreateKey(alias: String): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        
        // Check if key exists
        if (keyStore.containsAlias(alias)) {
            return keyStore.getKey(alias, null) as SecretKey
        }
        
        // Generate new key
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )
        
        val keyGenSpec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(false) // Set to true if you want biometric unlock
            .build()
        
        keyGenerator.init(keyGenSpec)
        return keyGenerator.generateKey()
    }

    /**
     * Encrypt data using AES-256-GCM
     * 
     * @param plaintext The data to encrypt
     * @param alias The keystore alias to use
     * @return Encrypted data with IV
     */
    fun encrypt(plaintext: String, alias: String = PII_KEY_ALIAS): EncryptedData {
        val key = getOrCreateKey(alias)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        
        val iv = cipher.iv
        val ciphertextBytes = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        
        return EncryptedData(
            ciphertext = Base64.encodeToString(ciphertextBytes, Base64.NO_WRAP),
            iv = Base64.encodeToString(iv, Base64.NO_WRAP),
            tag = "" // GCM tag is included in ciphertext
        )
    }

    /**
     * Decrypt data
     * 
     * @param encryptedData The encrypted data
     * @param alias The keystore alias to use
     * @return Decrypted plaintext
     */
    fun decrypt(encryptedData: EncryptedData, alias: String = PII_KEY_ALIAS): String {
        val key = getOrCreateKey(alias)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        
        val iv = Base64.decode(encryptedData.iv, Base64.NO_WRAP)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        
        val ciphertextBytes = Base64.decode(encryptedData.ciphertext, Base64.NO_WRAP)
        val plaintextBytes = cipher.doFinal(ciphertextBytes)
        
        return String(plaintextBytes, Charsets.UTF_8)
    }

    /**
     * Serialize encrypted data to JSON string
     */
    fun encryptedDataToJson(data: EncryptedData): String {
        return """{"ciphertext":"${data.ciphertext}","iv":"${data.iv}"}"""
    }

    /**
     * Deserialize JSON string to encrypted data
     */
    fun jsonToEncryptedData(json: String): EncryptedData {
        // Simple JSON parsing (you can use Gson/Moshi for production)
        val ciphertext = json.substringAfter("\"ciphertext\":\"").substringBefore("\"")
        val iv = json.substringAfter("\"iv\":\"").substringBefore("\"")
        return EncryptedData(ciphertext, iv, "")
    }
}
