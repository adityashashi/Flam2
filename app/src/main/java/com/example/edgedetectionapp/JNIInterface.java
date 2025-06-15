package com.example.edgedetectionapp;

public class JNIInterface {
    static {
        System.loadLibrary("native-lib");
    }
    
    public static native boolean initOpenCV();
    public static native byte[] convertYUVToRGB(byte[] yuvData, int width, int height);
    public static native byte[] processImageWithCanny(byte[] rgbData, int width, int height);
    public static native byte[] processImageWithGrayscale(byte[] rgbData, int width, int height);
}