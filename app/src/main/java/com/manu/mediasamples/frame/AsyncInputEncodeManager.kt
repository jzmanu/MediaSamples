package com.manu.mediasamples.frame

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import android.util.Log
import com.manu.mediasamples.app.MediaApplication
import java.io.IOException
import java.util.concurrent.LinkedBlockingQueue
import kotlin.properties.Delegates


/**
 * @Desc:AsyncInputEncodeManager
 * @Author: jzman
 */
object AsyncInputEncodeManager : MediaCodec.Callback(){
    private const val TAG = "AsyncInputEncodeManager"
    private const val MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC
    private lateinit var mMediaCodec: MediaCodec
    private lateinit var mMediaMuxer: MediaMuxer

    private var pts:Long = 0
    private var isMuxer = false
    private var isStop = false

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
        isStop = false
    }

    /**
     * 结束编码
     */
    fun stopEncode() {
        Log.d(TAG, "stopEncode")
        mMediaCodec.stop()
        mMediaCodec.release()
        if (isMuxer){
            mMediaMuxer.stop()
            mMediaMuxer.release()
        }
        isStop = true
    }

    /**
     * 添加新帧
     */
    fun offer(byteArray: ByteArray, timeStamp: Long) {
        val temp = mQuene.offer(FrameData(byteArray, timeStamp))
        Log.i(TAG, "offer return:$temp")
    }

    /**
     * 取出新帧
     */
    private fun poll(): FrameData? {
        return mQuene.poll()
    }

    fun isStop():Boolean{
        return isStop
    }

    /**
     * 初始化MediaCodec
     */
    private fun initCodec(width: Int, height: Int) {
        Log.d(TAG, "initCodec start")
        try {
            // 创建MediaCodec
            mMediaCodec = MediaCodec.createEncoderByType(MIME_TYPE)
            // 参数设置
            val mediaFormat = MediaFormat.createVideoFormat(MIME_TYPE, 1920, 1080)
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
            )
            // 颜色采样格式
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 2000000) // 比特率
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 15) // 帧率
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 10) // I帧间隔

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

        // 获取空的缓冲区
        val inputBuffer = codec.getInputBuffer(index)

        val data = poll()
        try {
            if (data != null) {
                if (inputBuffer != null) {
                    inputBuffer.clear()
                    inputBuffer.put(data.buffer)
                    codec.queueInputBuffer(
                        index,
                        0,
                        data.buffer.size,
                        System.nanoTime() / 1000,
                        0
                    )
                }
            } else {
                //EOS
                codec.queueInputBuffer(
                    index,
                    0, 0, 0, 0
                )
            }
        } catch (e: Exception) {
            println("error:${e.message}")
            println("error:${e.localizedMessage}")
            inputBuffer!!.clear()
            e.printStackTrace()
        }
    }

    override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
        Log.d(TAG,"onOutputFormatChanged format:${format}")
        mTrackIndex = mMediaMuxer.addTrack(format)
        mMediaMuxer.start()
        isMuxer = true
    }

    override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
        Log.d(TAG,"onError e:${e.message}")
    }

    /**
     * ImageReader获取的帧数据的封装
     */
    class FrameData(var buffer: ByteArray, var timeStamp: Long)
}