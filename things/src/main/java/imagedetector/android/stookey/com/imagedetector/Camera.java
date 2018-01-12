package imagedetector.android.stookey.com.imagedetector;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Handler;
import android.util.Log;
import android.util.Size;

import java.util.Collections;

import static android.content.ContentValues.TAG;
import static android.content.Context.CAMERA_SERVICE;

/**
 * Created by Stookey on 1/11/18.
 */

public class Camera {

    private static final String TAG = Camera.class.getSimpleName();

     static final int IMAGE_WIDTH = 1944;
     static final int IMAGE_HEIGHT = 2592;
    private static final int MAX_IMAGES = 3;

    //IMAGE RESULT PROCESSOR
    private ImageReader mImageReader;
    //Active Camera device connection
    private CameraDevice mCameraDevice;
    //active camera capture session
    private CameraCaptureSession mCaptureSession;

    //Lazy-loaded singleton
    private Camera(){
    }

    private static class InstanceHolder {
        private static Camera mCamera = new Camera();
    }

    public static Camera getInstance(){
        return InstanceHolder.mCamera;
    }


    public void initializeCamera(Context context,
                                 Handler backgroundHandler,
                                 ImageReader.OnImageAvailableListener imageAvailableListener){
        String[] camIds = {};
        CameraManager manager = (CameraManager)context.getSystemService(CAMERA_SERVICE);
        try {
            camIds = manager.getCameraIdList();
            Log.d(TAG, "# of Cameras: "+ camIds.length);
            Log.d(TAG, "List of Camera IDs: " + camIds);

        } catch (CameraAccessException e) {
            e.printStackTrace();
            Log.d(TAG, "initializeCamera: error getting cam ids", e);
        }
        String id = camIds[0];
        //Start Image Processor
        mImageReader = ImageReader.newInstance(IMAGE_WIDTH, IMAGE_HEIGHT, ImageFormat.JPEG, MAX_IMAGES);
        mImageReader.setOnImageAvailableListener(imageAvailableListener, backgroundHandler);

        //Open the camera
        try {
            manager.openCamera(id, mStateCallback, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            Log.d(TAG, "Camera Access Exception", e);
        }
    }

    //Reports when the camera was opened successfully after the openCamera() method
    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            Log.d(TAG, "Opened Camera");
            mCameraDevice = camera;
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            Log.d(TAG, "Camera Disconnected");
            camera.close();
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            Log.d(TAG,"Camera device error, closing");
            camera.close();
        }

        @Override
        public void onClosed(CameraDevice camera) {
            Log.d(TAG, "Closed camera, releasing");
            mCameraDevice = null;
        }
    };

    public void takePicture(){
        if(mCameraDevice == null){
            Log.w(TAG, "Cannot capture image. Camera not initialized");
            return;
        }
        try {
            mCameraDevice.createCaptureSession(Collections.singletonList(
                    mImageReader.getSurface()),
                    mSessionCallback,
                    null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            Log.d(TAG, "access exception while preparing pic", e);
        }
    }
    //Reports when Capture session is created and ready
    private CameraCaptureSession.StateCallback mSessionCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession session) {
            if(mCameraDevice == null)
                return;
            mCaptureSession = session;
            triggerImageCapture();
        }
        @Override
        public void onConfigureFailed(CameraCaptureSession session) {
            Log.d(TAG, "Failed to configure");
        }
    };

    private void triggerImageCapture(){
        try {
            final CaptureRequest.Builder captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
            Log.d(TAG, "Session Initialized");
            mCaptureSession.capture(captureBuilder.build(), mCaptureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private final CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted( CameraCaptureSession session,  CaptureRequest request, TotalCaptureResult result) {

            if(session != null){
                session.close();
                mCaptureSession = null;
                Log.d(TAG, "CaptureSession closed");
            }
        }
    };

    public void shutDown(){
        if(mCameraDevice != null){
            mCameraDevice.close();
        }
    }

    public static void dumpFormatInfo(Context context) {
        CameraManager manager = (CameraManager) context.getSystemService(CAMERA_SERVICE);
        String[] camIds = {};
        try {
            camIds = manager.getCameraIdList();
        } catch (CameraAccessException e) {
            Log.d(TAG, "Camera access exception getting IDs");
        }
        if (camIds.length < 1) {
            Log.d(TAG, "No cameras found");
        }
        String id = camIds[0];
        Log.d(TAG, "Using camera id " + id);
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(id);
            StreamConfigurationMap configs = characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            for (int format : configs.getOutputFormats()) {
                Log.d(TAG, "Getting sizes for format: " + format);
                for (Size s : configs.getOutputSizes(format)) {
                    Log.d(TAG, "\t" + s.toString());
                }
            }
            int[] effects = characteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_EFFECTS);
            for (int effect : effects) {
                Log.d(TAG, "Effect available: " + effect);
            }
        } catch (CameraAccessException e) {
            Log.d(TAG, "Cam access exception getting characteristics.");
        }
    }
}
