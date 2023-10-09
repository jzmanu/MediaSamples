#include <stdio.h>
#include "libavformat/avformat.h"
#include "audio_sample.h"
#include "video_sample.h"
#include "avio_audio_sample.h"

int main(int argc, char **argv) {
    // 从命令行接收参数
    if (argc <= 2) {
        fprintf(stderr, "Usage: %s <input file> <output file>\n", argv[0]);
        exit(1);
    }
    const char *inFileName = argv[1];
    const char *outFileName = argv[2];

    // audio sample
//    decodeAudio(inFileName,outFileName);
    // video sample
//    decodeVideo(inFileName, outFileName);
    // 自定义IO
    decodeAudioWithIO(inFileName, outFileName);
}