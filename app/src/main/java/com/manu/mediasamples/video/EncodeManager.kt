package com.manu.mediasamples.video

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import android.util.Log
import android.view.Surface
import com.manu.mediasamples.app.MediaApplication
import java.io.IOException
import kotlin.properties.Delegates

/**
 * @Desc: 编码线程
 * @Author: jzman
 */
class EncodeManager() : Thread() {
    private var isStop = false
    private var bufferInfo = MediaCodec.BufferInfo()
    private var pts: Long = 0
    private var mTrackIndex by Delegates.notNull<Int>()
    private var mStartMuxer = false

    private lateinit var mMediaCodec: MediaCodec
    private lateinit var mMediaMuxer: MediaMuxer

    /** 用作数据流输入的Surface */
    private lateinit var mSurface: Surface

    companion object {
        private const val TAG = "EncodeThread"

        private const val MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC
        private const val COLOR_FORMAT_YUV =
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
        private const val COLOR_FORMAT_SURFACE =
            MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
    }

    /**
     * 初始化MediaCodec
     */
    fun initCodec(width: Int, height: Int) {
        Log.i(TAG, "startEncode start")
        try {
            mMediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            val mediaFormat = MediaFormat.createVideoFormat(MIME_TYPE, width, height)
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, COLOR_FORMAT_SURFACE) // 颜色采样格式
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
            mMediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)

            mSurface = mMediaCodec.createInputSurface()
        } catch (e: Exception) {
            Log.i(TAG, "initCodec fail:${e.message} ")
            e.printStackTrace()
        }
    }

    /**
     * 初始化MediaMuxer
     */
    fun initMuxer() {
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

    /**
     * 开始编码
     */
    fun startEncode() {
        mStartMuxer = false
        start()
    }

    /**
     * 结束编码
     */
    fun stopEncode() {
        isStop = true
    }

    /**
     * 获取用作输入流输入的Surface
     */
    fun getSurface(): Surface {
        return mSurface
    }

    override fun run() {
        mMediaCodec.start()
        super.run()
        while (true) {
            if (isStop) {
                mMediaCodec.stop()
                mMediaCodec.release()

                mMediaMuxer.stop()
                mMediaMuxer.release()
                break
            }

            var outputBufferId: Int = mMediaCodec.dequeueOutputBuffer(bufferInfo, 0)
            if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                mTrackIndex = mMediaMuxer.addTrack(mMediaCodec.outputFormat)
                mMediaMuxer.start()
                mStartMuxer = true
            } else {
                while (outputBufferId >= 0) {
                    if (!mStartMuxer) {
                        Log.i(TAG, "MediaMuxer not start")
                        continue
                    }
                    val outputBuffer = mMediaCodec.getOutputBuffer(outputBufferId) ?: continue
                    outputBuffer.position(bufferInfo.offset)
                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                    if (pts == 0L) {
                        pts = bufferInfo.presentationTimeUs
                    }
                    bufferInfo.presentationTimeUs = bufferInfo.presentationTimeUs - pts
                    mMediaMuxer.writeSampleData(mTrackIndex, outputBuffer, bufferInfo)
                    Log.d(
                        TAG,
                        "pts = ${bufferInfo.presentationTimeUs / 1000000.0f} s ,${pts / 1000} ms"
                    )
                    mMediaCodec.releaseOutputBuffer(outputBufferId, false)
                    outputBufferId = mMediaCodec.dequeueOutputBuffer(bufferInfo, 0)
                }
            }
        }
    }
}