package com.manu.mediasamples.samples.record

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import android.util.Log
import android.view.Surface
import kotlin.properties.Delegates

/**
 * @Desc:VideoEncode
 * @Author: jzman
 */
object VideoEncode : MediaCodec.Callback(){
    private const val TAG = "VideoEncode"
    private const val MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC
    private const val COLOR_FORMAT_SURFACE =
        MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
    private lateinit var mVideoCodec: MediaCodec
    private lateinit var mVideoMuxer: MediaMuxer

    /** 用作数据流输入的Surface */
    private lateinit var mSurface: Surface

    private var pts:Long = 0

    /** 轨道索引 */
    var mVideoTrackIndex= -1

    /**
     * 初始化
     */
    fun initVideo(width: Int, height: Int,muxer: MediaMuxer) {
        Log.d(TAG, "initVideo")
        this.mVideoMuxer = muxer
        initCodec(width, height)
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
    fun startVideoEncode() {
        Log.d(TAG, "startEncode")
        mVideoCodec.start()
    }

    /**
     * 结束编码
     */
    fun stopVideoEncode() {
        Log.d(TAG, "stopEncode")
        mVideoCodec.stop()
    }

    /**
     * 初始化MediaCodec
     */
    private fun initCodec(width: Int, height: Int) {
        Log.d(TAG, "initCodec start")
        try {
            // 创建MediaCodec
            mVideoCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
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
            mVideoCodec.setCallback(this)
            // 配置状态
            mVideoCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            // 创建Surface作为MediaCodec的输入，createInputSurface只能在configure与start之间调用创建Surface
            mSurface = mVideoCodec.createInputSurface()
        } catch (e: Exception) {
            Log.i(TAG, "initCodec fail:${e.message} ")
            e.printStackTrace()
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
        mVideoMuxer.writeSampleData(mVideoTrackIndex,outputBuffer,info)
        mVideoCodec.releaseOutputBuffer(index,false)
    }

    override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
        Log.d(TAG,"onInputBufferAvailable index:$index")
    }

    override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
        Log.d(TAG,"onOutputFormatChanged format:${format}")
        mVideoTrackIndex = mVideoMuxer.addTrack(format)
//        if (AudioEncode.mAudioTrackIndex)
        mVideoMuxer.start()
    }

    override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
        Log.d(TAG,"onError e:${e.message}")
    }
}