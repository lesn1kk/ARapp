package lesnik.com.arapp_1;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import java.util.ArrayList;

final class ARAppSpeech implements RecognitionListener {
    //singleton
    private static final ARAppSpeech mARAppSpeech = new ARAppSpeech();
    private SpeechRecognizer mSpeechRecognizer;
    private final String mTag = this.getClass().getName();
    private ARAppActivity mContext;

    public static ARAppSpeech getInstance() {
        return mARAppSpeech;
    }

    public void init(Context context) {
        mContext = (ARAppActivity) context;
        mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(mContext);
        mSpeechRecognizer.setRecognitionListener(this);

        // TODO When this check is false, throw an alert. It is necessary!
        //boolean check = SpeechRecognizer.isRecognitionAvailable(mContext);
    }

    public void startListening() {
        Log.d(mTag, "startListening");

        Intent mIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        mSpeechRecognizer.startListening(mIntent);
    }

    @Override
    public void onReadyForSpeech(Bundle bundle) {
        Log.d(mTag, "onReadyForSpeech");

        ARAppStereoRenderer.onErrorListening = false;

        ARAppStereoRenderer.setTexture(R.drawable.listening);
    }

    @Override
    public void onBeginningOfSpeech() {
    }

    @Override
    public void onRmsChanged(float v) {
    }

    @Override
    public void onBufferReceived(byte[] bytes) {
    }

    @Override
    public void onEndOfSpeech() {
    }

    @Override
    public void onError(int i) {
        Log.e(mTag, "onError " + i);
        ARAppStereoRenderer.onErrorListeningNumber = i;
        ARAppStereoRenderer.onErrorListening = true;
        ARAppStereoRenderer.clearTexture();
        ARAppTextureLoader.resetAlpha();
    }

    private boolean isCommandAvailable;

    private void setCommandAvailableAndClearTexture() {
        isCommandAvailable = true;
        ARAppStereoRenderer.clearTexture();
    }

    @Override
    public void onResults(Bundle bundle) {
        Log.d(mTag, "onResults");

        isCommandAvailable = false;
        ArrayList<String> mResults = bundle.getStringArrayList("results_recognition");

        if (mResults != null) {
            for (String m:mResults) {
                System.out.println(m);
                switch (m.toLowerCase()) {
                    case "scan":
                        // Clear screen
                        setCommandAvailableAndClearTexture();
                        mContext.turnOnQRCodeScanner();
                        ARAppStereoRenderer.setTexture(R.drawable.scanningmode);
                        break;
                    case "screenshot":
                        // Clear screen
                        setCommandAvailableAndClearTexture();
                        ARAppStereoRenderer.takeScreenshot = true;
                        break;
                    case "stop":
                        // Clear screen
                        setCommandAvailableAndClearTexture();
                        mContext.turnOffQRCodeScanner();
                        break;
                    case "break":
                        // Clear screen
                        setCommandAvailableAndClearTexture();
                        mContext.turnOffQRCodeScanner();
                        break;
                    case "clear":
                        // Clear screen
                        setCommandAvailableAndClearTexture();
                        mContext.turnOffQRCodeScanner();
                        break;
                    case "blue":
                        // Clear screen
                        setCommandAvailableAndClearTexture();
                        ARAppStereoRenderer.setTexture(R.drawable.blue);
                        break;
                    default:
                        Log.d(mTag, "defaultCase");
                        break;
                }
            }
        }

        if (!isCommandAvailable) {
            ARAppStereoRenderer.setTexture(R.drawable.errorcommandnotfound);
        }
    }

    @Override
    public void onPartialResults(Bundle bundle) {

    }

    @Override
    public void onEvent(int i, Bundle bundle) {

    }
}
