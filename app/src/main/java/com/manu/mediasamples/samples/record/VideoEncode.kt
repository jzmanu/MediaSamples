package com.manu.mediasamples.samples.record

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import android.view.Surface
import android.widget.Toast
import com.manu.mediasamples.util.L

/**
 * @Desc:VideoEncode
 * @Author: jzman
 */
object VideoEncode : MediaCodec.Callback() {
    private const val TAG = "VideoEncode"
    private const val MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC
    private const val COLOR_FORMAT_SURFACE =
        MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
    private lateinit var mVideoCodec: MediaCodec
    private lateinit var mVideoMuxer: MediaMuxer

    /** 用作数据流输入的Surface */
    private lateinit var mSurface: Surface

    private var pts: Long = 0

    /**
     * 初始化
     */
    fun initVideo(width: Int, height: Int, muxer: MediaMuxer) {
        L.i(TAG, "initVideo")
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
        L.i(TAG, "startEncode > mVideoMuxer:${mVideoMuxer}")
        mVideoCodec.start()
    }

    /**
     * 结束编码
     */
    fun stopVideoEncode() {
        L.i(TAG, "stopEncode")
        mVideoCodec.stop()
        mVideoCodec.release()
        RecordConfig.isVideoStop = true
        if (RecordConfig.isAudioStop) {
            mVideoMuxer.stop()
            mVideoMuxer.release()
            RecordConfig.isVideoStop = false
        }
    }

    override fun onOutputBufferAvailable(
        codec: MediaCodec,
        index: Int,
        info: MediaCodec.BufferInfo
    ) {
        L.i(
            TAG,
            "onOutputBufferAvailable index:$index, info->offset:${info.offset},size:${info.size}" +
                    ",pts:${info.presentationTimeUs / 1000000} , isMuxerStart:${RecordConfig.isMuxerStart}"
        )
        // 如果发现MediaMuxer还未启动，则释放这个OutputBuffer
        if (!RecordConfig.isMuxerStart) {
            mVideoCodec.releaseOutputBuffer(index, false)
            return
        }
        val outputBuffer = codec.getOutputBuffer(index) ?: return
        if (info.size > 0) {
            outputBuffer.position(info.offset)
            outputBuffer.limit(info.size)
            if (pts == 0L) {
                info.presentationTimeUs = info.presentationTimeUs - pts
            }
            mVideoMuxer.writeSampleData(RecordConfig.videoTrackIndex, outputBuffer, info)
            mVideoCodec.releaseOutputBuffer(index, false)
        }
    }

    override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
        L.i(TAG, "onInputBufferAvailable index:$index")
    }

    override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
        L.i(TAG, "onOutputFormatChanged format:${format}")
        addVideoTrack(format)
        if (RecordConfig.audioTrackIndex != -1) {
            mVideoMuxer.start()
            RecordConfig.isMuxerStart = true
        }
    }

    override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
        L.i(TAG, "onError e:${e.message}")
    }

    /**
     * 初始化MediaCodec
     */
    private fun initCodec(width: Int, height: Int) {
        L.i(TAG, "initCodec start")
        try {
            // 创建MediaCodec
            mVideoCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            // 参数设置
            val mediaFormat = MediaFormat.createVideoFormat(MIME_TYPE, width, height)
            mediaFormat.setInteger(
                MediaFormat.KEY_COLOR_FORMAT,
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
            L.i(TAG, "initCodec fail:${e.message} ")
            e.printStackTrace()
        }
        L.i(TAG, "initCodec end")
    }

    private fun addVideoTrack(format: MediaFormat) {
        L.i(TAG, "addVideoTrack format:${format}")
        RecordConfig.videoTrackIndex = mVideoMuxer.addTrack(format)
        RecordConfig.isAddVideoTrack = true
    }
}