package com.manu.mediasamples.frame

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.ImageReader
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
import com.google.android.material.snackbar.Snackbar
import com.manu.mediasamples.MainActivity
import com.manu.mediasamples.R
import com.manu.mediasamples.databinding.ActivityCameraBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * @Desc: Camera2+ImageReader
 * @Author: jzman
 */
class CameraActivity3 : AppCompatActivity(), View.OnClickListener {

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

    private lateinit var mImageReader: ImageReader
    private var mImageThread = HandlerThread("ImageThread").apply { start() }
    private var mImageHandler = Handler(mImageThread.looper)

    private var isRecordState = false
    private var isCameraState = false;

    private var mMediaNative = IMediaNative()

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

    init {
        System.loadLibrary("native-yuv-to-buffer-lib")
    }

    companion object {
        private const val TAG = "CameraActivity3"

        /** 渲染缓冲区中最大的图像数量 */
        private const val IMAGE_BUFFER_SIZE: Int = 3
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
        previewSize = Size(1920, 1080)
        binding.textureView.setAspectRatio(previewSize.width, previewSize.height)
        binding.textureView.surfaceTextureListener = TextureListener()
    }

    @RequiresApi(Build.VERSION_CODES.N)
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

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onDestroy() {
        super.onDestroy()
        startRecord()
        mCameraThread.quitSafely()
        mExecutor.shutdownNow()
    }

    @SuppressLint("MissingPermission", "Recycle")
    private fun initCamera() {
        Log.i(TAG, "initCamera")
        // 打开Camera
        openCamera()
    }

    /**
     * 打开Camera
     */
    @SuppressLint("MissingPermission")
    private fun openCamera() {
        mCameraManager.openCamera(mCameraId, object : CameraDevice.StateCallback() {
            @RequiresApi(Build.VERSION_CODES.P)
            override fun onOpened(camera: CameraDevice) {
                // 设备开启
                Log.i(TAG, "onOpened")
                mCameraDevice = camera
                isCameraState = true
            }

            override fun onDisconnected(camera: CameraDevice) {
                // 设备断开
                Log.i(TAG, "onDisconnected")
                isCameraState = false
                finish()
            }

            override fun onError(camera: CameraDevice, error: Int) {
                // 意外错误
                Log.i(TAG, "onError:$error")
                isCameraState = false
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
            }

            override fun onClosed(camera: CameraDevice) {
                super.onClosed(camera)
                // 设备关闭，CameraDevice的close方法触发
                Log.i(TAG, "onClosed")
                isCameraState = false
            }
        }, mCameraHandler)
    }

    /**
     * 开启录制
     */
    @RequiresApi(Build.VERSION_CODES.N)
    private fun startRecord() {

        if (!isCameraState) {
            Snackbar.make(
                binding.container,
                getString(R.string.camera_error),
                Snackbar.LENGTH_LONG
            ).show()
            return
        }

        Snackbar.make(
            binding.container,
            getString(if (isRecordState) R.string.record_now else R.string.record_start),
            Snackbar.LENGTH_LONG
        ).show()
        if (isRecordState) return

        mSurfaceTexture = binding.textureView.surfaceTexture!!
        mSurface = Surface(mSurfaceTexture)


        // 初始化ImageReader
        mSurfaceTexture.setDefaultBufferSize(previewSize.width, previewSize.height)
        mImageReader = ImageReader.newInstance(
            previewSize.width,
            previewSize.height,
            ImageFormat.YUV_420_888,
            IMAGE_BUFFER_SIZE
        )

        // 添加预览的Surface和生成Image的Surface
        mCaptureRequestBuild = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        mCaptureRequestBuild.addTarget(mSurface)
        mCaptureRequestBuild.addTarget(mImageReader.surface)
        // 通知Image可用的回调，对ImageReader可以的每个帧都触发回调
        mImageReader.setOnImageAvailableListener(ImageAvailableListener(), mImageHandler)

        val outputs = mutableListOf<OutputConfiguration>()
        outputs.add(OutputConfiguration(mSurface))
        outputs.add(OutputConfiguration(mImageReader.surface))
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
                }

