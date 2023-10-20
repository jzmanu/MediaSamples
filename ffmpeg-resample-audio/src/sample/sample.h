//
// Created by jzman on 2023/10/20.
//

#ifndef FFMPEG_RESAMPLE_AUDIO_SAMPLE_H
#define FFMPEG_RESAMPLE_AUDIO_SAMPLE_H

/**
 * 音频重采样官方案例
 * @param outFileName
 */
int audioSampleBase(char *outFileName);

/**
 * 封装重采样器
 * @param outFileName
 */
int audioSampleResampler(const char *outFileName);

#endif //FFMPEG_RESAMPLE_AUDIO_SAMPLE_H
