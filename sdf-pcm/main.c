#include <stdio.h>

/**
 * SDL2播放PCM
 *
 * 使用SDL2播放PCM音频采样数据。
 * SDL实际上是对底层绘图API（Direct3D，OpenGL）的封装，使用起来明显简单于直接调用底层
 * API。
 * 测试的PCM数据采用采样率44.1k, 采用精度S16SYS, 通道数2
 *
 * 函数调用步骤如下:
 *
 * [初始化]
 * SDL_Init(): 初始化SDL。
 * SDL_OpenAudio(): 根据参数（存储于SDL_AudioSpec）打开音频设备。
 * SDL_PauseAudio(): 播放音频数据。
 *
 * [循环播放数据]
 * SDL_Delay(): 延时等待播放完成。
 *
 * 想要的要实现的东西：
 * 1. 音频回调函数
 * 2. 打开pcm文件
 * 3. 利用pcm文件的信息填充SDL_AudioSpec
 * 4. 打开音频设备
 * 5. 播放音频数据
 *
 *
 *           audio_buf
 *        \________________\
 *   audio_pos      audio_end
 *
 */
#include "SDL.h"

// 每次读取2帧数据，以1024个采样点为一帧，2通道，16bit采样精度
#define PCM_BUFFER_SIZE 2 * 1024 * 2 * 2

// 音频PCM数据缓存
static Uint8 *audio_buf = NULL;
// 目前的读取位置
static Uint8 *audio_pos = NULL;
// 缓存结束位置
static Uint8 *audio_end = NULL;

// 音频回调函数声明
void sdl_fill_pcm(void *userdata, Uint8 *stream, int len);

#undef main

int main() {
    int ret = -1;
    FILE *pcmFile = NULL;
    // 音频PCM文件名
    const char *path = "D:/Study/AV/SDL/Sample/05-sdl-pcm/44100_16bit_2ch.pcm";
    SDL_AudioSpec audioSpec;
    // 每次读取的数据长度
    size_t read_buf_len = 0;
    // 初始化SDL为音频场景
    SDL_Init(SDL_INIT_AUDIO);
    // 打开PCM文件
    pcmFile = fopen(path, "rb");

    if (!pcmFile) {
        printf("can't open pacm file.\n");
        goto _FAIL;
    }

    // 分配内存
    audio_buf = (uint8_t *) malloc(PCM_BUFFER_SIZE);

    // 填充SDL_AudioSpec
    audioSpec.freq = 44100;
    audioSpec.format = AUDIO_S16SYS;
    audioSpec.channels = 2;
    audioSpec.samples = 1024;   // 1024/44100 = 0.0232199546485261 约等于23.2ms,也就是23.2ms至少回调一次
    audioSpec.silence = 0;
    audioSpec.callback = sdl_fill_pcm;
    audioSpec.userdata = NULL;

    // 打开音频设备
    if (SDL_OpenAudio(&audioSpec, NULL)) {
        printf("can't open SDL Audio Device, err:%s\n", SDL_GetError());
        goto _FAIL;
    }

    // 播放音频
    SDL_PauseAudio(0);

    // 数据读取,记录读取的总数据
    uint64_t dataCount = 0;
    while (1) {
        // 从文件中读取PCM数据
        read_buf_len = fread(audio_buf, 1, PCM_BUFFER_SIZE, pcmFile);
        if (read_buf_len == 0) {
            break;
        }
        // 统计读取的总字节数
        dataCount += read_buf_len;
        // 更新数据读取结束位置
        audio_end = audio_buf + read_buf_len;
        // 更新数据读取开始位置
        audio_pos = audio_buf;

        while (audio_pos < audio_end) {
            // 等待PCM数据消耗
            SDL_Delay(10);
            printf("SDL_Delay\n");
        }
    }
    printf("play PCM finish\n");
    // 关闭音频设备
    SDL_CloseAudio();
    _FAIL:
    // 释放资源
    if (audio_buf) {
        free(audio_buf);
    }

    if (pcmFile) {
        fclose(pcmFile);
    }

    SDL_Quit();

    return 0;
}

/**
 * @brief sdl_fill_pcm
 * @param userdata
 * @param stream
 * @param len
 * 音频回调函数实现
 */
void sdl_fill_pcm(void *userdata, Uint8 *stream, int len) {
    //内存初始化
    SDL_memset(stream, 0, len);
    if (audio_pos >= audio_end) {
        // 数据读取完毕
        printf("buffer is read complete.\n");
        return;
    }
    // 数据不够直接读完，否则只读取一定长度的数据
    int remain_buf_len = audio_end - audio_pos;
    len = (remain_buf_len > len) ? len : remain_buf_len;
    // 拷贝数据到stream并设置音量
    SDL_MixAudio(stream, audio_pos, len, SDL_MIX_MAXVOLUME / 3);
    printf("already read data:%d\n", len);
    audio_pos += len;
}
