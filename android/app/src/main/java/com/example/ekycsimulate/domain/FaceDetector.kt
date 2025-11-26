package com.example.ekycsimulate.domain

import android.graphics.Bitmap
import android.graphics.Rect

/**
 * Interface for Face Detection to allow swapping models easily.
 */
interface FaceDetector {
    /**
     * Detect faces in the given bitmap.
     * @return List of detected faces (or empty if none)
     */
    suspend fun detect(bitmap: Bitmap): List<FaceResult>
}

/**
 * Standardized result for face detection.
 */
data class FaceResult(
    val bounds: Rect,
    val headEulerAngleY: Float, // Left/Right turn
    val headEulerAngleZ: Float, // Tilt
    val leftEyeOpenProbability: Float?,
    val rightEyeOpenProbability: Float?,
    val smilingProbability: Float?
)
