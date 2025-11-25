package com.example.ekycsimulate.zkp

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigInteger

@RunWith(AndroidJUnit4::class)
class ZKPKeyStorageTest {

    private lateinit var context: Context
    private lateinit var enrollmentManager: ZKPEnrollmentManager

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        // Clear existing prefs to start fresh
        context.getSharedPreferences("zkp_keys", Context.MODE_PRIVATE).edit().clear().apply()
        enrollmentManager = ZKPEnrollmentManager(context)
    }

    @Test
    fun testKeyStorageMigration() {
        // 1. Simulate enrollment (which saves the key)
        // We can't easily call performEnrollment without mocking OCR/Face data, 
        // but we can test the private methods if we make them accessible or use reflection.
        // Or better, we just use the public API if possible.
        // performEnrollment returns EnrollmentData which contains the private key.
        
        // Let's use reflection to access savePrivateKey/loadPrivateKey for precise testing
        // or just trust performEnrollment if we can construct inputs.
        
        // Construct dummy inputs
        val idCardInfo = com.example.ekycsimulate.ui.auth.IdCardInfo(
            idNumber = "123456789",
            fullName = "Test User",
            dob = "01/01/1990",
            origin = "Test Place",
            address = "Test Address"
        )
        
        val enrollmentData = enrollmentManager.performEnrollment(
            idCardInfo = idCardInfo,
            fullName = "Test User",
            phoneNumber = "0987654321",
            address = "Test Address",
            faceImageApproval = 1
        )
        
        assertNotNull("Enrollment should return data", enrollmentData)
        val originalPrivateKey = enrollmentData.privateKey
        
        // 2. Verify the key is saved in SharedPreferences as JSON (Encrypted)
        val prefs = context.getSharedPreferences("zkp_keys", Context.MODE_PRIVATE)
        val storedValue = prefs.getString("private_key", null)
        assertNotNull("Private key should be saved", storedValue)
        assertTrue("Stored value should be JSON (start with {)", storedValue!!.trim().startsWith("{"))
        
        // 3. Verify we can load it back
        // We can use performVerification which calls loadPrivateKey
        val verificationPayload = enrollmentManager.performVerification("test-session-id")
        assertNotNull("Verification should succeed", verificationPayload)
        
        // Verify the public key matches
        val publicKeyFromVerification = verificationPayload!!.publicKey
        val publicKeyFromEnrollment = enrollmentData.payload.publicKey
        assertEquals("Public keys should match", publicKeyFromEnrollment, publicKeyFromVerification)
    }

    @Test
    fun testPIIEncryption() {
        val plaintext = "Sensitive PII Data"
        val encryptedData = PIIEncryption.encrypt(plaintext, PIIEncryption.PII_KEY_ALIAS)
        
        assertNotEquals("Ciphertext should not be plaintext", plaintext, encryptedData.ciphertext)
        assertNotNull("IV should not be null", encryptedData.iv)
        
        val decrypted = PIIEncryption.decrypt(encryptedData, PIIEncryption.PII_KEY_ALIAS)
        assertEquals("Decrypted text should match original", plaintext, decrypted)
    }
    
    @Test
    fun testKeyWrappingEncryption() {
        val plaintext = "SecretKeyHex123456"
        val encryptedData = PIIEncryption.encrypt(plaintext, PIIEncryption.ZKP_KEY_ALIAS)
        
        assertNotEquals("Ciphertext should not be plaintext", plaintext, encryptedData.ciphertext)
        
        val decrypted = PIIEncryption.decrypt(encryptedData, PIIEncryption.ZKP_KEY_ALIAS)
        assertEquals("Decrypted text should match original", plaintext, decrypted)
    }
}
