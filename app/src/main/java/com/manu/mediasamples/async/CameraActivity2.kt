package com.manu.mediasamples.async

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.manu.mediasamples.MainActivity
import com.manu.mediasamples.R
import com.manu.mediasamples.databinding.ActivityCameraBinding
import com.manu.mediasamples.util.getPreviewOutputSize
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * @Desc: Camera2
 * @Author: jzman
 */
class CameraActivity2 : AppCompatActivity(), View.OnClickListener {

    private lateinit var binding: ActivityCameraBinding
    private lateinit var mCameraId: String

    private lateinit var mCaptureRequestBuild: CaptureRequest.Builder
    private lateinit var mExecutor: ExecutorService

    private lateinit var mCameraDevice: CameraDevice
    private lateinit var mCameraCaptureSession: CameraCaptureSession
    private lateinit var mSurfaceTexture: SurfaceTexture
    private lateinit var mSurface: Surface
    private lateinit var previewSize: Size

    private var mCameraThread = HandlerThread("CameraThread").apply { start() }
    private var mCameraHandler = Handler(mCameraThread.looper)
    private var isRecordState = false;

    /**
     * 获取CameraManager
     */
    private val mCameraManager: CameraManager by lazy {
        application.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    /**
     * 获取CameraCharacteristics
     */
    private val mCameraCharacteristics: CameraCharacteristics by lazy {
        mCameraManager.getCameraCharacteristics(mCameraId)
    }

    companion object {
        private const val TAG = "CameraActivity"
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnRecord.setOnClickListener(this)
        binding.btnStop.setOnClickListener(this)
        mCameraId = intent.getStringExtra(MainActivity.CAMERA_ID).toString()
        mExecutor = Executors.newSingleThreadExecutor()
        binding.textureView.surfaceTextureListener = TextureListener()
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btnRecord -> startRecord()
            R.id.btnStop -> stop()
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            mCameraDevice.close()
        } catch (exc: Throwable) {
            Log.e(TAG, "Error closing camera", exc)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mCameraThread.quitSafely()
        mExecutor.shutdownNow()
    }

    @SuppressLint("MissingPermission", "Recycle")
    private fun initCamera() = lifecycleScope.launch {
        Log.i(TAG, "initCamera")
        // 打开Camera
        mCameraDevice = openCamera()
        // 初始化ImageReader
        val size = Size(1080, 1920)
        mSurfaceTexture.setDefaultBufferSize(size.width, size.height)
        // 创建CameraCaptureSession
        mCameraCaptureSession = createCaptureSession()
    }

    /**
     * 打开Camera
     */
    @SuppressLint("MissingPermission")
    private suspend fun openCamera(): CameraDevice = suspendCancellableCoroutine { cont ->
        mCameraManager.openCamera(mCameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                // 设备开启
                Log.i(TAG, "onOpened")
                cont.resume(camera)
            }

            override fun onDisconnected(camera: CameraDevice) {
                // 设备断开
                Log.i(TAG, "onDisconnected")
                finish()
            }

            override fun onError(camera: CameraDevice, error: Int) {
                // 意外错误
                Log.i(TAG, "onError:$error")
                val msg = when (error) {
                    ERROR_CAMERA_DEVICE -> "Fatal (device)"
                    ERROR_CAMERA_DISABLED -> "Device policy"
                    ERROR_CAMERA_IN_USE -> "Camera in use"
                    ERROR_CAMERA_SERVICE -> "Fatal (service)"
                    ERROR_MAX_CAMERAS_IN_USE -> "Maximum cameras in use"
                    else -> "Unknown"
                }
                val exc = RuntimeException("Camera  error: ($error) $msg")
                Log.e(TAG, exc.message, exc)
                if (cont.isActive) cont.resumeWithException(exc)
            }

            override fun onClosed(camera: CameraDevice) {
                super.onClosed(camera)
                // 设备关闭，CameraDevice的close方法触发
                Log.i(TAG, "onClosed")
            }
        }, mCameraHandler)
    }

    /**
     * 创建CaptureSession
     */
    @RequiresApi(Build.VERSION_CODES.P)
    private suspend fun createCaptureSession(): CameraCaptureSession = suspendCoroutine { cont ->
        val outputs = mutableListOf<OutputConfiguration>()
        outputs.add(OutputConfiguration(mSurface))
        outputs.add(OutputConfiguration(AsyncEncodeManager.getSurface()))
        val sessionConfiguration = SessionConfiguration(
            SessionConfiguration.SESSION_REGULAR,
            outputs, mExecutor, object : CameraCaptureSession.StateCallback() {

                override fun onActive(session: CameraCaptureSession) {
                    super.onActive(session)
                    // 会话主动处理Capture Request
                    Log.i(TAG, "onActive")
                }

                override fun onReady(session: CameraCaptureSession) {
                    super.onReady(session)
                    // 每次会话没有更多的Capture Request时调用
                    // Camera完成自身配置没有Capture Request提交至会话也会调用
                    // 会话完成所有的Capture Request会回调
                    Log.i(TAG, "onReady")
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    val exc = RuntimeException("Camera $mCameraId session configuration failed")
                    Log.e(TAG, exc.message, exc)
                    cont.resumeWithException(exc)
                }

                override fun onConfigured(session: CameraCaptureSession) {
                    // Camera完成自身配置，会话开始处理请求
                    // Capture Request已经在会话中排队，则立即调用onActive
                    // 没有提交Capture Request则调用onReady
                    Log.i(TAG, "onConfigured")
                    cont.resume(session)
                }
            })
        mCameraDevice.createCaptureSession(sessionConfiguration)
    }

    /**
     * 开启录制
     */
    private fun startRecord() = lifecycleScope.launch {
        Snackbar
            .make(
                binding.container,
                getString(if (isRecordState) R.string.record_now else R.string.record_start),
                Snackbar.LENGTH_LONG
            ).show()
        if (isRecordState) return@launch
        AsyncEncodeManager.init(previewSize.width, previewSize.height)
        // 创建CameraCaptureSession
        mCameraCaptureSession = createCaptureSession()

        // 添加预览的Surface和生成Image的Surface
        mCaptureRequestBuild = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        val sur = AsyncEncodeManager.getSurface()
        mCaptureRequestBuild.addTarget(sur)
        mCaptureRequestBuild.addTarget(mSurface)

        // 设置各种参数
        mCaptureRequestBuild.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, 1) // 视频稳定功能是否激活
        // 发送CaptureRequest
        mCameraCaptureSession.setRepeatingRequest(
            mCaptureRequestBuild.build(),
            null,
            mCameraHandler
        )

        // 开始编码
        AsyncEncodeManager.startEncode()
        isRecordState = true
    }

    /**
     * 关闭CaptureSession
     */
    private fun closeCaptureSession() {
        mCameraCaptureSession.stopRepeating()
        mCameraCaptureSession.close()
    }

    private fun stop() {
        Snackbar
            .make(
                binding.container,
                getString(if (isRecordState) R.string.record_end else R.string.record_none),
                Snackbar.LENGTH_LONG
            ).show()
        if (!isRecordState) return
        AsyncEncodeManager.stopEncode()
        isRecordState = false
    }

    /**
     * TextureView关联的surfaceTexture可用时通知的回调
     */
    private inner class TextureListener : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
            // surfaceTexture的缓冲区大小变化时调用
            Log.i(TAG, "onSurfaceTextureSizeChanged")
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
            // surfaceTexture的updateTextImage更新指定的surfaceTexture时调用
            Log.i(TAG, "onSurfaceTextureUpdated")
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
            // surfaceTexture销毁的时候调用
            // 返回true表示surfaceTexture将不进行渲染，false表示需调用surfaceTexture的release方法进行destroy
            Log.i(TAG, "onSurfaceTextureDestroyed")
            return true
        }

        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            // surfaceTexture可用的时候调用
            Log.i(TAG, "onSurfaceTextureAvailable")

            mSurfaceTexture = binding.textureView.surfaceTexture!!
            mSurface = Surface(mSurfaceTexture)

            // 获取合适的预览大小
            previewSize = getPreviewOutputSize(
                binding.textureView.display,
                mCameraCharacteristics,
                mSurfaceTexture::class.java
            )

            AsyncEncodeManager.init(previewSize.width, previewSize.height)
            binding.textureView.setAspectRatio(previewSize.width, previewSize.height)
            initCamera()
        }
    }

    /**
     * ImageReader获取的帧数据的封装
     */
    class FrameData(var buffer: ByteArray, var timeStamp: Long)
}


