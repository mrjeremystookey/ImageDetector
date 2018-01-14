package imagedetector.android.stookey.com.imagedetector.classifier;

import android.content.Context;
import android.graphics.Bitmap;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;
import java.util.List;

import imagedetector.android.stookey.com.imagedetector.Helper;

/**
 * Created by Stookey on 1/12/18.
 */

public class TensorFlowImageClassifier implements Classifier {

    private static final String TAG = "TensorFlowImageClassifier";
    private String[] labels;

    private float[] floatValues;
    private int[] intValues;
    private float[] outputs;

    private TensorFlowInferenceInterface inferenceInterface;


    //Constructor
    public TensorFlowImageClassifier(Context context){
        this.inferenceInterface = new TensorFlowInferenceInterface(
                context.getAssets(),
                Helper.MODEL_FILE);
        this.labels = Helper.readLabels(context);

        intValues = new int[Helper.IMAGE_SIZE * Helper.IMAGE_SIZE];
        floatValues = new float[Helper.IMAGE_SIZE * Helper.IMAGE_SIZE * 3];
        outputs = new float[Helper.NUM_CLASSES];
    }


    @Override
    public List<Recognition> doRecognize(Bitmap image) {
        float[] pixels = Helper.getPixels(image, intValues, floatValues);
        inferenceInterface.feed(Helper.INPUT_NAME, pixels, Helper.NETWORK_STRUCTURE);
        inferenceInterface.run(Helper.OUTPUT_NAMES);
        //Fetch results into an array of confidence per category
        inferenceInterface.fetch(Helper.OUTPUT_NAME, outputs);
        //Maps highest confidence levels to labels
        return Helper.getBestResults(outputs, labels);
    }

    @Override
    public void destroyClassifier() {
        inferenceInterface.close();
    }
}
