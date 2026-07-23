cmake_minimum_required(VERSION 3.22.1)
project("zoomenhance")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

find_library(log-lib log)
find_library(android-lib android)
find_library(jnigraphics-lib jnigraphics)
find_library(vulkan-lib vulkan)

set(SD_CPP_ROOT ${CMAKE_SOURCE_DIR}/stable-diffusion.cpp)
set(SD_VULKAN ON CACHE BOOL "Enable Vulkan compute backend" FORCE)
set(SD_BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)

add_subdirectory(${SD_CPP_ROOT} ${CMAKE_BINARY_DIR}/sd-build)

add_library(native-lib SHARED native-lib.cpp)
target_include_directories(native-lib PRIVATE ${SD_CPP_ROOT})
target_link_libraries(native-lib stable-diffusion ${log-lib} ${android-lib} ${jnigraphics-lib} ${vulkan-lib})
