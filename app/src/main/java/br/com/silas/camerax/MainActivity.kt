package br.com.silas.camerax

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Bundle
import android.os.Environment
import android.util.DisplayMetrics
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageCapture
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity(), SurfaceHolder.Callback {
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraX: CameraX
    private var enabledFlash = false
    private lateinit var previewView: PreviewView

    var holder: SurfaceHolder? = null
    var surfaceView: SurfaceView? = null
    var canvas: Canvas? = null
    var paint: Paint? = null
    var cameraHeight = 0
    var cameraWidth:Int = 0
    var xOffset:Int = 0
    var yOffset:Int = 0
    var boxWidth:Int = 0
    var boxHeight:Int = 0

    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        supportActionBar!!.hide()
        previewView = findViewById(R.id.viewFinder)
        cameraX = CameraX(this, previewView)

        if(allPermissionsGranted()) {
            cameraX.startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        surfaceView = findViewById(R.id.overlay);
        surfaceView!!.setZOrderOnTop(true);
        holder = surfaceView!!.holder;
        holder!!.setFormat(PixelFormat.TRANSPARENT);
        holder!!.addCallback(this);

        findViewById<Button>(R.id.camera_capture_button).setOnClickListener { cameraX.takePhoto() }
        findViewById<ImageButton>(R.id.button_flash).setOnClickListener {
            if (enabledFlash) {
                enabledFlash = false
                cameraX.setFlash(enabledFlash)
                return@setOnClickListener
            }
            enabledFlash = true
            cameraX.setFlash(enabledFlash)
        }

        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply {
                mkdirs()
            }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
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

    override fun onDestroy() {
        super.onDestroy()
        cameraX.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                cameraX.startCamera()
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }


    private fun drawFocusRect(color: Int) {
        val displaymetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displaymetrics)
        val height: Int = previewView.height
        val width: Int = previewView.width
        cameraHeight = height
        cameraWidth = width
        val left: Int
        val right: Int
        val top: Int
        val bottom: Int
        var diameter: Int
        diameter = width
        if (height < width) {
            diameter = height
        }
        val offset = (0.05 * diameter).toInt()
        diameter -= offset
        canvas = holder!!.lockCanvas()
        canvas!!.drawColor(0, PorterDuff.Mode.CLEAR)
        //border's properties
        paint = Paint()
        paint!!.style = Paint.Style.STROKE
        paint!!.color = color
        paint!!.strokeWidth = 5f
        left = width / 2 - diameter / 3
        top = height / 2 - diameter / 3
        right = width / 2 + diameter / 3
        bottom = height / 2 + diameter / 3
        xOffset = left
        yOffset = top
        boxHeight = bottom - top
        boxWidth = right - left
        //Changing the value of x in diameter/x will change the size of the box ; inversely proportionate to x
        canvas!!.drawRect(110f, 300f, 950f, 1600f, paint!!)
//        canvas!!.drawRect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), paint!!)
        holder!!.unlockCanvasAndPost(canvas)
    }


    companion object {
        private const val TAG = "CameraXBasic"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {

    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        drawFocusRect(Color.parseColor("#b3dabb"));
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {

    }
}
