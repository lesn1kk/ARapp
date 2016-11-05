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
public class ARAppActivity extends GvrActivity implements ResultHandler, Camera.PreviewCallback {

    private Camera mCamera;
    private ARAppStereoRenderer renderer;
    private String TAG = "ARAppActivity";
    private Vibrator vibrator;
    private MultiFormatReader mMultiFormatReader;
    private ResultHandler mResultHandler;
    static final List<BarcodeFormat> ALL_FORMATS = new ArrayList();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ARAppGvrView glSurfaceView = new ARAppGvrView(this);

        renderer = glSurfaceView.getRenderer();
        setContentView(glSurfaceView);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        initMultiFormatReader();
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
            //params.setPreviewSize(1280, 720);

            mCamera.setParameters(params);
            mCamera.startPreview();

        } catch (IOException ioe) {
            Log.w("ARAppActivity", "CAM LAUNCH FAILED");
            System.out.println("Camera error");
        }
    }

    @Override
    public void onPause() {
        //TODO Handle this properly
        mCamera.stopPreview();
        mCamera.release();
        System.exit(0);
    }

    @Override
    public void handleResult(Result result) {
        Log.e("handleResult", result.getText());
    }

    @Override
    public void onCardboardTrigger() {
        Log.i(TAG, "onCardboardTrigger");

        // Always give user feedback.
        focusOnClick();

        // Turn on scanner
        // Set this class as a result handler
        mResultHandler = this;
        vibrator.vibrate(50);
        ARAppStereoRenderer.drawLine = true;
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

                Result var22 = null;
                PlanarYUVLuminanceSource var23 = this.buildLuminanceSource(data, width, height);

                if (var23 != null) {
                    BinaryBitmap var24 = new BinaryBitmap(new HybridBinarizer(var23));

                    try {
                        var22 = this.mMultiFormatReader.decodeWithState(var24);
                    } catch (ReaderException var17) {
                        ;
                    } catch (NullPointerException var18) {
                        ;
                    } catch (ArrayIndexOutOfBoundsException var19) {
                        ;
                    } finally {
                        this.mMultiFormatReader.reset();
                    }
                }

                final Result var22copy = var22;

                if (var22 != null) {
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        public void run() {
                            ResultHandler tmpResultHandler = mResultHandler;
                            mResultHandler = null; // turn off scanning after finding one
                            if (tmpResultHandler != null) {
                                tmpResultHandler.handleResult(var22copy);
                            }

                        }
                    });
                }

            } catch (RuntimeException var21) {
                Log.e("ZXingScannerView", var21.toString(), var21);
            }

        }
    }

    public PlanarYUVLuminanceSource buildLuminanceSource(byte[] data, int width, int height) {
        //TODO Draw a square or rect on view and tell user to hold qrcode inside of it, then get its coordinates, wight and height
//        Rect rect = this.getFramingRectInPreview(width, height);
//        if(rect == null) {
//            return null;
//        } else {
        PlanarYUVLuminanceSource source = null;

        try {
            source = new PlanarYUVLuminanceSource(data, width, height, 0, 0, width, height, false);
        } catch (Exception var7) {
            ;
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

