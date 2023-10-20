//
// Created by jzman on 2023/10/17.
//

#include "audio_resample.h"

static char err_buf[128] = {0};

static char *av_get_err(int ret) {
    av_strerror(ret, err_buf, 128);
    return err_buf;
}

static int init_resample_data(AudioResampler *resampler) {
    if (resampler->resampled_data) {
        av_freep(&resampler->resampled_data[0]);
    }
    av_freep(&resampler->resampled_data);
    int line_size = 0;
    int ret = av_samples_alloc_array_and_samples(
            &resampler->resampled_data, &line_size,
            resampler->dst_channels,
            resampler->resampled_data_size,
            resampler->audio_resample_param.dst_sample_fmt, 0);
    if (ret < 0) {
        fprintf(stderr, "init_resample_data > alloc audio resampler data buffer failed, err:%s\n",
                av_get_err(ret));
    }
    return ret;
}

/**
 *
 * @param nb_samples
 * @param resampler_params
 * @return
 */
static AVFrame *alloc_out_frame(const int nb_samples, const AudioResampleParam *resampler_params) {
    int ret;
    AVFrame *frame = av_frame_alloc();
    if (!frame) {
        return NULL;
    }
    frame->nb_samples = nb_samples;
    frame->channel_layout = resampler_params->dst_channel_layout;
    frame->format = resampler_params->dst_sample_fmt;
    frame->sample_rate = resampler_params->dst_sample_rate;
    // 为AVFrame数据缓冲区分配内存
    ret = av_frame_get_buffer(frame, 0);
    if (ret < 0) {
        printf("cannot allocate audio data buffer\n");
        return NULL;
    }
    return frame;
}

static AVFrame *get_one_frame(AudioResampler *resampler, const int nb_samples) {
    AVFrame *frame = alloc_out_frame(nb_samples, &resampler->audio_resample_param);
    if (frame) {
        // 往frame->data写入数据
        av_audio_fifo_read(resampler->audio_fifo, (void **) frame->data, nb_samples);
        frame->pts = resampler->cur_pts;
        resampler->cur_pts += nb_samples;
        resampler->total_resampled_num += nb_samples;
    }
    return frame;
}

AudioResampler *audio_resampler_alloc(AudioResampleParam param) {
    int ret = 0;
    // 为采样器AudioResampler分配内存
    AudioResampler *audio_resampler = (AudioResampler *) av_malloc(sizeof(AudioResampler));
    if (!audio_resampler) {
        fprintf(stderr, "AudioResampler malloc failed.");
        return NULL;
    }
    memset(audio_resampler, 0, sizeof(AudioResampler));

    // 设置参数
    audio_resampler->audio_resample_param = param;
    audio_resampler->src_channels = av_get_channel_layout_nb_channels(param.src_channel_layout);
    audio_resampler->dst_channels = av_get_channel_layout_nb_channels(param.dst_channel_layout);
    audio_resampler->start_pts = AV_NOPTS_VALUE;
    audio_resampler->cur_pts = AV_NOPTS_VALUE;
    AVAudioFifo *audio_fifo =
            av_audio_fifo_alloc(param.dst_sample_fmt, audio_resampler->dst_channels, 1);
    if (!audio_fifo) {
        fprintf(stderr, "AVAudioFifo alloc failed.");
        av_freep(audio_resampler);
        return NULL;
    }
    audio_resampler->audio_fifo = audio_fifo;

    if (param.src_sample_fmt == param.dst_sample_fmt &&
        param.src_channel_layout == param.dst_channel_layout &&
        param.src_sample_rate == param.dst_sample_rate) {
        audio_resampler->is_fifo_only = 1;
        fprintf(stderr, " current audio param don't need resampler.\n");
        return audio_resampler;
    }
    audio_resampler->swr_ctx = swr_alloc();
    if (!audio_resampler->swr_ctx){
        fprintf(stderr,"swr_alloc failed.\n");
        return NULL;
    }

    av_opt_set_sample_fmt(audio_resampler->swr_ctx, "in_sample_fmt",      param.src_sample_fmt, 0);
    av_opt_set_int(audio_resampler->swr_ctx,        "in_channel_layout",  param.src_channel_layout, 0);
    av_opt_set_int(audio_resampler->swr_ctx,        "in_sample_rate",     param.src_sample_rate, 0);
    av_opt_set_sample_fmt(audio_resampler->swr_ctx, "out_sample_fmt",     param.dst_sample_fmt, 0);
    av_opt_set_int(audio_resampler->swr_ctx,        "out_channel_layout", param.dst_channel_layout, 0);
    av_opt_set_int(audio_resampler->swr_ctx,        "out_sample_rate",    param.dst_sample_rate, 0);

    ret = swr_init(audio_resampler->swr_ctx);
    if (ret < 0) {
        fprintf(stderr, "swr_init failed.\n");
    }
    audio_resampler->resampled_data_size = 2048;

    if (init_resample_data(audio_resampler) < 0) {
        swr_free(&audio_resampler->swr_ctx);
        av_fifo_freep((AVFifoBuffer **) &audio_resampler->audio_fifo);
        av_freep(audio_resampler);
        return NULL;
    }
    fprintf(stdout, "audio_resampler_alloc finish.\n");
    return audio_resampler;
}

