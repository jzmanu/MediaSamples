//
// Created by jzman on 2023/10/9.
//

#ifndef FFMPEG_DECODE_AV_AVIO_AUDIO_SAMPLE_H
#define FFMPEG_DECODE_AV_AVIO_AUDIO_SAMPLE_H

#include "av_base.h"
#include "audio_sample.h"

/**
 * 解码音频
 * 内部自定义io读取数据
 * ffplay -ar 48000 -ac 2 -f f32le believe.pcm
 * aac文件解析成pcm文件
 * @param inFileName
 * @param outFileName
 * @return
 */
int decodeAudioWithIO(const char *inFileName, const char *outFileName);

/**
 * 自定义IO读取数据的函数
 * 这里从本地文件读取aac文件，用解码后的数据生成对应的pcm文件
 * 参见 avio_alloc_context 函数指针read_packet
 * @param opaque 用于传递参数给函数内部
 * @param buf 指向存储数据缓冲区的指针
 * @param buf_size
 * @return
 */
int read_packet(void *opaque, uint8_t *buf, int buf_size);


#endif //FFMPEG_DECODE_AV_AVIO_AUDIO_SAMPLE_H
