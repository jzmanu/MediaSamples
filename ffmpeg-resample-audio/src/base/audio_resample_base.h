//
// Created by jzman on 2023/10/19.
//

#ifndef FFMPEG_RESAMPLE_AUDIO_AUDIO_RESAMPLE_BASE_H
#define FFMPEG_RESAMPLE_AUDIO_AUDIO_RESAMPLE_BASE_H

#include "libavutil/samplefmt.h"
#include "libavutil/channel_layout.h"


/**
* 进行重采样;
* 将重采样后的samples放入audio fifo;
* 再从fifo读取指定的samples.
*/
int get_format_from_sample_fmt(const char **fmt,
                               enum AVSampleFormat sample_fmt);
/**
 * Fill dst buffer with nb_samples, generated starting from t.
 */
void fill_samples(double *dst, int nb_samples, int nb_channels, int sample_rate, double *t);

#endif //FFMPEG_RESAMPLE_AUDIO_AUDIO_RESAMPLE_BASE_H
