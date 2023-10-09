cmake_minimum_required(VERSION 3.26)
project(ffmpeg_decode_av C)

set(CMAKE_C_STANDARD 11)
# 头文件
include_directories(
        E:/msys64/home/ffmpeg/build/ffmepg-4.2/include
        src/base
        src/video
        src/audio
)
# 库文件
link_directories(E:/msys64/home/ffmpeg/build/ffmepg-4.2/lib)

add_executable(ffmpeg_decode_av
        main.c
        src/base/av_base.c
        src/audio/audio_sample.c
        src/video/video_sample.h
        src/video/video_sample.c
)

target_link_libraries(
        ffmpeg_decode_av
        avcodec
        avdevice
        avfilter
        avformat
        avutil
        postproc
        swresample
        swscale
)
