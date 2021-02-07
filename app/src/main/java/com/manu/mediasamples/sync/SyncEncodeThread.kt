package com.manu.mediasamples.sync

import android.media.MediaCodec
import android.media.MediaMuxer
import android.util.Log
import kotlin.properties.Delegates

/**
 * @Desc: 编码线程
 * @Author: jzman
 */
class SyncEncodeThread(var mMediaCodec: MediaCodec, var mMediaMuxer: MediaMuxer) : Thread() {
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

            // 返回已成功编码的输出缓冲区的索引
            var outputBufferId: Int = mMediaCodec.dequeueOutputBuffer(bufferInfo, 0)
            if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // 添加视频轨道
                mTrackIndex = mMediaMuxer.addTrack(mMediaCodec.outputFormat)
                mMediaMuxer.start()
                mStartMuxer = true
            } else {
                while (outputBufferId >= 0) {
                    if (!mStartMuxer) {
                        Log.i(TAG, "MediaMuxer not start")
                        continue
                    }
                    // 获取有效数据
                    val outputBuffer = mMediaCodec.getOutputBuffer(outputBufferId) ?: continue
                    outputBuffer.position(bufferInfo.offset)
                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                    if (pts == 0L) {
                        pts = bufferInfo.presentationTimeUs
                    }
                    bufferInfo.presentationTimeUs = bufferInfo.presentationTimeUs - pts
                    // 将数据写入复用器以生成文件
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