void audio_resampler_free(AudioResampler *resampler) {
    if (!resampler) {
        return;
    }
    if (resampler->swr_ctx) {
        swr_free(&resampler->swr_ctx);
    }

    if (resampler->audio_fifo) {
        av_fifo_freep((AVFifoBuffer **) &resampler->audio_fifo);
    }

    if (resampler->resampled_data) {
        av_freep(&resampler->resampled_data[0]);
    }
    av_freep(&resampler->resampled_data);
    av_freep(resampler);
}

int audio_resampler_send_frame(AudioResampler *resampler, AVFrame *frame) {
    int ret = 0;
    if (!resampler) {
        return 0;
    }
    int src_nb_samples = 0;
    uint8_t **src_data = NULL;
    if (frame) {
        src_nb_samples = frame->nb_samples;
        src_data = frame->extended_data;

        if (resampler->start_pts == AV_NOPTS_VALUE && frame->pts != AV_NOPTS_VALUE) {
            resampler->start_pts = frame->pts;
            resampler->cur_pts = frame->pts;
        }
    } else {
        resampler->is_flushed = 1;
    }

    if (resampler->is_fifo_only) {
        // 如果不需要重采样，则将原数据写入fifo
        if (src_data) {
            ret = av_audio_fifo_write(resampler->audio_fifo, (void **) src_data, src_nb_samples);
        }
        return ret;
    }

    // 计算下一个输入sample和输出sample之前的延迟
    int64_t delay = swr_get_delay(resampler->swr_ctx,
                                  resampler->audio_resample_param.src_sample_rate);
    int dst_nb_samples = av_rescale_rnd(delay + src_nb_samples,
                                        resampler->audio_resample_param.src_sample_rate,
                                        resampler->audio_resample_param.dst_sample_rate,
                                        AV_ROUND_UP);

    if (dst_nb_samples > resampler->resampled_data_size) {
        resampler->resampled_data_size = dst_nb_samples;
        if (init_resample_data(resampler) < 0) {
            return AVERROR(ENOMEM);
        }
    }

    // 格式转换
    int nb_samples = swr_convert(resampler->swr_ctx, resampler->resampled_data, dst_nb_samples,
                                 (const uint8_t **) src_data, src_nb_samples);

    ret = av_audio_fifo_write(resampler->audio_fifo, (void **) resampler->resampled_data,
                              nb_samples);
    fprintf(stdout, "audio_resampler_send_frame, size:%d\n", ret);
    if (ret < nb_samples) {
        fprintf(stdout, "Warn：av_audio_fifo_write failed, expected_write:%d, actual_write:%d\n",
                nb_samples, ret);
    }
    return ret;
}

