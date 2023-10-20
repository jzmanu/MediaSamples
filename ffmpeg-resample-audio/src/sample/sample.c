//
// Created by jzman on 2023/10/20.
//
#include "audio_resample_base.h"
#include "audio_resample.h"
#include "sample.h"

int audioSampleBase(char *outFileName){
    int64_t src_ch_layout = AV_CH_LAYOUT_STEREO, dst_ch_layout = AV_CH_LAYOUT_SURROUND;
    int src_rate = 48000, dst_rate = 44100;
    uint8_t **src_data = NULL, **dst_data = NULL;
    int src_nb_channels = 0, dst_nb_channels = 0;
    int src_linesize, dst_linesize;
    int src_nb_samples = 1024, dst_nb_samples, max_dst_nb_samples;
    enum AVSampleFormat src_sample_fmt = AV_SAMPLE_FMT_DBL, dst_sample_fmt = AV_SAMPLE_FMT_S16;
    const char *dst_filename = NULL;
    FILE *dst_file;
    int dst_bufsize;
    const char *fmt;
    struct SwrContext *swr_ctx;
    double t;
    int ret;


    dst_filename = outFileName;

    dst_file = fopen(dst_filename, "wb");
    if (!dst_file) {
        fprintf(stderr, "Could not open destination file %s\n", dst_filename);
        exit(1);
    }
    // 1. 为重采样上下文SwrContext分配内存
    /* create resampler context */
    swr_ctx = swr_alloc();
    if (!swr_ctx) {
        fprintf(stderr, "Could not allocate resampler context\n");
        ret = AVERROR(ENOMEM);
        goto end;
    }

    // 2. 设置输入、输出参数
    /* set options */
    av_opt_set_int(swr_ctx, "in_channel_layout", src_ch_layout, 0);
    av_opt_set_int(swr_ctx, "in_sample_rate", src_rate, 0);
    av_opt_set_sample_fmt(swr_ctx, "in_sample_fmt", src_sample_fmt, 0);

    av_opt_set_int(swr_ctx, "out_channel_layout", dst_ch_layout, 0);
    av_opt_set_int(swr_ctx, "out_sample_rate", dst_rate, 0);
    av_opt_set_sample_fmt(swr_ctx, "out_sample_fmt", dst_sample_fmt, 0);

    // 3. 初始化重采样上下文SwrContext
    /* initialize the resampling context */
    if ((ret = swr_init(swr_ctx)) < 0) {
        fprintf(stderr, "Failed to initialize the resampling context\n");
        goto end;
    }

    // 4. 为输入和输出缓冲区分配内存
    /* allocate source and destination samples buffers */
    src_nb_channels = av_get_channel_layout_nb_channels(src_ch_layout);
    ret = av_samples_alloc_array_and_samples(&src_data, &src_linesize, src_nb_channels,
                                             src_nb_samples, src_sample_fmt, 0);
    if (ret < 0) {
        fprintf(stderr, "Could not allocate source samples\n");
        goto end;
    }

    /* compute the number of converted samples: buffering is avoided
     * ensuring that the output buffer will contain at least all the
     * converted input samples */
    max_dst_nb_samples = dst_nb_samples =
            av_rescale_rnd(src_nb_samples, dst_rate, src_rate, AV_ROUND_UP);

    /* buffer is going to be directly written to a rawaudio file, no alignment */
    dst_nb_channels = av_get_channel_layout_nb_channels(dst_ch_layout);
    ret = av_samples_alloc_array_and_samples(&dst_data, &dst_linesize, dst_nb_channels,
                                             dst_nb_samples, dst_sample_fmt, 0);
    if (ret < 0) {
        fprintf(stderr, "Could not allocate destination samples\n");
        goto end;
    }
    t = 0;
    do {
        // 5. 填充输入数据缓冲区
        /* generate synthetic audio */
        fill_samples((double *) src_data[0], src_nb_samples, src_nb_channels, src_rate, &t);

        /* compute destination number of samples */
        dst_nb_samples = av_rescale_rnd(swr_get_delay(swr_ctx, src_rate) +
                                        src_nb_samples, dst_rate, src_rate, AV_ROUND_UP);
        if (dst_nb_samples > max_dst_nb_samples) {
            av_freep(&dst_data[0]);
            ret = av_samples_alloc(dst_data, &dst_linesize, dst_nb_channels,
                                   dst_nb_samples, dst_sample_fmt, 1);
            if (ret < 0)
                break;
            max_dst_nb_samples = dst_nb_samples;
        }

        // 6. 格式转换
        /* convert to destination format */
        ret = swr_convert(swr_ctx, dst_data, dst_nb_samples, (const uint8_t **) src_data,
                          src_nb_samples);
        if (ret < 0) {
            fprintf(stderr, "Error while converting\n");
            goto end;
        }
        dst_bufsize = av_samples_get_buffer_size(&dst_linesize, dst_nb_channels,
                                                 ret, dst_sample_fmt, 1);
        if (dst_bufsize < 0) {
            fprintf(stderr, "Could not get sample buffer size\n");
            goto end;
        }
        printf("t:%f in:%d out:%d\n", t, src_nb_samples, ret);
        // 7. 写入文件
        fwrite(dst_data[0], 1, dst_bufsize, dst_file);
    } while (t < 10);

    // 8. 刷新采样缓冲区
    ret = swr_convert(swr_ctx, dst_data, dst_nb_samples, NULL, 0);
    if (ret < 0) {
        fprintf(stderr, "Error while converting\n");
        goto end;
    }
    dst_bufsize = av_samples_get_buffer_size(&dst_linesize, dst_nb_channels,
                                             ret, dst_sample_fmt, 1);
    if (dst_bufsize < 0) {
        fprintf(stderr, "Could not get sample buffer size\n");
        goto end;
    }
    printf("flush in:%d out:%d\n", 0, ret);
    fwrite(dst_data[0], 1, dst_bufsize, dst_file);

    if ((ret = get_format_from_sample_fmt(&fmt, dst_sample_fmt)) < 0)
        goto end;
    fprintf(stderr, "Resampling succeeded. Play the output file with the command:\n"
                    "ffplay -f %s -channel_layout %"PRId64" -channels %d -ar %d %s\n",
            fmt, dst_ch_layout, dst_nb_channels, dst_rate, dst_filename);

    end:
    fclose(dst_file);

    if (src_data)
        av_freep(&src_data[0]);
    av_freep(&src_data);

    if (dst_data)
        av_freep(&dst_data[0]);
    av_freep(&dst_data);

    swr_free(&swr_ctx);
    return ret < 0;
}

