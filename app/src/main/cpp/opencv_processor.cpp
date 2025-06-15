#include "opencv_processor.h"
#include <android/log.h>

#define LOG_TAG "OpenCVProcessor"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

cv::Mat OpenCVProcessor::convertYUVToRGB(const unsigned char* yuvData, int width, int height) {
    try {
        cv::Mat yuvImage(height + height/2, width, CV_8UC1, (void*)yuvData);
        cv::Mat rgbImage;
        cv::cvtColor(yuvImage, rgbImage, cv::COLOR_YUV2RGB_NV21);
        return rgbImage;
    } catch (const cv::Exception& e) {
        LOGE("Error in convertYUVToRGB: %s", e.what());
        return cv::Mat();
    }
}

cv::Mat OpenCVProcessor::applyCanny(const cv::Mat& inputImage, double threshold1, double threshold2) {
    try {
        cv::Mat grayImage, edges, result;
        
        // Convert to grayscale if needed
        if (inputImage.channels() == 3) {
            cv::cvtColor(inputImage, grayImage, cv::COLOR_RGB2GRAY);
        } else {
            grayImage = inputImage.clone();
        }
        
        // Apply Gaussian blur to reduce noise
        cv::GaussianBlur(grayImage, grayImage, cv::Size(5, 5), 1.4);
        
        // Apply Canny edge detection
        cv::Canny(grayImage, edges, threshold1, threshold2);
        
        // Convert back to RGB for display
        cv::cvtColor(edges, result, cv::COLOR_GRAY2RGB);
        
        return result;
    } catch (const cv::Exception& e) {
        LOGE("Error in applyCanny: %s", e.what());
        return inputImage.clone();
    }
}

cv::Mat OpenCVProcessor::applyGrayscale(const cv::Mat& inputImage) {
    try {
        cv::Mat grayImage, result;
        
        if (inputImage.channels() == 3) {
            cv::cvtColor(inputImage, grayImage, cv::COLOR_RGB2GRAY);
            cv::cvtColor(grayImage, result, cv::COLOR_GRAY2RGB);
        } else {
            result = inputImage.clone();
        }
        
        return result;
    } catch (const cv::Exception& e) {
        LOGE("Error in applyGrayscale: %s", e.what());
        return inputImage.clone();
    }
}

std::vector<unsigned char> OpenCVProcessor::matToByteArray(const cv::Mat& mat) {
    try {
        std::vector<unsigned char> array;
        if (mat.isContinuous()) {
            array.assign(mat.data, mat.data + mat.total() * mat.channels());
        } else {
            for (int i = 0; i < mat.rows; ++i) {
                array.insert(array.end(), mat.ptr<unsigned char>(i), mat.ptr<unsigned char>(i) + mat.cols * mat.channels());
            }
        }
        return array;
    } catch (const cv::Exception& e) {
        LOGE("Error in matToByteArray: %s", e.what());
        return std::vector<unsigned char>();
    }
}

cv::Mat OpenCVProcessor::byteArrayToMat(const unsigned char* data, int width, int height, int channels) {
    try {
        cv::Mat mat(height, width, CV_8UC(channels));
        memcpy(mat.data, data, width * height * channels);
        return mat;
    } catch (const cv::Exception& e) {
        LOGE("Error in byteArrayToMat: %s", e.what());
        return cv::Mat();
    }
}