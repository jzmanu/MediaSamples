package com.manu.mediasamples

import android.Manifest
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.manu.mediasamples.async.CameraActivity2
import com.manu.mediasamples.databinding.ActivityMainBinding
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.PermissionRequest

/**
 * @Desc: MainActivity
 * @Author: jzman
 */
class MainActivity : AppCompatActivity(), View.OnClickListener,EasyPermissions.PermissionCallbacks  {
    private lateinit var binding: ActivityMainBinding

    companion object {
        private const val TAG = "main_activity"
        const val CAMERA_ID = "camera_id"
        /** Camera权限请求Code */
        private const val REQUEST_CODE_CAMERA = 0

        /**  Camera id */
        private const val CAMERA_TYPE  = CameraCharacteristics.LENS_FACING_BACK.toString()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnCamera2.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btnCamera2 -> requestPermission()
        }
    }

    private fun startCameraActivity(cameraId: String) {
        val intent = Intent(this, CameraActivity2::class.java)
        intent.putExtra(CAMERA_ID, cameraId)
        startActivity(intent)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        Log.d(TAG, "onPermissionsGranted")
        startCameraActivity(CAMERA_TYPE)
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        Log.d(TAG, "onPermissionsDenied")
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)){
            AppSettingsDialog.Builder(this).build().show();
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE -> {
                startCameraActivity(CAMERA_TYPE)
            }
        }
    }

    private fun requestPermission() {
        val permissions = arrayOf(Manifest.permission.CAMERA)
        if (EasyPermissions.hasPermissions(this, *permissions)) {
            startCameraActivity(CAMERA_TYPE)
        }else{
            EasyPermissions.requestPermissions(
                PermissionRequest.Builder(this, REQUEST_CODE_CAMERA, *permissions)
                    .setNegativeButtonText(getString(R.string.permission_negative))
                    .setPositiveButtonText(getString(R.string.permission_positive))
                    .setRationale(getString(R.string.request_camera_permission))
                    .build()
            )
        }
    }
}