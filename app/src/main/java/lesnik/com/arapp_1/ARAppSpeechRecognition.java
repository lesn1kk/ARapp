package lesnik.com.arapp_1;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import java.util.ArrayList;

/**
 * This class sets up and prepares android speech recognition service.
 * Also, process the output and sets appropriate texture.
 * If error occurs, also sets a proper texture.
 */
class ARAppSpeechRecognition implements RecognitionListener {

    /**
     * Make sure we have only one instance of this class (singleton).
     */
    private static final ARAppSpeechRecognition mARAppSpeechRecognition =
            new ARAppSpeechRecognition();

    /**
     * Speech recognized variable.
     */
    private SpeechRecognizer mSpeechRecognizer;

    /**
     * Set to true when recognized command is available.
     */
    private boolean isCommandAvailable;

    /**
     * Class tag used in debug logging.
     */
    private final String mTag = this.getClass().getName();

    /**
     * Returns instance of this class.
     * @return this instance.
     */
    static ARAppSpeechRecognition getInstance() {
        return mARAppSpeechRecognition;
    }

    /**
     * Initialize speech recognition service and sets this class as listener.
     * TODO When this check is false, throw an alert. It is necessary!
     * boolean check = SpeechRecognizer.isRecognitionAvailable(mContext);
     */
    void init() {
        mSpeechRecognizer =
                SpeechRecognizer.createSpeechRecognizer(ARAppActivity.getARAppContext());
        mSpeechRecognizer.setRecognitionListener(this);
    }

    /**
     * Android activity life cycle, called from onStop.
     * Cancels speech recognition service.
     */
    void onStop() {
        mSpeechRecognizer.cancel();
    }

    /**
     * Android activity life cycle, called from onDestroy.
     * Destroys speech recognition service.
     */
    void onDestroy() {
        mSpeechRecognizer.destroy();
    }
    /**
     * Starts listening for voice commands.
     */
    void startListening() {
        Log.d(mTag, "startListening");

        Intent mIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        mSpeechRecognizer.startListening(mIntent);
    }

    /**
     * From RecognitionListener, called when service is listening for commands.
     * @param bundle ?
     */
    @Override
    public void onReadyForSpeech(Bundle bundle) {
        Log.d(mTag, "onReadyForSpeech");

        ARAppStereoRenderer.getInstance().setListeningError(false);
        ARAppStereoRenderer.getInstance().setTexture(R.drawable.listening);
    }

    /**
     * Called when first sound changes are registered.
     */
    @Override
    public void onBeginningOfSpeech() {
    }

    /**
     * Called every time device registers sound change.
     * @param v The new RMS dB value
     */
    @Override
    public void onRmsChanged(float v) {
    }

    /**
     * More sound has been received. The purpose of this function is to allow giving feedback to
     * the user regarding the captured audio. There is no guarantee that this method will be called.
     * @param bytes a buffer containing a sequence of big-endian 16-bit integers representing a
     *              single channel audio stream. The sample rate is implementation dependent.
     */
    @Override
    public void onBufferReceived(byte[] bytes) {
    }

    /**
     * Called when user stops speaking.
     */
    @Override
    public void onEndOfSpeech() {
    }

    /**
     * Called when error occurs. Error 4 means no internet connection, other are related to
     * recognition error.
     * @param i Error code.
     */
    @Override
    public void onError(int i) {
        Log.e(mTag, "onError " + i);
        ARAppStereoRenderer.getInstance().setListeningError(i, true);
        ARAppTextureManager.resetAlpha();
    }

    /**
     * Called when command is recognized.
     */
    private void setCommandAvailableAndClearTexture() {
        isCommandAvailable = true;
        ARAppStereoRenderer.getInstance().clearTexture();
    }

    /**
     * Called when recognition service returns recognized commands.
     * @param bundle Contains recognized words.
     */
    @Override
    public void onResults(Bundle bundle) {
        Log.d(mTag, "onResults");

        isCommandAvailable = false;
        ArrayList<String> mResults = bundle.getStringArrayList("results_recognition");

        if (mResults != null) {
            for (String m:mResults) {
                Log.d(mTag, m);
                switch (m.toLowerCase()) {
                    case "scan":
                        // Clear screen
                        setCommandAvailableAndClearTexture();
                        ARAppActivity.getARAppContext().turnOnQRCodeScanner();
                        ARAppStereoRenderer.getInstance().setTexture(R.drawable.scanningmode);
                        break;
                    case "screenshot":
                        // Clear screen
                        setCommandAvailableAndClearTexture();
                        ARAppStereoRenderer.getInstance().setIsTakingScreenshot(true);
                        break;
                    case "stop":
                        // Clear screen
                        setCommandAvailableAndClearTexture();
                        ARAppActivity.getARAppContext().turnOffQRCodeScanner();
                        break;
                    case "break":
                        // Clear screen
                        setCommandAvailableAndClearTexture();
                        ARAppActivity.getARAppContext().turnOffQRCodeScanner();
                        break;
                    case "clear":
                        // Clear screen
                        setCommandAvailableAndClearTexture();
                        ARAppActivity.getARAppContext().turnOffQRCodeScanner();
                        break;
                    case "cancel":
                        // Clear screen
                        setCommandAvailableAndClearTexture();
                        ARAppActivity.getARAppContext().turnOffQRCodeScanner();
                        break;
                    case "blue":
                        // Clear screen
                        setCommandAvailableAndClearTexture();
                        ARAppStereoRenderer.getInstance().setTexture(R.drawable.blue);
                        break;
                    default:
                        Log.d(mTag, "defaultCase");
                        break;
                }
            }
        }

        if (!isCommandAvailable) {
            ARAppStereoRenderer.getInstance().setTexture(R.drawable.errorcommandnotfound);
        }
    }

    /**
     * Called when partial recognition results are available. The callback might be called at any
     * time between onBeginningOfSpeech() and onResults(Bundle) when partial results are ready.
     * @param bundle Contains returned results
     */
    @Override
    public void onPartialResults(Bundle bundle) {
    }

    /**
     * Reserved for adding future events.
     * @param i The type of the occurred event
     * @param bundle A Bundle containing the passed parameters
     */
    @Override
    public void onEvent(int i, Bundle bundle) {
    }
}
