package com.manu.mediasamples.others

import android.content.Context
import android.media.*
import android.util.Log
import java.util.concurrent.LinkedBlockingQueue
import kotlin.properties.Delegates

/**
 * @Desc: EncodeManager
 * @Author: jzman
 */
object Encode {

    private const val TAG = "EncodeManager"
    private const val MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC
    private const val COLOR_FORMAT = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible

    /** 编码器的缓存超时时间 */
    private const val TIMES_OUT = 10000L

    private lateinit var mMediaCodec: MediaCodec
    private var mTrackIndex by Delegates.notNull<Int>()
    private var isEncodeStart = false
    private var isStop = false
    private lateinit var mMediaMuxer: MediaMuxer
    private lateinit var mVideoEncodeThread:Thread

    private var mQuene: LinkedBlockingQueue<FrameData> = LinkedBlockingQueue()
//    private var mQuene: LinkedList<FrameData> = LinkedList()

    fun init(context: Context) {
        mMediaMuxer = MediaMuxer(
            "${context.filesDir}/test.mp4",
            MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
        )
    }

    /**
     * 开启编码
     */
    fun startEncode() {
        Log.i(TAG, "startEncode start")
        try {
            val codecInfo = matchSupportCodec(MIME_TYPE)
                ?: error("${MediaFormat.MIMETYPE_VIDEO_AVC} not support")
            // 创建MediaCodec
//        mMediaCodec = MediaCodec.createByCodecName(codecInfo.name)
            mMediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            // 编码格式
            val mediaFormat = MediaFormat.createVideoFormat(MIME_TYPE, 1080, 1920)
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, COLOR_FORMAT) // 颜色采样格式
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 3000000) // 比特率
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30) // 帧率
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1) // I帧间隔
            mMediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            mMediaCodec.start()
            isEncodeStart = true
        } catch (e: Exception) {
            Log.i(TAG, "startEncode fail:${e.message} ")
            e.printStackTrace()
        }

    }

    fun start() {
        mVideoEncodeThread = Thread(VideoEncodeRunnable())
        mVideoEncodeThread.start()
    }

    /**
     * 停止编码
     */
    fun stop() {

        try {
            isEncodeStart = false
            isStop = true
            mMediaCodec.stop()
            mMediaCodec.release()
            mMediaMuxer.stop()
            mMediaMuxer.release()
        }catch (e:Exception){
            e.printStackTrace()
            Log.i(TAG, "codec stop fail:${e.message}")
        }

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
     * 匹配设备支持的编解码器
     */
    private fun matchSupportCodec(mimeType: String): MediaCodecInfo? {
        val mediaCodecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
        val codeInfos = mediaCodecList.codecInfos
        for (codeInfo in codeInfos) {
            if (codeInfo.isEncoder) continue
            val types = codeInfo.supportedTypes
            for (type in types) {
                if (type.equals(mimeType, true)) {
                    return codeInfo
                }
            }
        }
        return null
    }

    /**
     * 编码线程
     */
    private class VideoEncodeRunnable : Runnable {
        override fun run() {
            if (!isEncodeStart) {
                Thread.sleep(200)
                startEncode()
            }

            // 处理ImageReader生成的帧数据
            while (!isStop && isEncodeStart) {

                if (isStop) break
                val data = poll()
                if (data != null) {
                    // 获取空闲的用于填充有效数据的输入缓冲区的索引，无可用缓冲区则返回-1
                    val inputBufferId = mMediaCodec.dequeueInputBuffer(TIMES_OUT)
                    Log.i(TAG, "VideoEncodeRunnable > inputBufferId:$inputBufferId ")
                    if (inputBufferId >= 0) {
                        // 获取空的缓冲区
                        val inputBuffer = mMediaCodec.getInputBuffer(inputBufferId)
                        val buffer = data.buffer
                        if (inputBuffer != null) {
                            inputBuffer.clear()
                            inputBuffer.put(buffer)
                        }

                        mMediaCodec.queueInputBuffer(
                            inputBufferId,
                            0,
                            buffer.size,
                            System.nanoTime() / 1000,
                            MediaCodec.BUFFER_FLAG_KEY_FRAME
                        )
                    }
                }
                val bufferInfo = MediaCodec.BufferInfo()
                //得到成功编码后输出的out buffer Id
                val outputBufferId = mMediaCodec.dequeueOutputBuffer(bufferInfo, TIMES_OUT)
                Log.i(TAG, "VideoEncodeRunnable > outputBufferId:$outputBufferId ")
                if (outputBufferId >= 0) {
                    val outputBuffer = mMediaCodec.getOutputBuffer(outputBufferId)

                    val out = ByteArray(bufferInfo.size)
//                    outputBuffer?.get(out)
                    outputBuffer?.position(bufferInfo.offset)
                    outputBuffer?.limit(bufferInfo.offset + bufferInfo.size)
                    // 将编码后的数据写入到MP4复用器
                    if (outputBuffer != null && bufferInfo.size != 0) {
                        mMediaMuxer.writeSampleData(mTrackIndex, outputBuffer, bufferInfo)
                        Log.i(
                            TAG,
                            "VideoEncodeRunnable > writeSampleData: size:${bufferInfo.size} "
                        )
                    }
                    //释放output buffer
                    mMediaCodec.releaseOutputBuffer(outputBufferId, false)
                } else if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    Log.i(TAG, "VideoEncodeRunnable > format_change ")
                    val mediaFormat = mMediaCodec.outputFormat
                    mTrackIndex = mMediaMuxer.addTrack(mediaFormat)
                    mMediaMuxer.start()
                }
            }
        }
    }

    /**
     * ImageReader获取的帧数据的封装
     */
    class FrameData(var buffer: ByteArray, var timeStamp: Long)
}