                override fun onConfigured(session: CameraCaptureSession) {
                    // Camera完成自身配置，会话开始处理请求
                    // Capture Request已经在会话中排队，则立即调用onActive
                    // 没有提交Capture Request则调用onReady
                    Log.i(TAG, "onConfigured")
                    mCameraCaptureSession = session

                    // 设置各种参数
                    mCaptureRequestBuild.set(
                        CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, // 视频稳定功能是否激活
                        1
                    )
                    // 发送CaptureRequest
                    mCameraCaptureSession.setRepeatingRequest(
                        mCaptureRequestBuild.build(),
                        null,
                        mCameraHandler
                    )
                    AsyncInputEncodeManager.init(previewSize.width, previewSize.height)
                    AsyncInputEncodeManager.startEncode()
                    isRecordState = true
                }
            })
        mCameraDevice.createCaptureSession(sessionConfiguration)
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
        AsyncInputEncodeManager.stopEncode()
        closeCaptureSession()
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

            // 获取合适的预览大小
//            previewSize = getPreviewOutputSize(
//                    binding.textureView.display,
//                    mCameraCharacteristics,
//                    mSurfaceTexture::class.java
//            )

            initCamera()
        }
    }

    /**
     *  通知Image可用的回调，对ImageReader可以的每个帧都触发回调
     */
    private inner class ImageAvailableListener : ImageReader.OnImageAvailableListener {
        override fun onImageAvailable(reader: ImageReader) {
            Log.i(TAG, "onImageAvailable > isStop：" + AsyncInputEncodeManager.isStop())
            // 实时获取最新的Image
            val image = reader.acquireLatestImage() ?: return
            Log.i(
                TAG,
                "onImageAvailable > planes:" + image.planes.size
            )
//
//            Log.i(TAG, "image plane size:${image.planes.size}")
//            Log.i(TAG, "image width:${image.width}")
//            Log.i(TAG, "image height:${image.height}")
//            Log.i(TAG, "image format:${image.format}")
//            Log.i(TAG, "image timestamp:${image.timestamp}") // 时间戳，单位纳秒
//
//            image.planes.forEachIndexed { index, plane ->
//                Log.i(TAG, "plane $index start")
//                Log.i(TAG, "buffer capacity: ${plane.buffer.capacity()}")
//                Log.i(TAG, "pixelStride: ${plane.pixelStride}") // 一行像素中两个连续像素值之间的距离，单位字节
//                Log.i(TAG, "rowStride: ${plane.rowStride}") // 两个连续像素行的起点之间的距离，单位字节
//                Log.i(TAG, "plane $index end")
//            }

            // 获取YUV数据
            val byteBufferY = image.planes[0].buffer
            val byteBufferYSize = byteBufferY.remaining()
            val byteBufferU = image.planes[1].buffer
            val byteBufferUSize = byteBufferU.remaining()
            val byteBufferV = image.planes[2].buffer
            val byteBufferVSize = byteBufferV.remaining()

            // 填充YUV数据
            val mYUVByteArray = ByteArray(byteBufferYSize + byteBufferUSize + byteBufferVSize)
            byteBufferY.get(mYUVByteArray, 0, byteBufferYSize)
            byteBufferU.get(mYUVByteArray, byteBufferYSize, byteBufferUSize)
            byteBufferV.get(mYUVByteArray, byteBufferYSize + byteBufferUSize, byteBufferVSize)
            if (!AsyncInputEncodeManager.isStop()) {
                AsyncInputEncodeManager.offer(mYUVByteArray,image.timestamp / 1000)
            }
            image.close()
            Log.i(TAG, "mYUVByteArray:${mYUVByteArray.size}")
        }
    }
}


