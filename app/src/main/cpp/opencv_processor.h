#ifndef OPENCV_PROCESSOR_H
#define OPENCV_PROCESSOR_H

#include <opencv2/opencv.cpp>
#include <opencv2/imgproc.hpp>

class OpenCVProcessor {
public:
    static cv::Mat convertYUVToRGB(const unsigned char* yuvData, int width, int height);
    static cv::Mat applyCanny(const cv::Mat& inputImage, double threshold1 = 50.0, double threshold2 = 150.0);
    static cv::Mat applyGrayscale(const cv::Mat& inputImage);
    static std::vector<unsigned char> matToByteArray(const cv::Mat& mat);
    static cv::Mat byteArrayToMat(const unsigned char* data, int width, int height, int channels);
};

#endif // OPENCV_PROCESSOR_H