package lesnik.com.arapp_1;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import java.util.ArrayList;

public class ARAppSpeech implements RecognitionListener{
    //singleton
    private static final ARAppSpeech mARAppSpeech = new ARAppSpeech();
    private SpeechRecognizer mSpeechRecognizer;
    private String TAG = this.getClass().getName();
    private ARAppActivity mContext;

    public static ARAppSpeech getInstance() {
        return mARAppSpeech;
    }

    public void init(Context _mContext) {
        mContext = (ARAppActivity)_mContext;
        mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(mContext);
        mSpeechRecognizer.setRecognitionListener(this);
        boolean check = SpeechRecognizer.isRecognitionAvailable(mContext);
        Log.e(TAG, "init " + check);
    }

    public void startListening() {
        Intent mIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        mSpeechRecognizer.startListening(mIntent);

        Log.e(TAG, "startListening");
    }

    @Override
    public void onReadyForSpeech(Bundle bundle) {
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
        Log.e(TAG, "onError " + i);
    }

    @Override
    public void onResults(Bundle bundle) {
        Log.e(TAG, "onResults");

        ArrayList<String> mResults = bundle.getStringArrayList("results_recognition");
        for (String m:mResults) {
            System.out.println(m);
            switch (m) {
                case "scan":
                    mContext.turnOnQRCodeScanner();
            }
        }
    }

    @Override
    public void onPartialResults(Bundle bundle) {

    }

    @Override
    public void onEvent(int i, Bundle bundle) {

    }
}
