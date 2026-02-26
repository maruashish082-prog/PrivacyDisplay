package com.privacydisplay

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

/**
 * CameraX ImageAnalysis.Analyzer that runs ML Kit face detection on each frame.
 * Calls [onFaceCountChanged] whenever the number of detected faces changes.
 * The privacy filter is triggered when more than 1 face is visible.
 */
class FaceDetectionAnalyzer(
    private val onFaceCountChanged: (faceCount: Int) -> Unit
) : ImageAnalysis.Analyzer {

    private val faceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setMinFaceSize(0.15f)   // Minimum face size (15% of frame)
            .build()
    )

    private var lastFaceCount = -1
    private var frameSkip = 0

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        // Process every 3rd frame to save battery
        frameSkip++
        if (frameSkip % 3 != 0) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image ?: run {
            imageProxy.close()
            return
        }

        val image = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        faceDetector.process(image)
            .addOnSuccessListener { faces ->
                val count = faces.size
                if (count != lastFaceCount) {
                    lastFaceCount = count
                    onFaceCountChanged(count)
                }
            }
            .addOnFailureListener { /* silently ignore */ }
            .addOnCompleteListener { imageProxy.close() }
    }
}
