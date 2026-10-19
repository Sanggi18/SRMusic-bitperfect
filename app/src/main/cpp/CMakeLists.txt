cmake_minimum_required(VERSION 3.22.1)
project("sroboe")

find_package(oboe REQUIRED CONFIG)

add_library(audio SHARED
    ${CMAKE_CURRENT_SOURCE_DIR}/sroboe.cpp
)

target_link_libraries(audio
    PRIVATE
    oboe::oboe
    log
    OpenSLES
)

add_library(decent_usb_audio SHARED
    ${CMAKE_CURRENT_SOURCE_DIR}/decent_usb_audio/usb-audio-output.cpp
)

target_link_libraries(decent_usb_audio
    PRIVATE
    log
)
