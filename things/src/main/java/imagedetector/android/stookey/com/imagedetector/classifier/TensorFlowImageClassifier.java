package imagedetector.android.stookey.com.imagedetector.classifier;

import android.graphics.Bitmap;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;
import java.util.List;

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

    @Override
    public List<Recognition> doRecognize(Bitmap bitmap) {
        return null;
    }

    @Override
    public void destroyClassifier() {

    }
}
