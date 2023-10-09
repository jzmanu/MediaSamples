//
// Created by jzman on 2023/9/15.
//

#include "av_base.h"

void testFfmpeg() {
    printf("avformat version:%d\n", avformat_version());
    printf("avformat configuration:%s\n", avformat_configuration());
}

void printAVFrameInfo(AVFrame *frame){
    printf("sample_rate:%d\n", frame->sample_rate);
    printf("channels:%d\n", frame->channels);
    printf("format:%d\n", frame->format);
    printf("format name:%s\n", av_get_sample_fmt_name(frame->format));
    printf("format bytes:%d\n", av_get_bytes_per_sample(frame->format));
    printf("format isPlanar:%d\n", av_sample_fmt_is_planar(frame->format));
    printf("width:%d\n", frame->width);
    printf("height:%d\n", frame->height);
}

char * avGetErr(int ret){
    static char err_buf[128] = {0};
    av_strerror(ret, err_buf, 128);
    return err_buf;
}
