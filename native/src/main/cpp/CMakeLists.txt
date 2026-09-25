cmake_minimum_required(VERSION 3.22.1)
project("djpro_native")

include(FetchContent)
FetchContent_Declare(
    oboe
    GIT_REPOSITORY https://github.com/google/oboe.git
    GIT_TAG 1.9.0
)
FetchContent_MakeAvailable(oboe)

add_library(djpro_native SHARED
    oboe/OboeEngine.cpp
    demucs/DemucsBridge.cpp
)

target_link_libraries(djpro_native
    oboe::oboe
    android
    log
)
