//
// Created by jzman on 2023/10/17.
//

#ifndef FFMPEG_RESAMPLE_AUDIO_AUDIO_RESAMPLE_H
#define FFMPEG_RESAMPLE_AUDIO_AUDIO_RESAMPLE_H

#include "libavformat/avformat.h"
#include "libswresample/swresample.h"
#include "libavutil/audio_fifo.h"
#include "libavutil/opt.h"

/**
 * 音频重采样参数
 */
typedef struct {
    // 输入参数
    enum AVSampleFormat src_sample_fmt;
    int src_sample_rate;
    int64_t src_channel_layout;
    // 输出参数
    enum AVSampleFormat dst_sample_fmt;
    int dst_sample_rate;
    int64_t dst_channel_layout;
} AudioResampleParam;

/**
 * 重采样器
 */
typedef struct {
    SwrContext *swr_ctx;                      // 重采样上下文
    AudioResampleParam audio_resample_param;  // 重采样参数
    int is_fifo_only;         // 无需重采样只需缓存到FIFO中
    int is_flushed;           //
    AVAudioFifo *audio_fifo;  // 采样点的缓存
    int64_t start_pts;        // 起始PTS
    int64_t cur_pts;          // 当前PTS

    uint8_t **resampled_data;       // 用来缓存重采样后的数据
    int resampled_data_size;        // 重采样后的采样数
    int src_channels;
    int dst_channels;
    int64_t total_resampled_num;    // 统计总共的重采样点数，目前只是统计
} AudioResampler;

/**
 * 分配重采样器
 * @param param
 * @return
 */
AudioResampler *audio_resampler_alloc(AudioResampleParam param);

/**
 * 释放重采样器
 * @param resampler
 */
void audio_resampler_free(AudioResampler *resampler);

/**
 * 发送要进行重采样的帧
 * @param resampler 重采样器
 * @param frame Frame
 * @return 返回重采样后得到的采样点数
 */
int audio_resampler_send_frame(AudioResampler *resampler, AVFrame *frame);


/**
 * @brief 发送要进行重采样的帧
 * @param resampler
 * @param in_data 二级指针
 * @param in_nb_samples 输入的采样点数量(单个通道)
 * @param pts       pts
 * @return
 */
int audio_resampler_send_frame2(AudioResampler *resampler, uint8_t **in_data,int in_nb_samples, int64_t pts);

/**
 * @brief 发送要进行重采样的帧
 * @param resampler
 * @param in_data 一级指针
 * @param in_bytes 传入数据的字节大小
 * @param pts
 * @return
 */
int audio_resampler_send_frame3(AudioResampler *resampler, uint8_t *in_data,int in_bytes, int64_t pts);

/**
 * @brief 获取重采样后的数据
 * @param resampler
 * @param nb_samples  需要读取多少采样数量: 如果nb_samples>0，只有audio fifo>=nb_samples才能获取到数据;nb_samples=0,有多少就给多少
 *
 * @return 如果获取到采样点数则非NULL
 */
AVFrame *audio_resample_receive_frame(AudioResampler *resampler, int nb_samples);

/**
 * @brief 获取重采样后的数据
 * @param resampler
 * @param out_data 缓存获取到的重采样数据，buf一定要充足
 * @param nb_samples 需要读取多少采样数量: 如果nb_samples>0，只有audio fifo>=nb_samples才能获取到数据;nb_samples=0,有多少就给多少
 * @param pts  获取帧的pts值
 * @return 获取到的采样点数量
 */
int  audio_resampler_receive_frame2(AudioResampler *resampler, uint8_t **out_data, int nb_samples, int64_t *pts);

/**
 * @brief audio_resampler_get_fifo_size
 * @param resampler
 * @return audio fifo的缓存的采样点数量
 */
int audio_resampler_get_fifo_size(AudioResampler *resampler);
/**
 * @brief audio_resampler_get_start_pts
 * @param resampler
 * @return 起始的pts
 */
int64_t audio_resampler_get_start_pts(AudioResampler *resampler);
/**
 * @brief audio_resampler_get_cur_pts
 * @param resampler
 * @return 当前的pts
 */
int64_t audio_resampler_get_cur_pts(AudioResampler *resampler);
#endif //FFMPEG_RESAMPLE_AUDIO_AUDIO_RESAMPLE_H
