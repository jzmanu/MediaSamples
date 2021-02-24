package com.manu.mediasamples.frame

import android.graphics.ImageFormat
import android.graphics.Rect
import android.media.*
import android.os.Build
import android.util.Log
import com.manu.mediasamples.app.MediaApplication
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.LinkedBlockingQueue
import kotlin.properties.Delegates


/**
 * @Desc:AsyncInputEncodeManager
 * @Author: jzman
 */
object AsyncInputEncodeManager : MediaCodec.Callback(){
    private const val TAG = "AsyncInputEncodeManager"
    private const val MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC
    private const val COLOR_FORMAT_SURFACE =
        MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
    private lateinit var mMediaCodec: MediaCodec
    private lateinit var mMediaMuxer: MediaMuxer

    private var pts:Long = 0

    /** 轨道索引 */
    private var mTrackIndex by Delegates.notNull<Int>()

    private var mQuene: LinkedBlockingQueue<FrameData> = LinkedBlockingQueue()

    /**
     * 初始化
     */
    fun init(width: Int, height: Int) {
        Log.d(TAG, "init")
        initCodec(width, height)
        initMuxer()
    }

    /**
     * 开始编码
     */
    fun startEncode() {
        Log.d(TAG, "startEncode")
        mMediaCodec.start()
    }

    /**
     * 结束编码
     */
    fun stopEncode() {
        Log.d(TAG, "stopEncode")
        mMediaCodec.stop()
        mMediaMuxer.stop()
    }

    /**
     * 添加新帧
     */
    fun offer(byteArray: ByteArray) {
        val temp = mQuene.offer(FrameData(byteArray, System.nanoTime()))
        Log.i(TAG, "offer return:$temp")
    }

    /**
     * 取出新帧
     */
    fun poll(): FrameData? {
        return mQuene.poll()
    }

    /**
     * 初始化MediaCodec
     */
    private fun initCodec(width: Int, height: Int) {
        Log.d(TAG, "initCodec start")
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
            // 设置Callback
            mMediaCodec.setCallback(this)
            // 配置状态
            mMediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        } catch (e: Exception) {
            Log.i(TAG, "initCodec fail:${e.message} ")
            e.printStackTrace()
        }
    }

    /**
     * 初始化MediaMuxer
     */
    private fun initMuxer() {
        Log.d(TAG, "initMuxer start")
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

    override fun onOutputBufferAvailable(
        codec: MediaCodec,
        index: Int,
        info: MediaCodec.BufferInfo
    ) {
        Log.d(TAG,"onOutputBufferAvailable index:$index, info->offset:${info.offset},size:${info.size},pts:${info.presentationTimeUs/1000000}")
        val outputBuffer = codec.getOutputBuffer(index) ?: return
        outputBuffer.position(info.offset)
        outputBuffer.limit(info.size)
        if (pts == 0L){
            info.presentationTimeUs = info.presentationTimeUs - pts
        }
        mMediaMuxer.writeSampleData(mTrackIndex,outputBuffer,info)
        mMediaCodec.releaseOutputBuffer(index,false)
    }

    override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
        Log.d(TAG,"onInputBufferAvailable index:$index")

        // 测试发现仅回调几次，导致获取的帧数据不断添加导致OOM，后续完善
        val data = poll()
        // 获取空的缓冲区
        val inputBuffer = mMediaCodec.getInputBuffer(index)
        if (data == null) return
        val buffer = data.buffer
        if (inputBuffer != null) {
            inputBuffer.clear()
            inputBuffer.put(buffer)
        }

        mMediaCodec.queueInputBuffer(
            index,
            0,
            buffer.size,
            System.nanoTime() / 1000,
            0
        )
    }

    override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
        Log.d(TAG,"onOutputFormatChanged format:${format}")
        mTrackIndex = mMediaMuxer.addTrack(format)
        mMediaMuxer.start()
    }

    override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
        Log.d(TAG,"onError e:${e.message}")
    }

    /**
     * ImageReader获取的帧数据的封装
     */
    class FrameData(var buffer: ByteArray, var timeStamp: Long)
}