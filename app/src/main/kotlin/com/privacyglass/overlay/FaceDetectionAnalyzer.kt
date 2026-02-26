package com.privacyglass.overlay

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

/**
 * CameraX ImageAnalysis.Analyzer using ML Kit face detection.
 * Processes every 3rd frame to reduce battery usage.
 * Calls [onFaceCountChanged] when the detected face count changes.
 */
class FaceDetectionAnalyzer(
    private val onFaceCountChanged: (Int) -> Unit
) : ImageAnalysis.Analyzer {

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setMinFaceSize(0.12f)
            .build()
    )

    private var lastCount = -1
    private var frameCounter = 0

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        // Skip 2 out of 3 frames for battery efficiency
        if (++frameCounter % 3 != 0) { imageProxy.close(); return }

        val mediaImage = imageProxy.image ?: run { imageProxy.close(); return }
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        detector.process(image)
            .addOnSuccessListener { faces ->
                val count = faces.size
                if (count != lastCount) {
                    lastCount = count
                    onFaceCountChanged(count)
                }
            }
            .addOnFailureListener { /* silently ignore */ }
            .addOnCompleteListener { imageProxy.close() }
    }
}
