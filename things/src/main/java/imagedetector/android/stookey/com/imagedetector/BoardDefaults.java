package imagedetector.android.stookey.com.imagedetector;

/**
 * Created by Stookey on 1/12/18.
 */

@SuppressWarnings("WeakerAccess")
public class BoardDefaults {

    public static final String DEVICE_RP13 = "rp13";


    public static String getGPIOForLed(){
        return "BCM25";
    }

    public static String getGPIOForButton(){
        return "BCM23";
    }

    public static String getGPIOForDacTrigger(){
        return "BCM16";
    }

}
