package com.example.ekycsimulate.data

import android.graphics.Bitmap
import com.example.ekycsimulate.domain.FaceDetector
import com.example.ekycsimulate.domain.FaceResult
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.tasks.await

/**
 * Implementation of FaceDetector using Google ML Kit.
 */
class MLKitFaceDetector : FaceDetector {

    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL) // For eyes open/smiling
        .enableTracking()
        .build()

    private val detector = FaceDetection.getClient(options)

    override suspend fun detect(bitmap: Bitmap): List<FaceResult> {
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val faces = detector.process(image).await()
            
            faces.map { face ->
                FaceResult(
                    bounds = face.boundingBox,
                    headEulerAngleY = face.headEulerAngleY,
                    headEulerAngleZ = face.headEulerAngleZ,
                    leftEyeOpenProbability = face.leftEyeOpenProbability,
                    rightEyeOpenProbability = face.rightEyeOpenProbability,
                    smilingProbability = face.smilingProbability
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