int audioSampleResampler(const char *outFileName){
    // 输入参数
    int64_t src_ch_layout = AV_CH_LAYOUT_STEREO;
    int src_rate = 48000;
    enum AVSampleFormat src_sample_fmt = AV_SAMPLE_FMT_DBL;
    int src_nb_channels = 0;
    uint8_t **src_data = NULL;  // 二级指针
    int src_linesize;
    int src_nb_samples = 1024;


    // 输出参数
    int64_t dst_ch_layout = AV_CH_LAYOUT_STEREO;
    int dst_rate = 44100;
    enum AVSampleFormat dst_sample_fmt = AV_SAMPLE_FMT_S16;
    int dst_nb_channels = 0;
    uint8_t **dst_data = NULL;  //二级指针
    int dst_linesize;
    int dst_nb_samples = 1152; // 固定为1152

    // 输出文件
    const char *dst_filename = NULL;
    FILE *dst_file;
    int dst_bufsize;
    const char *fmt;

    // 重采样实例
    AudioResampleParam resampler_params;
    resampler_params.src_channel_layout = src_ch_layout;
    resampler_params.src_sample_fmt = src_sample_fmt;
    resampler_params.src_sample_rate = src_rate;

    resampler_params.dst_channel_layout = dst_ch_layout;
    resampler_params.dst_sample_fmt = dst_sample_fmt;
    resampler_params.dst_sample_rate = dst_rate;

    AudioResampler *resampler = audio_resampler_alloc(resampler_params);
    if (!resampler) {
        printf("audio_resampler_alloc failed\n");
        goto end;
    }

    double t;
    int ret;


    dst_filename = outFileName;

    dst_file = fopen(dst_filename, "wb");
    if (!dst_file) {
        fprintf(stderr, "Could not open destination file %s\n", dst_filename);
        exit(1);
    }


    /* allocate source and destination samples buffers */
    // 计算出输入源的通道数量
    src_nb_channels = av_get_channel_layout_nb_channels(src_ch_layout);
    // 给输入源分配内存空间
    ret = av_samples_alloc_array_and_samples(&src_data, &src_linesize, src_nb_channels,
                                             src_nb_samples, src_sample_fmt, 0);
    if (ret < 0) {
        fprintf(stderr, "Could not allocate source samples\n");
        goto end;
    }


    /* buffer is going to be directly written to a rawaudio file, no alignment */
    dst_nb_channels = av_get_channel_layout_nb_channels(dst_ch_layout);
    dst_nb_samples = 1152;      // 指定每次获取1152的数据
    // 分配输出缓存内存
    ret = av_samples_alloc_array_and_samples(&dst_data, &dst_linesize, dst_nb_channels,
                                             dst_nb_samples, dst_sample_fmt, 0);
    if (ret < 0) {
        fprintf(stderr, "Could not allocate destination samples\n");
        goto end;
    }

    t = 0;
    int64_t in_pts = 0;
    int64_t out_pts = 0;
    do {
        /* generate synthetic audio */
        // 生成输入源
        fill_samples((double *) src_data[0], src_nb_samples, src_nb_channels, src_rate, &t);

        audio_resampler_send_frame2(resampler, src_data, src_nb_samples, in_pts);
        in_pts += src_nb_samples;
        int ret_size = audio_resampler_receive_frame2(resampler, dst_data, dst_nb_samples, &out_pts);

        if (ret_size > 0) {
            dst_bufsize = av_samples_get_buffer_size(&dst_linesize, dst_nb_channels,
                                                     ret_size, dst_sample_fmt, 1);
            if (dst_bufsize < 0) {
                fprintf(stderr, "Could not get sample buffer size\n");
                goto end;
            }
            printf("t:%f in:%d out:%d, out_pts:%"PRId64"\n", t, src_nb_samples, ret_size, out_pts);
            fwrite(dst_data[0], 1, dst_bufsize, dst_file);
        } else {
            printf("can't get %d samples, ret_size:%d, cur_size:%d\n", dst_nb_samples, ret_size,
                   audio_resampler_get_fifo_size(resampler));
        }
    } while (t < 10);  // 调整t观察内存使用情况

    // flush  只有对数据完整度要求非常高的场景才需要做flush
    audio_resampler_send_frame2(resampler, NULL, 0, 0);
    int fifo_size = audio_resampler_get_fifo_size(resampler);
    int get_size = (fifo_size > dst_nb_samples ? dst_nb_samples : fifo_size);
    int ret_size = audio_resampler_receive_frame2(resampler, dst_data, get_size, &out_pts);
    if (ret_size > 0) {
        printf("flush ret_size:%d\n", ret_size);
        // 不够一帧的时候填充为静音, 这里的目的是补偿最后一帧如果不够采样点数不足1152，用静音数据进行补足
        av_samples_set_silence(dst_data, ret_size, dst_nb_samples - ret_size, dst_nb_channels,
                               dst_sample_fmt);
        dst_bufsize = av_samples_get_buffer_size(&dst_linesize, dst_nb_channels,
                                                 dst_nb_samples, dst_sample_fmt, 1);
        if (dst_bufsize < 0) {
            fprintf(stderr, "Could not get sample buffer size\n");
            goto end;
        }
        printf("flush in:%d out:%d, out_pts:%"PRId64"\n", 0, ret_size, out_pts);
        fwrite(dst_data[0], 1, dst_bufsize, dst_file);
    }


    if ((ret = get_format_from_sample_fmt(&fmt, dst_sample_fmt)) < 0)
        goto end;
    fprintf(stderr, "Resampling succeeded. Play the output file with the command:\n"
                    "ffplay -f %s -channel_layout %"PRId64" -channels %d -ar %d %s\n",
            fmt, dst_ch_layout, dst_nb_channels, dst_rate, dst_filename);

    end:
    fclose(dst_file);

    if (src_data)
        av_freep(&src_data[0]);
    av_freep(&src_data);

    if (dst_data)
        av_freep(&dst_data[0]);
    av_freep(&dst_data);

    if (resampler) {
        audio_resampler_free(resampler);
    }
    printf("finish\n");
    return ret < 0;
}