package br.com.silas.camerax

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraX(private val context: AppCompatActivity, private val viewFinder: PreviewView) {

    private var imageCapture: ImageCapture? = null

    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    init {
        imageCapture = ImageCapture.Builder()
            .build()
    }

     fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val photoFile = createImageFile(context)

        val outputOptions = ImageCapture
            .OutputFileOptions
            .Builder(photoFile)
            .build()

        imageCapture.takePicture(
            outputOptions, ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    val msg = "Photo capture succeeded: $savedUri"
                    Log.d(TAG, msg)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exception.message}", exception)
                }

            }
        )
    }

    fun setFlash(enableFlash: Boolean) {
        imageCapture = if (enableFlash) {
            ImageCapture.Builder()
                .setFlashMode(ImageCapture.FLASH_MODE_ON)
                .build()
        } else {
            ImageCapture.Builder()
                .setFlashMode(ImageCapture.FLASH_MODE_OFF)
                .build()
        }



        startCamera()
    }

     fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({

            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(
                    context, cameraSelector, preview, imageCapture
                )
            } catch (ex: Exception) {
                Log.d(TAG, "Use case binding failed", ex)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun onDestroy() {
        cameraExecutor.shutdown()
    }

    private fun createImageFile(context: Context): File {

        val timesTamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale("PT", "BR")).format(Date())
        UUID.randomUUID().toString().plus(timesTamp)

        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "${UUID.randomUUID().toString().plus(timesTamp)}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        )

    }

    companion object {
        private val TAG = CameraX::class.java.simpleName
    }
}