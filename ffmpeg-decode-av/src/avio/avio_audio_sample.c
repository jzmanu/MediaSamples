//
// Created by jzman on 2023/10/9.
//
#include "avio_audio_sample.h"

int decodeAudioWithIO(const char *inFileName, const char *outFileName) {
    FILE *inFile, *outFile;
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

    // 自定义IO
    uint8_t *ioBuffer = av_malloc(AUDIO_INPUT_BUF_SIZE);
    AVIOContext *ioCtx = avio_alloc_context(ioBuffer, AUDIO_INPUT_BUF_SIZE, 0,
                                            inFile, read_packet, NULL, NULL);
    if (!ioCtx){
        fprintf(stderr, "avio_alloc_context failed.\n");
        exit(1);
    }
    AVFormatContext *formatCtx = avformat_alloc_context();
    if (!formatCtx) {
        fprintf(stderr, "avformat_alloc_context failed.\n");
        exit(1);
    }
    formatCtx->pb = ioCtx;
    int ret = avformat_open_input(&formatCtx, NULL, NULL,NULL);
    if (ret < 0){
        fprintf(stderr, "avformat_open_input failed, err:%s\n", avGetErr(ret));
        exit(1);
    }

    // 查找解码器：aac和mp3
    enum AVCodecID avcodecId = AV_CODEC_ID_AAC;
    if (strstr(inFileName, "mp3") != NULL) {
        avcodecId = AV_CODEC_ID_MP3;
    }
    AVCodec *codec = avcodec_find_decoder(avcodecId);
    if (!codec) {
        fprintf(stderr, "avcodec_find_decoder failed.\n");
        exit(1);
    }

    // 分配解码器上下文
    AVCodecContext *codecCtx = avcodec_alloc_context3(codec);
    if (!codecCtx) {
        fprintf(stderr, "avcodec_alloc_context3 failed.\n");
        exit(1);
    }

    // 打开解码器、关联解码器上下文
    ret = avcodec_open2(codecCtx, codec, NULL);
    if (ret < 0) {
        fprintf(stderr, "avcodec_open2 failed, err:%s\n", avGetErr(ret));
        exit(1);
    }

    // 解码
    AVPacket *pkt = av_packet_alloc();
    if (!pkt){
        fprintf(stderr, "av_packet_alloc failed.\n");
        exit(1);
    }
    AVFrame *frame = av_frame_alloc();
    if (!frame){
        fprintf(stderr, "av_frame_alloc failed.\n");
        exit(1);
    }
    while (1){
        ret = av_read_frame(formatCtx, pkt);
        if (ret < 0){
            break;
        }
        startAudioDecode(codecCtx, pkt, frame, outFile);
    }

    printf("finish\n");
    // 刷新解码器
    startAudioDecode(codecCtx,NULL,frame,outFile);
    // 释放资源
    fclose(inFile);
    fclose(outFile);

    av_free(ioBuffer);
    av_free(pkt);
    av_free(frame);

    avformat_close_input(&formatCtx);
    avcodec_free_context(&codecCtx);
    avio_context_free(&ioCtx);
    return 0;
}

int read_packet(void *opaque, uint8_t *buf, int buf_size) {
    FILE *inFIle = (FILE *) opaque;
    size_t readSize = fread(buf, 1, buf_size, inFIle);
    if (readSize <= 0) {
        printf("read_packet data read end.\n");
        return AVERROR_EOF;  // 数据读取完毕
    }
    printf("read_packet readSize:%zu ,buf_size:%d\n", readSize, buf_size);
    return (int) readSize;
}
