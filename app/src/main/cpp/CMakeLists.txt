cmake_minimum_required(VERSION 3.22.1)

project("native-lib")

set(CMAKE_CXX_STANDARD 14)

# Find OpenCV
find_package(OpenCV REQUIRED)

# Include directories
include_directories(${OpenCV_INCLUDE_DIRS})

# Add library
add_library(native-lib SHARED
    native-lib.cpp
    opencv_processor.cpp
)

# Link libraries
target_link_libraries(native-lib
    ${OpenCV_LIBS}
    android
    log
    jnigraphics
)