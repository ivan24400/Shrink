cmake_minimum_required(VERSION 3.4.1)

add_library( dcrz SHARED
             src/main/cpp/dcrz.c
             src/main/cpp/bwt.c
             src/main/cpp/ibwt.c
             src/main/cpp/huffEncode.c
             src/main/cpp/rle.c
             src/main/cpp/huffDecode.c
             src/main/cpp/dcrzDecode.c
             src/main/cpp/mtf.c
             src/main/cpp/dcrzEncode.c
)

target_link_libraries(dcrz
                    android
                    log
                    )