//
// Created by jzman on 2023/9/15.
//

#ifndef DECODE_AUDIO_VIDEO_SAMPLE_H
#define DECODE_AUDIO_VIDEO_SAMPLE_H

/**
 * 视频输入缓冲区的大小
 */
#define VIDEO_INPUT_BUF_SIZE 20480

/**
 * 视频填充阈值的大小
 */
#define VIDEO_REFILL_THRESH 4096

/**
 * 解码视频
 * aac文件解析成pcm文件
 * @param inFileName
 * @param outFileName
 * @return
 */
int decodeVideo(const char *inFileName, const char *outFileName);

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
void startVideoDecode(AVCodecContext *ctx, AVPacket *avPkt, AVFrame * avFrame, FILE *outFile);

#endif // DECODE_AUDIO_VIDEO_SAMPLE_H
