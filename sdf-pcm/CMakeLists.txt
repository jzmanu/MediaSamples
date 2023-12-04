cmake_minimum_required(VERSION 3.26)
project(sdf_pcm C)

set(CMAKE_C_STANDARD 11)

# 头文件
include_directories(E:/msys64/mingw64/include/SDL2)
# 库文件
link_directories(E:/msys64/mingw64/lib)
add_executable(sdl_pcm main.c)
target_link_libraries(sdl_pcm mingw32 SDL2 SDL2main)
