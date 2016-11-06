package lesnik.com.arapp_1;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.util.Log;

import com.google.vr.sdk.base.GvrActivity;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;

@SuppressWarnings("deprecation, unchecked")
public class ARAppActivity extends GvrActivity implements IResultHandler, Camera.PreviewCallback {

    static ARAppStereoRenderer mRenderer;
    private Vibrator mVibrator;
    private MultiFormatReader mMultiFormatReader;
    static final List<BarcodeFormat> ALL_FORMATS = new ArrayList();
    private IResultHandler mResultHandler;
    private String TAG = this.getClass().getName();
    private ARAppSpeech mARAppSpeech;
    private ARAppView mARAppView;

    public ARAppView getARAppView() {
        return mARAppView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mARAppView = new ARAppView(this);

        mRenderer = mARAppView.getRenderer();
        setContentView(mARAppView);

        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        initMultiFormatReader();

        mARAppSpeech = ARAppSpeech.getInstance();
        mARAppSpeech.init(this);
    }

    @Override
    public void handleResult(Result mResult) {
        Log.e(TAG, mResult.getText());
    }

    public void turnOnQRCodeScanner() {
        ARAppStereoRenderer.drawLine = true;
        mResultHandler = this;
    }

    public void turnOffQRCodeScanner() {
        ARAppStereoRenderer.drawLine = false;
        mResultHandler = null;
    }

    @Override
    public void onPause() {
        //TODO Handle this properly
        super.onPause();

        ARAppCamera.getCamera().stopPreview();
        ARAppCamera.getCamera().release();
        System.exit(0);
    }

    @Override
    public void onCardboardTrigger() {
        Log.i(TAG, "onCardboardTrigger");

        // Always give user feedback.
        mVibrator.vibrate(50);

        // Focus to get better image quality and results from QRCodeScanner
        ARAppCamera.focusCamera();

        // Turn on speech recognition
        mARAppSpeech.startListening();
    }

    // I need to handle every frame here, in main activity class, because I need context class to do it
    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (this.mResultHandler != null) {
            try {
                Camera.Parameters e = camera.getParameters();
                Camera.Size size = e.getPreviewSize();
                int width = size.width;
                int height = size.height;
                byte[] rawResult = new byte[data.length];
                int source = 0;

                while (true) {
                    if (source >= height) {
                        source = width;
                        width = height;
                        height = source;
                        data = rawResult;
                        break;
                    }

                    for (int finalRawResult = 0; finalRawResult < width; ++finalRawResult) {
                        rawResult[finalRawResult * height + height - source - 1] = data[finalRawResult + source * width];
                    }

                    ++source;
                }

                Result mResult = null;
                PlanarYUVLuminanceSource mSource = this.buildLuminanceSource(data, width, height);

                if (mSource != null) {
                    BinaryBitmap mBitMap = new BinaryBitmap(new HybridBinarizer(mSource));

                    try {
                        mResult = this.mMultiFormatReader.decodeWithState(mBitMap);
                    } catch (ReaderException|NullPointerException|ArrayIndexOutOfBoundsException ex) {

                    } finally {
                        this.mMultiFormatReader.reset();
                    }
                }

                final Result mResultCopy = mResult;

                if (mResult != null) {
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        public void run() {
                            IResultHandler tmpResultHandler = mResultHandler;
                            // turn off scanning after finding one
                            turnOffQRCodeScanner();

                            if (tmpResultHandler != null) {
                                tmpResultHandler.handleResult(mResultCopy);
                            }

                        }
                    });
                }
            } catch (RuntimeException ex) {
                Log.e(TAG, ex.toString(), ex);
                ex.printStackTrace();
            }

        }
    }

    public PlanarYUVLuminanceSource buildLuminanceSource(byte[] data, int width, int height) {
        //TODO Draw a square or rect on view and tell user to hold qrcode inside of it, then get its coordinates, width and height
//        Rect rect = this.getFramingRectInPreview(width, height);
//        if(rect == null) {
//            return null;
//        } else {
        PlanarYUVLuminanceSource source = null;

        try {
            source = new PlanarYUVLuminanceSource(data, width, height, 0, 0, width, height, false);
        } catch (Exception ex) {
            ex.printStackTrace();
            Log.e(TAG, ex.toString(), ex);
        }

        return source;
        // }
    }

    private void initMultiFormatReader() {
        EnumMap hints = new EnumMap(DecodeHintType.class);
        hints.put(DecodeHintType.POSSIBLE_FORMATS, this.getFormats());
        this.mMultiFormatReader = new MultiFormatReader();
        this.mMultiFormatReader.setHints(hints);
    }

    public Collection<BarcodeFormat> getFormats() {
        return ALL_FORMATS;
    }

    static {
        ALL_FORMATS.add(BarcodeFormat.UPC_A);
        ALL_FORMATS.add(BarcodeFormat.UPC_E);
        ALL_FORMATS.add(BarcodeFormat.EAN_13);
        ALL_FORMATS.add(BarcodeFormat.EAN_8);
        ALL_FORMATS.add(BarcodeFormat.RSS_14);
        ALL_FORMATS.add(BarcodeFormat.CODE_39);
        ALL_FORMATS.add(BarcodeFormat.CODE_93);
        ALL_FORMATS.add(BarcodeFormat.CODE_128);
        ALL_FORMATS.add(BarcodeFormat.ITF);
        ALL_FORMATS.add(BarcodeFormat.CODABAR);
        ALL_FORMATS.add(BarcodeFormat.QR_CODE);
        ALL_FORMATS.add(BarcodeFormat.DATA_MATRIX);
        ALL_FORMATS.add(BarcodeFormat.PDF_417);
    }
}

