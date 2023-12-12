project(nanodetncnn)

cmake_minimum_required(VERSION 3.10)

set(OpenCV_DIR ${CMAKE_SOURCE_DIR}/opencv-mobile-4.5.1-android/sdk/native/jni)
find_package(OpenCV REQUIRED core imgproc)

# nanodet
#set(ncnn_DIR ${CMAKE_SOURCE_DIR}/ncnn-20220420-android/${ANDROID_ABI}/lib/cmake/ncnn)
#find_package(ncnn REQUIRED)

#add_library(nanodetncnn SHARED nanodetncnn.cpp nanodet.cpp)
#target_link_libraries(nanodetncnn ncnn ${OpenCV_LIBS} mediandk)

# segmentation - yolov8
#add_library(ncnnyolov8 SHARED yolov8ncnn.cpp yolov8.cpp)
#target_link_libraries(ncnnyolov8 ncnn ${OpenCV_LIBS} mediandk)

# detection - yolov5
set(ncnn_DIR ${CMAKE_SOURCE_DIR}/ncnn-20201218-android/${ANDROID_ABI}/lib/cmake/ncnn)
find_package(ncnn REQUIRED)

add_library(ncnnyolov5 SHARED yolov5ncnn.cpp yolov5.cpp)
target_link_libraries(ncnnyolov5 ncnn ${OpenCV_LIBS} mediandk jnigraphics)
