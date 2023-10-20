/**
 * @example resampling_audio.c
 * libswresample API use example.
 */

#include <libavutil/samplefmt.h>
#include "sample.h"

int main(int argc, char **argv) {
    if (argc != 2) {
        fprintf(stderr, "Usage: %s output_file\n"
                        "API example program to show how to resampler an audio stream with libswresample.\n"
                        "This program generates a series of audio frames, resamples them to a specified "
                        "output format and rate and saves them to an output file named output_file.\n",
                argv[0]);
        exit(1);
    }

//    audioSampleBase(argv[1]);
    audioSampleResampler(argv[1]);
    return 1;
}
