package com.manu.mediasamples.sync

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import android.util.Log
import android.view.Surface
import com.manu.mediasamples.app.MediaApplication
import java.io.IOException

/**
 * @Desc:EncodeManager
 * @Author: jzman
 */
object EncodeManager {
    private const val TAG = "EncodeManager"
    private const val MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC
    private const val COLOR_FORMAT_SURFACE =
        MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
    private lateinit var mMediaCodec: MediaCodec
    private lateinit var mMediaMuxer: MediaMuxer

    /** 用作数据流输入的Surface */
    private lateinit var mSurface: Surface

    /** 编码线程 */
    private var mEncodeThread: SyncEncodeThread? = null

    /**
     * 初始化
     */
    fun init(width: Int, height: Int) {
        initCodec(width, height)
        initMuxer()
        mEncodeThread =
            SyncEncodeThread(
                mMediaCodec,
                mMediaMuxer
            )
    }

    /**
     * 获取用作输入流输入的Surface
     */
    fun getSurface(): Surface {
        return mSurface
    }

    /**
     * 开始编码
     */
    fun startEncode() {
        Log.d(TAG, "startEncode")
        if (mEncodeThread == null) error("not call init method.")
        mEncodeThread?.start()
    }

    /**
     * 结束编码
     */
    fun stopEncode() {
        Log.d(TAG, "stopEncode")
        if (mEncodeThread == null) error("not call init method.")
        mEncodeThread?.isStop = true
        mEncodeThread = null
    }

    /**
     * 初始化MediaCodec
     */
    private fun initCodec(width: Int, height: Int) {
        Log.i(TAG, "initCodec start")
        try {
            // 创建MediaCodec
            mMediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            // 参数设置
            val mediaFormat = MediaFormat.createVideoFormat(MIME_TYPE, width, height)
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                COLOR_FORMAT_SURFACE
            ) // 颜色采样格式
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, width * height * 4) // 比特率
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30) // 帧率
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1) // I帧间隔

            mediaFormat.setInteger(
                MediaFormat.KEY_PROFILE,
                MediaCodecInfo.CodecProfileLevel.AVCProfileHigh
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mediaFormat.setInteger(
                    MediaFormat.KEY_LEVEL,
                    MediaCodecInfo.CodecProfileLevel.AVCLevel31
                )
            }
            // 配置状态
            mMediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            // 创建Surface作为MediaCodec的输入，createInputSurface只能在configure与start之间调用创建Surface
            mSurface = mMediaCodec.createInputSurface()
        } catch (e: Exception) {
            Log.i(TAG, "initCodec fail:${e.message} ")
            e.printStackTrace()
        }
    }

    /**
     * 初始化MediaMuxer
     */
    private fun initMuxer() {
        try {
            val path = "${MediaApplication.context.filesDir}/test.mp4"
            mMediaMuxer = MediaMuxer(path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        } catch (e: IOException) {
            Log.e(
                TAG,
                "initMuxer fail: ${e.message}"
            )
        }
    }
}