package com.manu.mediasamples

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.PermissionRequest

class PermissionActivity : AppCompatActivity(),EasyPermissions.PermissionCallbacks {

    companion object {
        private const val TAG = "PermissionActivity"
        /** Camera权限请求Code */
        private const val REQUEST_CODE_CAMERA = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermission()
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        Log.d(TAG, "onPermissionsGranted")
        startMainActivity()
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        Log.d(TAG, "onPermissionsDenied")
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)){
            AppSettingsDialog.Builder(this).build().show();
        }else{
            finish()
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
                startMainActivity()
            }
        }
    }

    private fun requestPermission() {
        val permissions = arrayOf(Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO)
        if (EasyPermissions.hasPermissions(this, *permissions)) {
            startMainActivity()
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

    private fun startMainActivity(){
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}