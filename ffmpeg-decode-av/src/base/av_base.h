//
// Created by jzman on 2023/9/15.
//

#include <stdbool.h>
#include "libavformat/avformat.h"

#ifndef DECODE_AV_DECODE_BASE_H
#define DECODE_AV_DECODE_BASE_H

/**
 * 测试ffmpeg是否引入成功
 */
void testFfmpeg();

/**
 * 打印解码后的帧信息
 * @param frame
 */
void printAVFrameInfo(AVFrame *frame);

/**
 * 错误码转换
 * @param ret
 * @return
 */
char * avGetErr(int ret);

#endif // DECODE_AV_DECODE_BASE_H
