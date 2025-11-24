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
 * PII Encryption using AES-256-GCM with Android Keystore
 * Keys are stored in hardware-backed keystore (HSM equivalent on Android)
 */
object PIIEncryption {
    
    private const val KEY_ALIAS = "ekyc_pii_encryption_key"
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
    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        
        // Check if key exists
        if (keyStore.containsAlias(KEY_ALIAS)) {
            return keyStore.getKey(KEY_ALIAS, null) as SecretKey
        }
        
        // Generate new key
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )
        
        val keyGenSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
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
     * Encrypt PII data using AES-256-GCM
     * 
     * @param plaintext The PII data in JSON format
     * @return Encrypted data with IV
     */
    fun encrypt(plaintext: String): EncryptedData {
        val key = getOrCreateKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        
        return EncryptedData(
            ciphertext = Base64.encodeToString(ciphertext, Base64.NO_WRAP),
            iv = Base64.encodeToString(iv, Base64.NO_WRAP),
            tag = "" // GCM tag is included in ciphertext
        )
    }

    /**
     * Decrypt PII data
     * 
     * @param encryptedData The encrypted data
     * @return Decrypted plaintext
     */
    fun decrypt(encryptedData: EncryptedData): String {
        val key = getOrCreateKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        
        val iv = Base64.decode(encryptedData.iv, Base64.NO_WRAP)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        
        val ciphertext = Base64.decode(encryptedData.ciphertext, Base64.NO_WRAP)
        val plaintext = cipher.doFinal(ciphertext)
        
        return String(plaintext, Charsets.UTF_8)
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
