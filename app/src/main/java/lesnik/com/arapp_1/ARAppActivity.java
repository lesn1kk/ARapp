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

    private Camera mCamera;
    private ARAppStereoRenderer renderer;
    private Vibrator vibrator;
    private MultiFormatReader mMultiFormatReader;
    static final List<BarcodeFormat> ALL_FORMATS = new ArrayList();
    private IResultHandler mResultHandler;
    private String TAG = this.getClass().getName();
    private ARAppSpeech mARAppSpeech;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ARAppView glSurfaceView = new ARAppView(this);

        renderer = glSurfaceView.getRenderer();
        setContentView(glSurfaceView);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
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

    public Context getContext() {
        return this;
    }

    public void focusOnClick() {
        try {
            mCamera.cancelAutoFocus();
            mCamera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {

                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error during focusOnClick!");
            e.printStackTrace();
        }
    }

    public void startCamera(int texture) {
        SurfaceTexture surface = new SurfaceTexture(texture);
        renderer.setSurface(surface);

        mCamera = Camera.open();

        try {
            mCamera.setPreviewTexture(surface);
            mCamera.setPreviewCallback(this);
            //set camera to continually auto-focus
            Camera.Parameters params = mCamera.getParameters();
            //*EDIT*//params.setFocusMode("continuous-picture");
            //It is better to use defined constraints as opposed to String, thanks to AbdelHady
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);

//            List<int[]> list = params.getSupportedPreviewFpsRange();
//            params.setPreviewFrameRate(30);
//            params.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_CLOUDY_DAYLIGHT);
//            params.setPreviewFpsRange(1000,30000);
//            params.getPictureSize();

            //params.setPictureSize(1920,1080);
            params.setPreviewSize(1280, 720);

            mCamera.setParameters(params);
            mCamera.startPreview();

        } catch (IOException ex) {
            Log.e(TAG, "CAM LAUNCH FAILED");
        }
    }

    @Override
    public void onPause() {
        //TODO Handle this properly
        super.onPause();

        mCamera.stopPreview();
        mCamera.release();
        System.exit(0);
    }

    @Override
    public void onCardboardTrigger() {
        Log.i(TAG, "onCardboardTrigger");

        // Always give user feedback.
        vibrator.vibrate(50);
        // Focus to get better image quality and results from QRCodeScanner
        focusOnClick();

        // Turn on speech recognition
        mARAppSpeech.startListening();
    }

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
                        System.out.println("NOT FOUND EX");
                        Log.e(TAG, ex.toString());
                        ex.printStackTrace();
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

