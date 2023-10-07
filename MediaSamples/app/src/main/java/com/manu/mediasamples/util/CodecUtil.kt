package com.manu.mediasamples.util

import android.media.MediaCodecInfo
import android.media.MediaCodecList


/**
 * @Desc:
 * @Author: jzman
 */
object CodecUtil {

    /**
     * 查询指定MIME类型的编码器
     */
    fun selectCodec(mimeType: String): MediaCodecInfo? {
        val mediaCodecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
        val codeInfos = mediaCodecList.codecInfos
        for (codeInfo in codeInfos) {
            if (!codeInfo.isEncoder) continue
            val types = codeInfo.supportedTypes
            for (type in types) {
                if (type.equals(mimeType, true)) {
                    return codeInfo
                }
            }
        }
        return null
    }
}