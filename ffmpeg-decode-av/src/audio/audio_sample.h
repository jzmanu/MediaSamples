//
// Created by jzman on 2023/9/15.
//

#ifndef DECODE_AV_AUDIO_SAMPLE_H
#define DECODE_AV_AUDIO_SAMPLE_H

#include "libavcodec/avcodec.h"
#include "av_base.h"
/**
 * 音频输入缓冲区的大小
 */
#define AUDIO_INPUT_BUF_SIZE 20480

/**
 * 音频填充阈值的大小
 */
#define AUDIO_REFILL_THRESH 4096

/**
 * 解码音频
 * ffplay -ar 48000 -ac 2 -f f32le believe.pcm
 * aac文件解析成pcm文件
 * @param inFileName
 * @param outFileName
 * @return
 */
int decodeAudio(const char *inFileName, const char *outFileName);

/**
 * 开始解码
 * 这个时候已经有完整的AVPacket了
 * 1. 发送AVPacket到解码器
 * 2. 循环接收解码后AVFrame
 * 3. 写入文件
 * @param context
 * @param pkt
 * @param frame
 * @param outFile
 */
void startAudioDecode(AVCodecContext *ctx, AVPacket *avPkt, AVFrame * avFrame, FILE *outFile);

#endif //DECODE_AV_AUDIO_SAMPLE_H
