package com.example.ekycsimulate.data

import android.graphics.Bitmap
import android.graphics.Rect
import com.example.ekycsimulate.domain.FaceDetector
import com.example.ekycsimulate.domain.FaceResult
import kotlinx.coroutines.delay

/**
 * Fake implementation of FaceDetector for testing on Emulator.
 * Always returns a successful face detection.
 */
class FakeFaceDetector : FaceDetector {
    override suspend fun detect(bitmap: Bitmap): List<FaceResult> {
        // Simulate processing delay
        delay(500)
        
        // Return a perfect face
        return listOf(
            FaceResult(
                bounds = Rect(0, 0, 100, 100),
                headEulerAngleY = 0f, // Looking straight
                headEulerAngleZ = 0f, // No tilt
                leftEyeOpenProbability = 0.9f,
                rightEyeOpenProbability = 0.9f,
                smilingProbability = 0.5f
            )
        )
    }
}
