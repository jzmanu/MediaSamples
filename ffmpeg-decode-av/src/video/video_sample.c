//
// Created by jzman on 2023/9/18.
//
#include <libavcodec/avcodec.h>
#include "video_sample.h"
#include "av_base.h"

int decodeVideo(const char *inFileName, const char *outFileName) {
    const AVCodec *codec;
    AVCodecParserContext *codecParseContext = NULL;
    AVCodecContext *codecContext = NULL;
    FILE *inFile, *outFile;
    uint8_t inBuf[VIDEO_INPUT_BUF_SIZE + AV_INPUT_BUFFER_PADDING_SIZE];
    uint8_t *data;
    size_t dataSize, len;
    AVPacket *pkt;
    AVFrame *decodeFrame;
    int ret;
    pkt = av_packet_alloc();

    // 打开输入文件
    inFile = fopen(inFileName, "rb");
    if (!inFile) {
        fprintf(stderr, "Could not open input file.\n");
        exit(1);
    }
    // 打开输出文件
    outFile = fopen(outFileName, "wb");
    if (!outFile) {
        fprintf(stderr, "Could not open output file.\n");
        exit(1);
    }

    // 1. 查找解码器：aac和mp3
    enum AVCodecID avcodecId = AV_CODEC_ID_H264;
    if (strstr(inFileName, "mpeg2") != NULL) {
        avcodecId = AV_CODEC_ID_MPEG2VIDEO;
    }
    codec = avcodec_find_decoder(avcodecId);
    if (!codec) {
        fprintf(stderr, "Codec not found.\n");
        exit(1);
    }

    // 2. 初始化解码器对应的裸流的解析器
    // AVCodecParserContext(数据)  +  AVCodecParser(方法)
    codecParseContext = av_parser_init(avcodecId);
    if (!codecParseContext) {
        fprintf(stderr, "Parser not found.\n");
        exit(1);
    }

    // 3. 分配解码器上下文
    codecContext = avcodec_alloc_context3(codec);
    if (!codecContext) {
        fprintf(stderr, "Could not allocate audio codec context.\n");
        exit(1);
    }

    // 4. 打开解码器、关联解码器上下文
    if (avcodec_open2(codecContext, codec, NULL) < 0) {
        fprintf(stderr, "Could not open Codec.\n");
        exit(1);
    }

    // 4. 解码直到文件结束
    data = inBuf;
    // 从输入文件中
    dataSize = fread(inBuf, 1, VIDEO_INPUT_BUF_SIZE, inFile);
    while (dataSize > 0) {
        if (!decodeFrame) {
            if (!(decodeFrame = av_frame_alloc())) {
                fprintf(stderr, "Could not alloc.\n");
                exit(1);
            }
        }
        // 解析出一个完整的AVPacket
        ret = av_parser_parse2(codecParseContext, codecContext,
                               &pkt->data, &pkt->size,
                               data, dataSize, AV_NOPTS_VALUE, AV_NOPTS_VALUE, 0);
        if (ret < 0) {
            fprintf(stderr, "Error while  parsing.\n");
            exit(1);
        }

        data += ret;    // 跳过已经处理了的数据
        dataSize -= ret; // 从读取数据中减去已经处理过的数据

        if (pkt->size) {
            // 已经有完整的AVPacket了
            startVideoDecode(codecContext, pkt, decodeFrame, outFile);
        }

        // 音频数据低于阈值，从文件中读取数据填充音频缓冲区
        if (dataSize < VIDEO_REFILL_THRESH) {
            memmove(inBuf, data, dataSize);
            data = inBuf;
            len = fread(data + dataSize, 1, VIDEO_INPUT_BUF_SIZE - dataSize, inFile);
            if (len > 0) {
                dataSize += len;
            }
        }
    }

    // 5. 刷新解码器
    pkt->data = NULL;
    pkt->size = 0;
    startVideoDecode(codecContext, pkt, decodeFrame, outFile);

    // 6. 释放资源
    fclose(outFile);
    fclose(inFile);
    avcodec_free_context(&codecContext);
    av_parser_close(codecParseContext);
    av_frame_free(&decodeFrame);
    av_packet_free(&pkt);
}

void startVideoDecode(AVCodecContext *ctx, AVPacket *avPkt, AVFrame *avFrame, FILE *outFile) {
    int i, channel;
    int ret, dataSize;
    // 将包含压缩数据的AVPacket发送到解码器
    ret = avcodec_send_packet(ctx, avPkt);
    if (ret < 0) {
        fprintf(stderr, "Error submitting the packet to the decoder.\n");
        exit(1);
    }
    while (1) {
        // 获取到解码后的AVFrame数据。
        ret = avcodec_receive_frame(ctx, avFrame);
        // AVERROR_EOF：表示所有的帧已经被输出，已经到达流的末尾，一次或多次调用avcodec_receive_frame即可出发该状态
        // AVERROR(EAGAIN)：表示需要更多的AVPacket才能输出AVFrame
        if (ret == AVERROR_EOF || ret == AVERROR(EAGAIN)) {
            return;
        } else if (ret < 0) {
            fprintf(stderr, "Error during decoding.\n");
            exit(1);
        }
        static int printFlag = 0;
        if (printFlag == 0) {
            printAVFrameInfo(avFrame);
            printFlag = 1;
        }

        // 一般H264默认为 AV_PIX_FMT_YUV420P, 具体怎么强制转为 AV_PIX_FMT_YUV420P 在音视频合成输出的时候讲解
        // frame->linesize[1]  对齐的问题
        // 正确写法  linesize[]代表每行的字节数量，所以每行的偏移是linesize[]
        for (int j = 0; j < avFrame->height; j++)
            fwrite(avFrame->data[0] + j * avFrame->linesize[0], 1, avFrame->width, outFile);
        for (int j = 0; j < avFrame->height / 2; j++)
            fwrite(avFrame->data[1] + j * avFrame->linesize[1], 1, avFrame->width / 2, outFile);
        for (int j = 0; j < avFrame->height / 2; j++)
            fwrite(avFrame->data[2] + j * avFrame->linesize[2], 1, avFrame->width / 2, outFile);
    }
}
