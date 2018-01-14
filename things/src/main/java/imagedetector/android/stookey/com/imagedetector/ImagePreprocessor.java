package imagedetector.android.stookey.com.imagedetector;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.nfc.Tag;
import android.util.Log;

import junit.framework.Assert;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Created by Stookey on 1/12/18.
 */

public class ImagePreprocessor {
    private static final String TAG = "ImagePreprocessor";
    private Bitmap rgbFrameBitmap, croppedBitmap;


    //Constructor
    public ImagePreprocessor(){
        Log.d("TAG", "Preprocessor Initialized");
        this.croppedBitmap = Bitmap.createBitmap(Helper.IMAGE_SIZE, Helper.IMAGE_SIZE, Bitmap.Config.ARGB_8888);
        this.rgbFrameBitmap = Bitmap.createBitmap(Camera.IMAGE_WIDTH, Camera.IMAGE_HEIGHT, Bitmap.Config.ARGB_8888);
        Log.d(TAG, "Camera.IMAGE_WIDTH, Camera.IMAGE_HEIGHT: "+ Camera.IMAGE_WIDTH +", "+ Camera.IMAGE_HEIGHT);
    }

    public Bitmap preprocessImage(final Image image){
        if(image == null){
            return null;
        }
        Log.d(TAG, "Preprocessing Image...");
        Log.d(TAG, "Image Properties: "  + image.getWidth() + ", " + image.getHeight());
        Log.d(TAG, "rgbFrameBitmap properties: " + rgbFrameBitmap.getWidth() + ", " + rgbFrameBitmap.getHeight());

        //Not sure what these lines do, will research later
        Assert.assertEquals("Invalid size width", rgbFrameBitmap.getWidth(), image.getWidth());
        Assert.assertEquals("Invalid size height", rgbFrameBitmap.getHeight(), image.getHeight());

        if(croppedBitmap != null && rgbFrameBitmap != null){
            ByteBuffer bb = image.getPlanes()[0].getBuffer();
            rgbFrameBitmap = BitmapFactory.decodeStream(new ByteBufferBackedInputStream(bb));
            Helper.cropAndRescaleBitmap(rgbFrameBitmap, croppedBitmap, 0);
        }
        //image.close();
        Log.d(TAG,"croppedBitmap: " + croppedBitmap.toString());
        return croppedBitmap;
    }


    //Class to convert a Bytebuffer into an InputStream
    private static class ByteBufferBackedInputStream extends InputStream {
        ByteBuffer buffer;

        public ByteBufferBackedInputStream(ByteBuffer buffer){
            this.buffer = buffer;
        }

        public int read() throws IOException {
            if (!buffer.hasRemaining()){
                return -1;
            }
            return buffer.get() & 0xFF;
        }

        public int read(byte[] bytes, int off, int len) throws IOException{
            if(!buffer.hasRemaining()){
                return -1;
            }

            len = Math.min(len, buffer.remaining());
            buffer.get(bytes, off, len);
            return len;
        }

    }
}
