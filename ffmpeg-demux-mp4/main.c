#include <stdio.h>
#include <stdlib.h>
#include "libavformat/avformat.h"

#define ERROR_STRING_SIZE 1024

// 采样率
const int sampleFrequencies[] = {
        96000,  // 0x0
        88200,  // 0x1
        64000,  // 0x2
        48000,  // 0x3
        44100,  // 0x4
        32000,  // 0x5
        24000,  // 0x6
        22050,  // 0x7
        16000,  // 0x8
        12000,  // 0x9
        11025,  // 0xa
        8000,   // 0xb
        7350    // 0x3
        // d e f是保留的
};

/**
 * 填充ADTS Header
 * 这里保留了一些分析过程
 * @brief adts_header
 * @param adtsHeaders
 * @param dataLength
 * @param profile
 * @param sampleRate
 * @param channels
 */
void adts_header(char * const adtsHeaders, const int dataLength, const int profile,
                 const int sampleRate, const int channels){
    printf("(%d) adts_header dataLength:%d\n",__LINE__,dataLength);
    // 默认48000Hz
    int sampleFrequenciesIndex = 3;
    // 遍历获取实际的采样率索引
    int size = sizeof (sampleFrequencies) / sizeof (sampleFrequencies[0]);
    for(int index = 0; index < size; index++){
        if(sampleRate == sampleFrequencies[index]){
            sampleFrequenciesIndex = index;
            break;
        }
    }
    printf("(%d) adts_header sampleFrequencieIndex:%d\n",__LINE__,sampleFrequenciesIndex);

    // ADTS帧的长度 = 真实数据长度 + ADTS Heder长度
    int adtsLen = dataLength + 7;

    // 填充ADTS Header
    // 0            1           2           3           4           5           6
    // 1111 1111    1111 0001   11001100
    // 0XFF         0XF1
    //         11   001100
    // 11000000       001100
    //   001100   00001111
    //            00000011
    adtsHeaders[0] = 0XFF;          // syncword高八位
//    adtsHeaders[1] = 0XF1;
    adtsHeaders[1] = 0XF0;          // syncword低八位
    adtsHeaders[1] |= (0 << 3);     // ID,0表示MPEG-4
    adtsHeaders[1] |= (0 << 0);     // layer,AAC始终为00，2位
    adtsHeaders[1] |= 1;            // protection_absent，1表示无误码校验，ADTS Header 7个字节


    adtsHeaders[2] = (profile << 6);// profile，2位
//    adtsHeaders[2] |= (sampleFrequencieIndex << 2);     // sampling_frequency_index，采样率索引，4位
    adtsHeaders[2] |= (sampleFrequenciesIndex & 0x0f) << 2;
    adtsHeaders[2] |= (0 << 2);                         // private_bit,表示无私有位
    //  0000 0100   0x4
    //& 0000 0111
    //  0000 0100   >>2 = 0000 0001
    adtsHeaders[2] |=(channels & 0x4) >> 2;             // channel_configuration，取其高位1位，3位


    //  0000 0011   0x3
    //& 0000 0111
    //  0000 0011   >>2 = 0000 0001
    adtsHeaders[3] = (channels & 0x3) << 6;             // channel_configuration，取其低1位，3位
    adtsHeaders[3] |= (0 << 5);                         // original/copy
    adtsHeaders[3] |= (0 << 4);                         // home
    adtsHeaders[3] |= (0 << 3);                         // copyright_identification_bit
    adtsHeaders[3] |= (0 << 2);                         // copyright_identification_start
    //      1100 0000 0000 0     0x1800
    // &    1111 1111 1111 1
    //      1100 0000 0000 0    >>11
    adtsHeaders[3] |= ((adtsLen & 0x1800) >> 11);         // frame_length,去最高位2位

    //      0011 1111 1100 0     0x7f8
    // &    1111 1111 1111 1
    //      0011 1111 1100 0    >>3  1111 1111
    adtsHeaders[4] = (uint8_t)((adtsLen & 0x7f8) >> 3);            // frame_length,高2位之后的次8位

    //      0000 0000 0011 1     0x7
    // &    1111 1111 1111 1
    //      0000 0000 0111 1    <<5  1110 0000
    adtsHeaders[5] = (uint8_t)((adtsLen & 0x7) << 5);              // frame_length,低3位
    //      111 1100 0000     0x7c0
    // &    111 1111 1111     0x7ff
    //      111 1100 0000    <<6  1111 1
    adtsHeaders[5] |= (0x7ff & 0x7c0) >> 6;             // adts_buffer_fullness,取高5位
    adtsHeaders[5] |= 0x1f;

    //      0000 0111 111     0x1f
    // &    1111 1111 111     0x7ff
    //      0000 0111 111    <<6  1111 1
//    adtsHeaders[6] = (uint8_t)((0x1f & 0x7ff) << 2);   // adts_buffer_fullness,取低6位
    adtsHeaders[6] = 0xfc;                               // number_of_raw_data_blocks_in_frame，2位
    printf("(%d) adts_header adtsHeaders[6]:%X\n",__LINE__,adtsHeaders[6]);
}

/**
 * 从mp4文件中解析出h264和aac文件
 * @param argc
 * @param argv
 * @return
 */