int audio_resampler_send_frame2(AudioResampler *resampler, uint8_t **in_data, int in_nb_samples,
                                int64_t pts) {
    int ret = 0;
    if (!resampler) {
        return 0;
    }
    int src_nb_samples = 0;
    uint8_t **src_data = NULL;
    if (in_data) {
        src_nb_samples = in_nb_samples;
        src_data = in_data;

        if (resampler->start_pts == AV_NOPTS_VALUE && pts != AV_NOPTS_VALUE) {
            resampler->start_pts = pts;
            resampler->cur_pts = pts;
        }
    } else {
        resampler->is_flushed = 1;
    }

    if (resampler->is_fifo_only) {
        // 如果不需要重采样，则将原数据写入fifo
        if (src_data) {
            ret = av_audio_fifo_write(resampler->audio_fifo, (void **) src_data, src_nb_samples);
        }
        return ret;
    }

    // 计算下一个输入sample和输出sample之前的延迟
    int64_t delay = swr_get_delay(resampler->swr_ctx,
                                  resampler->audio_resample_param.src_sample_rate);
    int dst_nb_samples = av_rescale_rnd(delay + src_nb_samples,
                                        resampler->audio_resample_param.src_sample_rate,
                                        resampler->audio_resample_param.dst_sample_rate,
                                        AV_ROUND_UP);

    if (dst_nb_samples > resampler->resampled_data_size) {
        resampler->resampled_data_size = dst_nb_samples;
        if (init_resample_data(resampler) < 0) {
            return AVERROR(ENOMEM);
        }
    }

    // 格式转换
    int nb_samples = swr_convert(resampler->swr_ctx, resampler->resampled_data, dst_nb_samples,
                                 (const uint8_t **) src_data, src_nb_samples);

    ret = av_audio_fifo_write(resampler->audio_fifo, (void **) resampler->resampled_data,
                              nb_samples);
    fprintf(stdout, "audio_resampler_send_frame, size:%d\n", ret);
    if (ret < nb_samples) {
        fprintf(stdout, "Warn：av_audio_fifo_write failed, expected_write:%d, actual_write:%d\n",
                nb_samples, ret);
    }
    return ret;
}

int audio_resampler_send_frame3(AudioResampler *resampler, uint8_t *in_data, int in_bytes,
                                int64_t pts) {
    if (!resampler) {
        return 0;
    }

    AVFrame *frame = NULL;
    if (in_data) {
        frame = av_frame_alloc();
        frame->format = resampler->audio_resample_param.src_sample_fmt;
        frame->channel_layout = resampler->audio_resample_param.src_channel_layout;
        int ch = av_get_channel_layout_nb_channels(
                resampler->audio_resample_param.src_channel_layout);
        frame->nb_samples = in_bytes / av_get_bytes_per_sample(
                resampler->audio_resample_param.src_sample_fmt) / ch;
        avcodec_fill_audio_frame(frame, ch, resampler->audio_resample_param.src_sample_fmt,
                                 in_data, in_bytes, 0);
        frame->pts = pts;
    }

    int ret = audio_resampler_send_frame(resampler, frame);
    if (frame) {
        av_frame_free(&frame);      // 释放内存
    }
    return ret;
}

AVFrame *audio_resample_receive_frame(AudioResampler *resampler, int nb_samples) {
    nb_samples = nb_samples == 0 ? av_audio_fifo_size(resampler->audio_fifo) : nb_samples;

    if (av_audio_fifo_size(resampler->audio_fifo) < nb_samples || nb_samples == 0)
        return NULL;
    // 采样点数满足条件
    return get_one_frame(resampler, nb_samples);
}

int audio_resampler_receive_frame2(AudioResampler *resampler, uint8_t **out_data, int nb_samples,
                                   int64_t *pts) {
    nb_samples = nb_samples == 0 ? av_audio_fifo_size(resampler->audio_fifo) : nb_samples;
    if (av_audio_fifo_size(resampler->audio_fifo) < nb_samples || nb_samples == 0)
        return 0;
    int ret = av_audio_fifo_read(resampler->audio_fifo, (void **) out_data, nb_samples);
    *pts = resampler->cur_pts;
    resampler->cur_pts += nb_samples;
    resampler->total_resampled_num += nb_samples;
    return ret;
}

int audio_resampler_get_fifo_size(AudioResampler *resampler) {
    if (!resampler) {
        return 0;
    }
    return av_audio_fifo_size(resampler->audio_fifo);   // 获取fifo的采样点数量
}

int64_t audio_resampler_get_start_pts(AudioResampler *resampler) {
    if (!resampler) {
        return 0;
    }
    return resampler->start_pts;
}

int64_t audio_resampler_get_cur_pts(AudioResampler *resampler) {
    if (!resampler) {
        return 0;
    }
    return resampler->cur_pts;
}

