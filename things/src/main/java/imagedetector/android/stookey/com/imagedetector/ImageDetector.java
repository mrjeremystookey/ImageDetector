package imagedetector.android.stookey.com.imagedetector;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.things.contrib.driver.button.Button;
import com.google.android.things.contrib.driver.voicehat.VoiceHat;
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManagerService;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import imagedetector.android.stookey.com.imagedetector.classifier.Classifier;
import imagedetector.android.stookey.com.imagedetector.classifier.TensorFlowImageClassifier;

/**
 * Created by Stookey on 1/11/18.
 */

public class ImageDetector extends Activity {

    private ImagePreprocessor mImagePreprocessor;
    private TensorFlowImageClassifier mTensorFlowClassifier;

    private static final String TAG = "ImageDetector";
    private Handler mCameraHandler;
    private HandlerThread mCameraThread;

    private StorageReference mStorageReference;

    private Camera mCamera;
    private PeripheralManagerService service;

    private static final int BUTTON_DEBOUNCE_DELAY_MS = 20;
    private static final boolean USE_VOICEHAT_I2S_DAC = Build.DEVICE.equals(BoardDefaults.DEVICE_RP13);

    private com.google.android.things.contrib.driver.button.Button mButton;
    private Gpio mLed;

    private Bitmap bitmapPicture;
    private List<Classifier.Recognition> results;




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
                        if(pressed)
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

        mStorageReference = FirebaseStorage.getInstance().getReference();
        //Error when initializing the TensorFlowImageClassifier
        mTensorFlowClassifier = new TensorFlowImageClassifier(ImageDetector.this);

    }


    private ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            //Get the raw image bytes
            //Image image = reader.acquireNextImage();
            Image image = reader.acquireLatestImage();
            //Reads as a JPEG
            try {
                Log.d(TAG,"Image Characteristics: ");
                Log.d(TAG, "Format: " + image.getFormat());
                Log.d(TAG, "Width: " + image.getWidth());
                Log.d(TAG, "Height: " + image.getHeight());
                ImagePreprocessor imagePreprocessor = new ImagePreprocessor();
                //Processes the image to be entered into the TensorFlow model
                //Gets the results from the TensorFlow model
                bitmapPicture = imagePreprocessor.preprocessImage(image);
                Log.d(TAG, "bitmapPicture check (width, height): " + bitmapPicture.getWidth() + ", " + bitmapPicture.getHeight());
                results = mTensorFlowClassifier.doRecognize(bitmapPicture);
                Log.d(TAG, "Results" + results);
            } catch (NullPointerException e) {
                //e.printStackTrace();
            }
            ByteBuffer imageBuf = image.getPlanes()[0].getBuffer();
            final byte[] imageBytes = new byte[imageBuf.remaining()];
            Log.d(TAG, "Length of imageBytes array: "+imageBytes.length);
            imageBuf.get(imageBytes);
            Log.d(TAG, "Closing Image");
            image.close();
            onPictureTaken(imageBytes, results);
        }
    };

    private void onPictureTaken(final byte[] imageBytes, List<Classifier.Recognition> results){
        if (imageBytes != null) {
            //Do something with the image.
            //Image Uploaded to Firebase Storage
            Log.i(TAG, "Photo ready to be processed");
            StorageReference tensorImages = mStorageReference.child("images/tensor.jpg");
            try {
                tensorImages.putBytes(imageBytes).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Uri downloadUrl = taskSnapshot.getDownloadUrl();
                        Log.i(TAG, "View the image here: " + downloadUrl);
                        Log.i(TAG, "Photo uploaded to Firebase Storage");
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Unable to upload to Firebase");
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCameraThread.quitSafely();
        if (mLed != null) {
            try {
                mLed.close();
            } catch (IOException e) {
                Log.w(TAG, "error closing LED", e);
            }
            mLed = null;
        }
        if (mButton != null) {
            try {
                mButton.close();
            } catch (IOException e) {
                Log.w(TAG, "error closing button", e);
            }
            mButton = null;
        }
//        if (mDac != null) {
//            try {
//                mDac.close();
//            } catch (IOException e) {
//                Log.w(TAG, "error closing voice hat trigger", e);
//            }
//            mDac = null;
    }

}


