package com.example.screentool

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var projectionManager: MediaProjectionManager

    private val captureLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

            if (result.resultCode == Activity.RESULT_OK) {
                val intent = Intent(this, ScreenService::class.java).apply {
                    putExtra("code", result.resultCode)
                    putExtra("data", result.data)
                    putExtra("mode", "screenshot") // או "record"
                }
                startForegroundService(intent)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        projectionManager =
            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        val btnShot = Button(this).apply {
            text = "📸 Screenshot"
            setOnClickListener {
                val intent = projectionManager.createScreenCaptureIntent()
                captureLauncher.launch(intent)
            }
        }

        val btnRec = Button(this).apply {
            text = "🎥 Record"
            setOnClickListener {
                val intent = projectionManager.createScreenCaptureIntent()
                intent.putExtra("mode", "record")
                captureLauncher.launch(intent)
            }
        }

        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            addView(btnShot)
            addView(btnRec)
        }

        setContentView(layout)
    }
}
