package com.manu.mediasamples

import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.manu.mediasamples.samples.async.AsyncActivity
import com.manu.mediasamples.databinding.ActivityMainBinding
import com.manu.mediasamples.samples.frame.ImageActivity
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
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnImageReader.setOnClickListener(this)
        binding.btnSync.setOnClickListener(this)
        binding.btnAsync.setOnClickListener(this)
        binding.btnRecord.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when(v?.id){
            R.id.btnSync -> startCameraActivity(CAMERA_TYPE,SyncActivity::class.java)
            R.id.btnAsync -> startCameraActivity(CAMERA_TYPE,AsyncActivity::class.java)
            R.id.btnImageReader -> startCameraActivity(CAMERA_TYPE,ImageActivity::class.java)
            R.id.btnRecord -> startCameraActivity(CAMERA_TYPE,RecordActivity::class.java)
        }
    }

    private fun startCameraActivity(cameraId: String, clazz:Class<*>) {
        val intent = Intent(this, clazz)
        intent.putExtra(CAMERA_ID, cameraId)
        startActivity(intent)
    }
}