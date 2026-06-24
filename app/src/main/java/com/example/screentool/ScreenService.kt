package com.example.screentool

import android.app.*
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Environment
import android.os.IBinder
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import java.io.File
import java.io.FileOutputStream

class ScreenService : Service() {

    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var recorder: MediaRecorder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val code = intent!!.getIntExtra("code", Activity.RESULT_OK)
        val data = intent.getParcelableExtra<Intent>("data")
        val mode = intent.getStringExtra("mode") ?: "screenshot"

        val mgr = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projection = mgr.getMediaProjection(code, data!!)

        startForeground(1, notification())

        if (mode == "record") startRecording()
        else takeScreenshot()

        return START_NOT_STICKY
    }

    // 📸 צילום מסך אמיתי
    private fun takeScreenshot() {

        val metrics = resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels

        val reader = ImageReader.newInstance(width, height, 0x1, 2)

        virtualDisplay = projection?.createVirtualDisplay(
            "shot",
            width, height, metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.surface, null, null
        )

        reader.setOnImageAvailableListener({
            val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener

            val plane = image.planes[0]
            val buffer = plane.buffer
            val pixelStride = plane.pixelStride
            val rowStride = plane.rowStride

            val bmp = Bitmap.createBitmap(
                width + (rowStride - pixelStride * width) / pixelStride,
                height,
                Bitmap.Config.ARGB_8888
            )

            bmp.copyPixelsFromBuffer(buffer)

            saveImage(bmp)

            image.close()
            stopSelf()

        }, null)
    }

    // 🎥 הקלטת מסך
    private fun startRecording() {

        val file = File(getExternalFilesDir(null), "record.mp4")

        recorder = MediaRecorder().apply {
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(file.absolutePath)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setVideoEncodingBitRate(5 * 1000000)
            setVideoFrameRate(30)
            setVideoSize(720, 1280)
            prepare()
        }

        val surface = recorder!!.surface

        virtualDisplay = projection?.createVirtualDisplay(
            "rec",
            720, 1280, resources.displayMetrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            surface, null, null
        )

        recorder?.start()
    }

    private fun saveImage(bitmap: Bitmap) {

        val name = "shot_${System.currentTimeMillis()}.png"

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ScreenTool")
        }

        val uri: Uri? =
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        uri?.let {
            contentResolver.openOutputStream(it).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out!!)
            }
        }
    }

    private fun notification(): Notification {

        val id = "screen_channel"

        if (android.os.Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel(
                id, "ScreenTool",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(ch)
        }

        return NotificationCompat.Builder(this, id)
            .setContentTitle("ScreenTool running")
            .setContentText("Capturing screen...")
            .setSmallIcon(android.R.drawable.presence_video_online)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        recorder?.stop()
        recorder?.release()
        virtualDisplay?.release()
        projection?.stop()
        super.onDestroy()
    }
}
