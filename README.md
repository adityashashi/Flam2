# 📷 Real-Time Edge Detection Viewer

An advanced Android application demonstrating real-time computer vision processing using Android SDK, OpenCV (C++), OpenGL ES, and JNI integration.

---

## 🚀 Features Implemented ✅

### Core Requirements
- **📸 Camera Integration**: Camera2 API with YUV420_888 format
- **🔄 OpenCV Processing**: Native C++ implementation with JNI bridge
- **🎨 OpenGL Rendering**: Hardware-accelerated texture rendering
- **⚡ Real-time Performance**: 15+ FPS on most devices

### Bonus Features
- **🔘 Filter Toggle**: Switch between original and processed views
- **📊 FPS Counter**: Real-time performance monitoring
- **🎛️ Multiple Filters**: Canny, Grayscale, Sobel, Threshold
- **🛡️ Error Handling**: Comprehensive exception management

---

## 📱 Screenshots

*Add your app screenshots here*

---

## 🏗️ Architecture Overview

```text
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   MainActivity  │    │  Camera2 API     │    │  ImageReader    │
│                 │◄──►│                  │◄──►│                 │
│  • UI Control   │    │  • Frame Capture │    │  • YUV Frames   │
│  • Permissions  │    │  • Camera Params │    │  • Callbacks    │
└─────────────────┘    └──────────────────┘    └─────────────────┘
         │                          │                      │
         ▼                          ▼                      ▼
┌─────────────────┐    ┌──────────────────┐    ┌────────────────────┐
│    OpenGL       │    │    JNI Bridge    │    │     OpenCV C++     │
│    Renderer     │◄──►│                  │◄──►│                    │
│  • Texture      │    │  • Type Convert  │    │  • Edge Detection  │
│  • Shaders      │    │  • Memory Mgmt   │    │  • Filtering       │
└─────────────────┘    └──────────────────┘    └────────────────────┘
