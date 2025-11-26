package com.example.ekycsimulate.utils

import java.security.MessageDigest
import kotlin.random.Random

/**
 * Manages Zero-Knowledge Proof (ZKP) operations based on a Hash-based Challenge-Response protocol.
 * Note: This is a simplified ZKP as described in the reference PDF, primarily for demonstration.
 */
object ZKPManager {

    /**
     * Generates a commitment to the secret (e.g., password or ID).
     * Commitment = SHA256(secret)
     */
    fun generateCommitment(secret: String): String {
        return hash(secret)
    }

    /**
     * Generates a ZKP Proof consisting of a Challenge and a Response.
     * Challenge = Random Nonce
     * Response = Hash(Challenge + Secret + Commitment)
     *
     * @param secret The secret knowledge (e.g., ID number).
     * @return Pair<Challenge, Response>
     */
    fun generateZKPProof(secret: String): Pair<String, String> {
        // 1. Generate Commitment (In a real scenario, this might be done earlier or stored)
        val commitment = generateCommitment(secret)

        // 2. Generate a random challenge (nonce)
        val challenge = Random.nextInt(1000, 9999).toString()

        // 3. Compute Response
        // Response = Hash(Challenge + Secret + Commitment)
        val rawResponse = challenge + secret + commitment
        val response = hash(rawResponse)

        return Pair(challenge, response)
    }

    /**
     * Verifies the ZKP Proof.
     *
     * @param secret The secret (Verifier must know this in this simplified protocol).
     * @param commitment The commitment to the secret.
     * @param challenge The challenge used in the proof.
     * @param response The response provided by the prover.
     * @return True if valid, False otherwise.
     */
    fun verifyZKPProof(secret: String, commitment: String, challenge: String, response: String): Boolean {
        // Recompute the expected response
        val rawExpected = challenge + secret + commitment
        val expectedResponse = hash(rawExpected)

        return expectedResponse == response
    }

    private fun hash(input: String): String {
        val bytes = input.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
}
