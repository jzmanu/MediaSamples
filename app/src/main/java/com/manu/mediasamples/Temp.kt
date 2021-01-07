//package com.manu.mediasamples
//
//import android.media.MediaCodec
//import android.media.MediaCodecInfo
//import android.media.MediaFormat
//import android.util.Log
//import java.nio.ByteBuffer
//
///**
// * @Desc:
// * @Author: jzman
// */
//class Temp {
//    private fun encodeToMp4(frameData: ByteArray) {
//        Log.i(CameraActivity.TAG, "encodeToMp4")
//        // 视频格式
//        val mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, 1080, 1920)
//        mediaFormat.setInteger(
//            MediaFormat.KEY_COLOR_FORMAT,
//            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
//        )
//        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 3000000)
//        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30)
//        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
//
//        // 创建编码器
//        val mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
//        // 配置编码器
//        mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
//        // 启动编码器
//        mediaCodec.start()
//
//        val presentTime = System.nanoTime()
//        while (hasMuxer) {
//            // 返回有效数据填充的输入缓冲区的索引
//            val inputBufferId: Int = mediaCodec.dequeueInputBuffer(30)
//            if (inputBufferId >= 0) {
//                //返回一个已清除的，可写的ByteBuffer对象，以使出队输入缓冲区索引包含输入数据
//                // 调用此方法后，必须不再使用先前为同一输入索引返回的任何ByteBuffer或Image对象。
//                val inputBuffer: ByteBuffer? = mediaCodec.getInputBuffer(inputBufferId)
//                // 向inputBuffer填充帧数据
//                inputBuffer?.put(frameData)
//                // 将填满数据的inputBuffer放到编码队列
//                mediaCodec.queueInputBuffer(
//                    inputBufferId,
//                    0,
//                    frameData.size,
//                    (System.nanoTime() - presentTime) / 1000,
//                    MediaCodec.BUFFER_FLAG_KEY_FRAME
//                )
//            }
//            val bufferInfo = MediaCodec.BufferInfo()
//
//            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
//                bufferInfo.size = 0
//                break
//            }
//
//            // 返回已成功解码的输出缓冲区的索引或INFO_*常量，最多阻塞timeoutUs微秒
//            // INFO_TRY_AGAIN_LATER 表明调用超时
//            // INFO_OUTPUT_FORMAT_CHANGED 输出格式已更改，后续数据将采用新格式，也可用getOutputFormat返回新格式
//            val outputBufferId: Int = mediaCodec.dequeueOutputBuffer(bufferInfo, 30)
//            Log.i(CameraActivity.TAG, "outputBufferId:$outputBufferId")
//            if (outputBufferId >= 0) {
//                // 返回只读的输出缓冲区的有效数据
//                val outputBuffer: ByteBuffer? = mediaCodec.getOutputBuffer(outputBufferId)
//                // 获取输出格式
////                val bufferFormat: MediaFormat = mediaCodec.getOutputFormat(outputBufferId) // option A
////                Log.i(TAG, "bufferFormat:$bufferFormat")
//                val byteArray = ByteArray(bufferInfo.size);
//                outputBuffer?.get(byteArray)
//                outputBuffer?.position(bufferInfo.offset)
//                outputBuffer?.limit(bufferInfo.size + bufferInfo.offset)
//
//                if (outputBuffer != null) {
//                    mMediaMuxer?.writeSampleData(mTrackIndex, outputBuffer, bufferInfo)
//                }
//                mediaCodec.releaseOutputBuffer(outputBufferId, false)
//            } else if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
//                // Subsequent data will conform to new format.
//                // Can ignore if using getOutputFormat(outputBufferId)
//                val outputFormat = mediaCodec.outputFormat // option B
//                mTrackIndex = mMediaMuxer?.addTrack(outputFormat)!!
//                mMediaMuxer?.start()
//            }
//        }
//    }
//}