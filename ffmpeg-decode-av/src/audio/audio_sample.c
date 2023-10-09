//
// Created by jzman on 2023/9/15.
//
#include "audio_sample.h"

int decodeAudio(const char *inFileName, const char *outFileName){
    const AVCodec *codec;
    AVCodecParserContext *codecParseContext = NULL;
    AVCodecContext *codecContext = NULL;
    FILE *inFile, *outFile;
    uint8_t inBuf[AUDIO_INPUT_BUF_SIZE + AV_INPUT_BUFFER_PADDING_SIZE];
    uint8_t *data;
    size_t dataSize, len;
    AVPacket *pkt;
    AVFrame  *decodeFrame;
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
    enum AVCodecID avcodecId = AV_CODEC_ID_AAC;
    if (strstr(inFileName, "mp3") != NULL) {
        avcodecId = AV_CODEC_ID_MP3;
    }
    codec = avcodec_find_decoder(avcodecId);
    if (!codec) {
        fprintf(stderr, "Codec not found.\n");
        exit(1);
    }

    // 2. 初始化解码器对应的裸流的解析器
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
    dataSize = fread(inBuf, 1, AUDIO_INPUT_BUF_SIZE, inFile);

    while (dataSize > 0){
        if (!decodeFrame){
            if (!(decodeFrame = av_frame_alloc())){
                fprintf(stderr,"Could not alloc.\n");
                exit(1);
            }
        }
        // 解析出一个完整的AVPacket
        ret = av_parser_parse2(codecParseContext,codecContext,
                               &pkt->data, &pkt->size,
                               data,dataSize, AV_NOPTS_VALUE, AV_NOPTS_VALUE, 0);
        if (ret < 0){
            fprintf(stderr, "Error while  parsing.\n");
            exit(1);
        }

        data += ret;
        dataSize-= ret;

        if (pkt->size){
            // 已经有完整的AVPacket了
            startAudioDecode(codecContext, pkt, decodeFrame, outFile);
        }

        // 音频数据低于阈值，从文件中读取数据填充音频缓冲区
        if (dataSize < AUDIO_REFILL_THRESH){
            memmove(inBuf, data, dataSize);
            data = inBuf;
            len = fread(data + dataSize,1, AUDIO_INPUT_BUF_SIZE-dataSize, inFile);
            if (len > 0){
                dataSize += len;
            }
        }
    }
    // 5. 刷新解码器
    pkt->data = NULL;
    pkt->size = 0;
    startAudioDecode(codecContext,pkt,decodeFrame, outFile);

    // 6. 释放资源
    fclose(outFile);
    fclose(inFile);
    avcodec_free_context(&codecContext);
    av_parser_close(codecParseContext);
    av_frame_free(&decodeFrame);
    av_packet_free(&pkt);
    return 0;
}

void startAudioDecode(AVCodecContext *ctx, AVPacket *avPkt, AVFrame * avFrame, FILE *outFile){
    int i,channel;
    int ret, dataSize;
    // 将包含压缩数据的AVPacket发送到解码器
    ret = avcodec_send_packet(ctx, avPkt);
    if (ret < 0){
        fprintf(stderr, "Error submitting the packet to the decoder.\n");
        exit(1);
    }
    while (1){
        // 获取到解码后的AVFrame数据。
        ret = avcodec_receive_frame(ctx, avFrame);
        // AVERROR_EOF：表示所有的帧已经被输出，已经到达流的末尾，一次或多次调用avcodec_receive_frame即可出发该状态
        // AVERROR(EAGAIN)：表示需要更多的AVPacket才能输出AVFrame
        if (ret == AVERROR_EOF || ret == AVERROR(EAGAIN)){
            return;
        }else if (ret < 0){
            fprintf(stderr, "Error during decoding.\n");
            exit(1);
        }
        static int printFlag = 0;
        if (printFlag == 0){
            printAVFrameInfo(avFrame);
            printFlag = 1;
        }
        // 获取指定量化格式的单个样本使用的字节数量
        dataSize = av_get_bytes_per_sample(ctx->sample_fmt);
        if (dataSize == 0){
            fprintf(stderr, "sample_fmt is unknown.\n");
            exit(1);
        }

        /**
            P表示Planar（平面），其数据格式排列方式为 :
            LLLLLLRRRRRRLLLLLLRRRRRRLLLLLLRRRRRRL...（每个LLLLLLRRRRRR为一个音频帧）
            而不带P的数据格式（即交错排列）排列方式为：
            LRLRLRLRLRLRLRLRLRLRLRLRLRLRLRLRLRLRL...（每个LR为一个音频样本）
         播放范例：ffplay -ar 48000 -ac 2 -f f32le believe.pcm
          */
        // planar模式frame.data[i]表示第i个声道的数据，声道数超过8要从frame.extended_data中获取
        // packed模式frame.data[0]或frame.extended_data[0]包含所有的音频数据
        for (i = 0; i < avFrame->nb_samples; i++) {
            for (channel = 0; channel < ctx->channels; channel++){
                fwrite(avFrame->data[channel] + dataSize * i,1, dataSize, outFile);
            }
        }
    }
}