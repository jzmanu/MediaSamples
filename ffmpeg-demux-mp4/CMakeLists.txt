cmake_minimum_required(VERSION 3.26)
project(ffmpeg_demux_mp4 C)
set(CMAKE_C_STANDARD 11)

# 头文件
include_directories(E:/msys64/home/ffmpeg/build/ffmepg-4.2/include)
# 库文件
link_directories(E:/msys64/home/ffmpeg/build/ffmepg-4.2/lib)

add_executable(ffmpeg_demux_mp4 main.c)

target_link_libraries(
        ffmpeg_demux_mp4
        avcodec
        avdevice
        avfilter
        avformat
        avutil
        postproc
        swresample
        swscale
)
