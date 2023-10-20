cmake_minimum_required(VERSION 3.26)
project(ffmpeg_resample_audio C)

set(CMAKE_C_STANDARD 11)

# 头文件
include_directories(
        E:/msys64/home/ffmpeg/build/ffmepg-4.2/include
        src/base
        src/resampler
        src/sample)
# 库文件
link_directories(E:/msys64/home/ffmpeg/build/ffmepg-4.2/lib)

add_executable(ffmpeg_resample_audio
        main.c
        src/base/audio_resample_base.c
        src/resampler/audio_resample.c
        src/sample/sample.c)

target_link_libraries(
        ffmpeg_resample_audio
        avcodec
        avdevice
        avfilter
        avformat
        avutil
        postproc
        swresample
        swscale
)
