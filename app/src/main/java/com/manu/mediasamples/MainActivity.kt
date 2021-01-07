package com.manu.mediasamples

import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.manu.mediasamples.databinding.ActivityMainBinding

/**
 * MainActivity
 */
class MainActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnFront.setOnClickListener(this)
        binding.btnBack.setOnClickListener(this)

        startCameraActivity(CameraCharacteristics.LENS_FACING_BACK.toString())
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btnFront -> startCameraActivity(CameraCharacteristics.LENS_FACING_FRONT.toString())
            R.id.btnBack -> startCameraActivity(CameraCharacteristics.LENS_FACING_BACK.toString())
        }
    }

    private fun startCameraActivity(cameraId: String) {
        val intent = Intent(this, CameraActivity::class.java)
        intent.putExtra(CAMERA_ID, cameraId)
        startActivity(intent)
    }

    companion object {
        const val CAMERA_ID = "camera_id"
    }

}