package com.manu.mediasamples

import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.manu.mediasamples.samples.async.AsyncActivity
import com.manu.mediasamples.databinding.ActivityMainBinding
import com.manu.mediasamples.samples.audio.AudioTrackActivity
import com.manu.mediasamples.samples.frame.ImageActivity
import com.manu.mediasamples.samples.opengl.OpenGLActivity
import com.manu.mediasamples.samples.record.RecordActivity
import com.manu.mediasamples.samples.sync.SyncActivity

/**
 * @Desc: MainActivity
 * @Author: jzman
 */
class MainActivity : AppCompatActivity(), View.OnClickListener{
    private lateinit var binding: ActivityMainBinding

    companion object {
        private const val TAG = "main_activity"
        const val CAMERA_ID = "camera_id"

        /**  Camera id */
        private const val CAMERA_TYPE  = CameraCharacteristics.LENS_FACING_BACK.toString()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnImageReader.setOnClickListener(this)
        binding.btnSync.setOnClickListener(this)
        binding.btnAsync.setOnClickListener(this)
        binding.btnRecord.setOnClickListener(this)
        binding.btnAudioTrack.setOnClickListener(this)
        binding.btnOpenGL.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when(v?.id){
            R.id.btnSync -> startSampleActivity(CAMERA_TYPE,SyncActivity::class.java)
            R.id.btnAsync -> startSampleActivity(CAMERA_TYPE,AsyncActivity::class.java)
            R.id.btnImageReader -> startSampleActivity(CAMERA_TYPE,ImageActivity::class.java)
            R.id.btnRecord -> startSampleActivity(CAMERA_TYPE,RecordActivity::class.java)
            R.id.btnAudioTrack -> startSampleActivity(CAMERA_TYPE,AudioTrackActivity::class.java)
            R.id.btnOpenGL -> startSampleActivity(OpenGLActivity::class.java)
        }
    }

    private fun startSampleActivity(cameraId: String, clazz:Class<*>) {
        val intent = Intent(this, clazz)
        intent.putExtra(CAMERA_ID, cameraId)
        startActivity(intent)
    }

    private fun startSampleActivity(clazz:Class<*>) {
        val intent = Intent(this, clazz)
        startActivity(intent)
    }
}