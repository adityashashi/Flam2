package com.example.edgedetectionapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.*;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 200;
    
    private GLSurfaceView glSurfaceView;
    private OpenGLRenderer renderer;
    private Button toggleButton;
    private TextView fpsTextView;
    
    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension;
    private ImageReader imageReader;
    private Handler backgroundHandler;
    private HandlerThread backgroundThread;
    
    private boolean isFilterEnabled = false;
    private long lastFrameTime = 0;
    private int frameCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        if (!JNIInterface.initOpenCV()) {
            Log.e(TAG, "Cannot initialize OpenCV");
            Toast.makeText(this, "Cannot initialize OpenCV", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        
        initViews();
        setupOpenGL();
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            openCamera();
        }
    }
    
    private void initViews() {
        glSurfaceView = findViewById(R.id.gl_surface_view);
        toggleButton = findViewById(R.id.btn_toggle_filter);
        fpsTextView = findViewById(R.id.tv_fps);
        
        toggleButton.setOnClickListener(v -> {
            isFilterEnabled = !isFilterEnabled;
            toggleButton.setText(isFilterEnabled ? "Show Original" : "Show Edge Detection");
        });
    }
    
    private void setupOpenGL() {
        glSurfaceView.setEGLContextClientVersion(2);
        renderer = new OpenGLRenderer();
        glSurfaceView.setRenderer(renderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }
    
    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("Camera Background");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }
    
    private void stopBackgroundThread() {
        if (backgroundThread != null) {
            backgroundThread.quitSafelyAfterPendingTasks();
            try {
                backgroundThread.join();
                backgroundThread = null;
                backgroundHandler = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            String cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            
            Size[] sizes = map.getOutputSizes(ImageFormat.YUV_420_888);
            imageDimension = chooseOptimalSize(sizes);
            
            imageReader = ImageReader.newInstance(imageDimension.getWidth(), imageDimension.getHeight(), ImageFormat.YUV_420_888, 1);
            imageReader.setOnImageAvailableListener(imageAvailableListener, backgroundHandler);
            
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            manager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    
    private Size chooseOptimalSize(Size[] choices) {
        for (Size size : choices) {
            if (size.getWidth() <= 1280 && size.getHeight() <= 720) {
                return size;
            }
        }
        return choices[0];
    }
    
    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            createCameraPreview();
        }
        
        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            cameraDevice.close();
        }
        
        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };
    
    private void createCameraPreview() {
        try {
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(imageReader.getSurface());
            
            cameraDevice.createCaptureSession(Arrays.asList(imageReader.getSurface()), 
                new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(@NonNull CameraCaptureSession session) {
                        if (cameraDevice == null) return;
                        
                        captureSession = session;
                        updatePreview();
                    }
                    
                    @Override
                    public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                        Toast.makeText(MainActivity.this, "Configuration change", Toast.LENGTH_SHORT).show();
                    }
                }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    
    private void updatePreview() {
        if (cameraDevice == null) return;
        
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    
    private final ImageReader.OnImageAvailableListener imageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = reader.acquireLatestImage();
            if (image != null) {
                processImage(image);
                image.close();
            }
        }
    };
    
    private void processImage(Image image) {
        // Convert YUV to RGB
        byte[] rgbData = convertYUVToRGB(image);
        
        // Process with OpenCV
        byte[] processedData;
        if (isFilterEnabled) {
            processedData = JNIInterface.processImageWithCanny(rgbData, image.getWidth(), image.getHeight());
        } else {
            processedData = rgbData;
        }
        
        // Update OpenGL texture
        renderer.updateTexture(processedData, image.getWidth(), image.getHeight());
        glSurfaceView.requestRender();
        
        // Update FPS
        updateFPS();
    }
    
    private byte[] convertYUVToRGB(Image image) {
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();
        
        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();
        
        byte[] nv21 = new byte[ySize + uSize + vSize];
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);
        
        return JNIInterface.convertYUVToRGB(nv21, image.getWidth(), image.getHeight());
    }
    
    private void updateFPS() {
        frameCount++;
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFrameTime >= 1000) {
            final float fps = frameCount * 1000.0f / (currentTime - lastFrameTime);
            runOnUiThread(() -> fpsTextView.setText(String.format("FPS: %.1f", fps)));
            frameCount = 0;
            lastFrameTime = currentTime;
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();
        if (glSurfaceView != null) {
            glSurfaceView.onResume();
        }
    }
    
    @Override
    protected void onPause() {
        if (glSurfaceView != null) {
            glSurfaceView.onPause();
        }
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }
    
    private void closeCamera() {
        if (captureSession != null) {
            captureSession.close();
            captureSession = null;
        }
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
    }
}