int main(int argc, char **argv) {
    // 从命令行接收参数
    if (argc <= 2) {
        fprintf(stderr, "Usage: %s <input file> <output1 file> <output2 file>\n", argv[0]);
        exit(1);
    }
    const char *inFileName = argv[1];
    const char *out1FileName = argv[2];
    const char *out2FileName = argv[3];

    FILE *h264File = NULL;
    FILE *aacFile = NULL;
    h264File = fopen(out1FileName, "wb");
    if (!h264File) {
        fprintf(stderr, "h264File open failed.\n");
        exit(1);
    }
    aacFile = fopen(out2FileName, "wb");
    if (!aacFile) {
        fprintf(stderr, "aacFile open failed.\n");
        exit(1);
    }

    char error[ERROR_STRING_SIZE+1];
    AVFormatContext *ifmtCtx = NULL;
    int ret, audioIndex, videoIndex;
    AVPacket *pkt = NULL;

    // 1. 分配解复用器上下文
    ifmtCtx = avformat_alloc_context();
    if (!ifmtCtx) {
        fprintf(stderr, "avformat_alloc_context failed.\n");
        exit(1);
    }

    // 2. 打开本地文件或网络流
    ret = avformat_open_input(&ifmtCtx, inFileName, NULL, NULL);
    if (ret < 0) {
        fprintf(stderr, "avformat_open_input failed.\n");
        exit(1);
    }

    // 3. 读取媒体文件的部分数据包获取码流信息
    ret = avformat_find_stream_info(ifmtCtx, NULL);
    if (ret < 0) {
        av_strerror(ret, error, ERROR_STRING_SIZE);
        fprintf(stderr, "avformat_find_stream_info failed, ret:%d error:%s\n", ret, error);
        exit(1);
    }

    // 4. 获取音频流和视频流的索引
    audioIndex = av_find_best_stream(ifmtCtx, AVMEDIA_TYPE_AUDIO,
                                     -1, -1, NULL, 0);
    if (audioIndex < 0) {
        av_strerror(audioIndex, error, ERROR_STRING_SIZE);
        fprintf(stderr, "av_find_best_stream audio failed, ret:%d error:%s\n", ret, error);
        exit(1);
    }

    videoIndex = av_find_best_stream(ifmtCtx, AVMEDIA_TYPE_VIDEO,
                                     -1, -1, NULL, 0);
    if (videoIndex < 0) {
        av_strerror(videoIndex, error, ERROR_STRING_SIZE);
        fprintf(stderr, "av_find_best_stream video failed, ret:%d error:%s\n", ret, error);
        exit(1);
    }

    // 5. h264_mp4toannexb
    const AVBitStreamFilter *avBSFFilter = av_bsf_get_by_name("h264_mp4toannexb");
    if (!avBSFFilter) {
        fprintf(stderr, "get h264_mp4toannexb filter by av_bsf_get_by_name failed.\n");
        exit(1);
    }
    AVBSFContext *avBtx = NULL;
    ret = av_bsf_alloc(avBSFFilter, &avBtx);
    if (ret < 0) {
        av_strerror(ret, error, ERROR_STRING_SIZE);
        fprintf(stderr, "av_bsf_alloc failed, ret:%d error:%s\n", ret, error);
        exit(1);
    }
    ret = avcodec_parameters_copy(avBtx->par_in, ifmtCtx->streams[videoIndex]->codecpar);
    if (ret < 0) {
        av_strerror(ret, error, ERROR_STRING_SIZE);
        fprintf(stderr, "avcodec_parameters_copy failed, ret:%d error:%s\n", ret, error);
        exit(1);
    }
    ret = av_bsf_init(avBtx);
    if (ret < 0) {
        av_strerror(ret, error, ERROR_STRING_SIZE);
        fprintf(stderr, "av_bsf_init failed, ret:%d error:%s\n", ret, error);
        exit(1);
    }

    // 6. 循环从文件中读取数据
    pkt = av_packet_alloc();
    av_init_packet(pkt);
    while (1) {
        ret = av_read_frame(ifmtCtx, pkt);
        if (ret < 0) {
            av_strerror(ret, error, ERROR_STRING_SIZE);
            fprintf(stderr, "av_read_frame failed, ret:%d error:%s\n", ret, error);
            break;
        }
        // av_read_frame成功读取到AVPacket，外部需要释放pkt释放
        if (pkt->stream_index == AVMEDIA_TYPE_AUDIO){
            // 写入AAC ADTS Header
            char adtsHeader[7] = {0};
            adts_header(adtsHeader,pkt->size,
                        ifmtCtx->streams[AVMEDIA_TYPE_AUDIO]->codecpar->profile,
                        ifmtCtx->streams[AVMEDIA_TYPE_AUDIO]->codecpar->sample_rate,
                        ifmtCtx->streams[AVMEDIA_TYPE_AUDIO]->codecpar->channels);
            fwrite(adtsHeader,1,7, aacFile);
            // 写入AAC data
            fwrite(pkt->data,1, pkt->size, aacFile);
            av_packet_unref(pkt);
        }else if (pkt->stream_index == AVMEDIA_TYPE_VIDEO){
            ret = av_bsf_send_packet(avBtx,pkt);
            if (ret < 0) {
                av_strerror(ret, error, ERROR_STRING_SIZE);
                fprintf(stderr, "av_bsf_send_packet failed, ret:%d error:%s\n", ret, error);
                av_packet_unref(pkt);
                continue;
            }
            while (1){
                ret = av_bsf_receive_packet(avBtx, pkt);
                if (ret != 0){
                    break;
                }
                size_t size = fwrite(pkt->data,1, pkt->size, h264File);
                if (size != pkt->size){
                    fprintf(stderr, "[video] fwrite pkt->size isn't equal already write length, pkt->size:%d write size:%zu\n", pkt->size, size);
                }
                av_packet_unref(pkt);
            }
        }else{
            av_packet_unref(pkt);
        }
    }
    // 7. 释放资源
    fclose(aacFile);
    fclose(h264File);
    avformat_close_input(&ifmtCtx);
    if (!pkt){
        av_packet_unref(pkt);
    }
    av_bsf_free(&avBtx);
    return 0;
}
