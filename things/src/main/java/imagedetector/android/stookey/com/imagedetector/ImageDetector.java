package imagedetector.android.stookey.com.imagedetector;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.google.android.things.contrib.driver.button.Button;
import com.google.android.things.contrib.driver.voicehat.VoiceHat;
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by Stookey on 1/11/18.
 */

public class ImageDetector extends Activity {
    private static final String TAG = "ImageDetector";
    private Handler mCameraHandler;
    private HandlerThread mCameraThread;


    private Camera mCamera;
    private PeripheralManagerService service;

    private static final int BUTTON_DEBOUNCE_DELAY_MS = 20;
    private static final boolean USE_VOICEHAT_I2S_DAC = Build.DEVICE.equals(BoardDefaults.DEVICE_RP13);

    private com.google.android.things.contrib.driver.button.Button mButton;
    private Gpio mLed;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        service = new PeripheralManagerService();
        Log.d(TAG, "onCreate: Available GPIO " + service.getGpioList());

        if (checkSelfPermission(Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // A problem occurred auto-granting the permission
            Log.d(TAG, "No permission");
            return;
        } else
            Log.d(TAG, "Camera Permission Granted");

        //Creating background worker thread to handle camera actions
        mCameraThread = new HandlerThread("CameraBackground");
        mCameraThread.start();
        mCameraHandler = new Handler(mCameraThread.getLooper());

        Log.d(TAG, "Type of mCamera" + mCamera);
        mCamera = Camera.getInstance();
        mCamera.initializeCamera(this, mCameraHandler, mOnImageAvailableListener);


        Log.d(TAG, "Configuring GPIO Peripheral Pins from the VoiceHat");
        try {
            mButton = VoiceHat.openButton();
            mLed = VoiceHat.openLed();
            mButton.setDebounceDelay(BUTTON_DEBOUNCE_DELAY_MS);
            mButton.setOnButtonEventListener(new Button.OnButtonEventListener() {
                @Override
                public void onButtonEvent(Button button, boolean pressed) {
                    //Do Something with Button Press here.
                    try {
                        if(mLed.getValue() == true)
                            mLed.setValue(false);
                        else
                            mLed.setValue(true);
                        Log.d(TAG, "Taking Picture");
                        mCamera.takePicture();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            mLed.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            mLed.setActiveType(Gpio.ACTIVE_HIGH);
        } catch (IOException e) {
            e.printStackTrace();
        }



    }






    private ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            //Get the raw image bytes
            Image image = reader.acquireLatestImage();
            ByteBuffer imageBuf= image.getPlanes()[0].getBuffer();
            final byte[] imageBytes = new byte[imageBuf.remaining()];
            imageBuf.get(imageBytes);
            image.close();
            onPictureTaken(imageBytes);
        }
    };

    private void onPictureTaken(final byte[] imageBytes){
        if (imageBytes != null) {
            //Do something with the image.
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCameraThread.quitSafely();

    }

}
