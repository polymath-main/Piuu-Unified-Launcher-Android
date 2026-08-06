cmake_minimum_required(VERSION 3.22.1)

project("piuu_core" C)

# Set 16 KB max page size linker flag for Android 16 (API Level 36) 64-bit ABI compatibility
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -Wl,-z,max-page-size=16384")

add_library(
    piuu_core
    SHARED
    piuu_core.c
)

find_library(
    log-lib
    log
)

target_link_libraries(
    piuu_core
    ${log-lib}
)
