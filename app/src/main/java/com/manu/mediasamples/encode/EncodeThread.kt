package com.manu.mediasamples.encode

import android.media.MediaCodec
import android.media.MediaMuxer
import android.util.Log
import android.view.Surface
import kotlin.properties.Delegates

/**
 * @Desc: 编码线程
 * @Author: jzman
 */
class EncodeThread(var mMediaCodec: MediaCodec, var mMediaMuxer: MediaMuxer) : Thread() {
    /** 结束编码标志 */
    var isStop = false
    /** 复用器开启的标志 */
    private var mStartMuxer = false
    /** 缓冲区元数据：大小、偏移量、pts*/
    private var bufferInfo = MediaCodec.BufferInfo()
    /** 显示时间戳 */
    private var pts: Long = 0
    /** 轨道索引 */
    private var mTrackIndex by Delegates.notNull<Int>()

    companion object {
        private const val TAG = "EncodeManager"
    }

    override fun run() {
        super.run()
        mMediaCodec.start()
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