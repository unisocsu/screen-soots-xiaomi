package com.example.screentool

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val REQ = 1000
    private lateinit var mgr: MediaProjectionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val btn = Button(this).apply {
            text = "Start Capture"
            setOnClickListener {
                startActivityForResult(mgr.createScreenCaptureIntent(), REQ)
            }
        }

        setContentView(btn)

        mgr = getSystemService(Context.MEDIA_PROJECTION_SERVICE)
                as MediaProjectionManager
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQ && resultCode == Activity.RESULT_OK) {
            val i = Intent(this, ScreenService::class.java)
            i.putExtra("code", resultCode)
            i.putExtra("data", data)
            startForegroundService(i)
        }
    }
}