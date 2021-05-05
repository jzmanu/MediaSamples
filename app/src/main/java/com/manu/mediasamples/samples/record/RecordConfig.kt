package com.manu.mediasamples.samples.record

import android.media.AudioFormat
import android.media.MediaRecorder

/**
 * @Desc:录制配置
 * @Author: jzman
 * @Date: 2021/4/24 15:47.
 */
object RecordConfig {

    /** 录音源为主麦克风 */
    const val AUDIO_SOURCE = MediaRecorder.AudioSource.MIC

    /** 音频格式 */
    const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

    /** 采样率 */
    const val SAMPLE_RATE = 44100

    /** 声道配置 */
    const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_STEREO

    /** 音频轨道索引 */
    var audioTrackIndex = -1

    /** 视频轨道索引 */
    var videoTrackIndex = -1

    /** 标示MediaMuxer是否已经start */
    var isMuxerStart: Boolean = false

    /** 标识视频是否停止 */
    var isAudioStop: Boolean = false

    /** 标识音频是否停止 */
    var isVideoStop: Boolean = false

    /** 标识添加音轨是否成功*/
    var isAddAudioTrack: Boolean = false

    /** 标识添加视轨是否成功 */
    var isAddVideoTrack: Boolean = false
}