package lesnik.com.arapp_1;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.PictureDrawable;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

import com.google.vr.sdk.base.GvrActivity;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.larvalabs.svgandroid.SVG;
import com.larvalabs.svgandroid.SVGParser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

/**
 * Main activity class.
 * Contains logic of QR Code scanner core, because this class is set as camera preview callback.
 * Implements IResultHandler, interface for handling results of QR Code Scanner.
 * Implements Camera.PreviewCallback, every new camera's frame is processed here.
 *
 * Override:
 * GvrActivity: onCardboardTrigger
 * IResultHandler: onHandleResult
 * Camera.PreviewCallback: onPreviewFrame
 * TODO Consider creating static method that return this context.
 */
@SuppressWarnings("deprecation, unchecked")
public class ARAppActivity extends GvrActivity implements IResultHandler, Camera.PreviewCallback {

    /**
     * Constant value used to recognize permission request.
     */
    private final static int PERMISSIONS_GRANTED = 1;

    /**
     * List containing all necessary application permissions, used to ask user about them.
     */
    private final static ArrayList<String> permissionsList = new ArrayList<String>() {{
        add(Manifest.permission.CAMERA);
        add(Manifest.permission.RECORD_AUDIO);
        add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        add(Manifest.permission.VIBRATE);
    }};

    /**
     * Application context available for all packages by calling getter method.
     */
    private static ARAppActivity mContext;

    /**
     * Should be initialized with application. Responsible for vibrator hardware.
     */
    private Vibrator mVibrator;

    /**
     * Holds information about available QR Code formats. Should be initialized with application.
     */
    private MultiFormatReader mMultiFormatReader;

    /**
     * List of possible QR Code formats. Used in initMultiFormatReader() method.
     */
    private static final List<BarcodeFormat> ALL_FORMATS = new ArrayList();

    /**
     * Pointer to the context class that will handle results from QR Code Scanner.
     */
    private IResultHandler mResultHandler;

    /**
     * Class TAG. Used in logs.
     */
    private String mTag = this.getClass().getName();

    /**
     * Local instance of speech recognition service. Used to initialize recognition service when
     * application starts.
     */
    private ARAppSpeech mARAppSpeech;

    /**
     * Creating main and only activity context of this application. ARAppView is setup here,
     * created ARAppView instance as contentView, set local renderer instance, setup vibrator,
     * setup QR code scanner, setup speech recognition.
     * @param savedInstanceState param
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = this;
        askForPermissions();
    }

    /**
     * Called when application do have necessary permissions. Sets up OpenGL ES 2.0 context.
     */
    public void init() {
        ARAppView.createInstance();
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        initMultiFormatReader();

        mARAppSpeech = ARAppSpeech.getInstance();
        mARAppSpeech.init();

        setContentView(ARAppView.getInstance());
    }

    /**
     * Method called from onCreate to check application permissions and if there is lack of any
     * necessary, ask user to grant them.
     */
    public void askForPermissions() {
        if (checkSelfPermission(Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.VIBRATE)
                != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(permissionsList.toArray(new String[permissionsList.size()]),
                    PERMISSIONS_GRANTED);
        } else {
            init();
        }
    }

    /**
     * Handler for results of asking for permissions. If any of permission was rejected,
     * show toast message for 3s that application doesn't have required permissions and kill app.
     * @param requestCode Constant integer passed when requestPermissions was called
     * @param permissions Array of permissions strings
     * @param grantResults Array of granted results, length is equal to number of permissions asked,
     *                     values 0 or -1 when permission was granted or rejected by user. I use it
     *                     to check if all permissions were granted.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        boolean hasRequiredPermissions = true;
        switch (requestCode) {
            case PERMISSIONS_GRANTED: {
                for(int x:grantResults) {
                    if (x == -1) {
                        hasRequiredPermissions = false;
                        break;
                    }
                }

                if (hasRequiredPermissions) {
                    init();
                } else {
                    CharSequence text = "Application doesn't have required permissions!";
                    Toast.makeText(this, text, Toast.LENGTH_SHORT).show();

                    new CountDownTimer(3000, 1000) {
                        public void onTick(long millisUntilFinished) {
                        }

                        public void onFinish() {
                            System.exit(0);
                        }
                    }.start();
                }
            }
        }
    }

    /**
     * ARAppActivity is a result handler for QR code scanner, because this is the only activity.
     * Depending on the result, proper texture is set for drawing, scanner is turned off after.
     * @param mResult Contains result from decoded QR Code.
     * TODO FUTURE: Send JSON to server from here, generate PNG file from SVG
     */
    @Override
    public void handleResult(Result mResult) {
        String mResultString = mResult.getText();

        Log.e(mTag, mResultString);

        switch (mResultString) {
            case "zielony":
                ARAppStereoRenderer.getInstance().setTexture(R.drawable.green);
                break;
            case "green":
                ARAppStereoRenderer.getInstance().setTexture(R.drawable.green);
                break;
            case "niebieski":
                ARAppStereoRenderer.getInstance().setTexture(R.drawable.blue);
                break;
            case "blue":
                ARAppStereoRenderer.getInstance().setTexture(R.drawable.blue);
                break;
            case "convert":
                try {
                    convert();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                break;
            default:
                break;
        }

        turnOffQRCodeScanner();
    }

    public void convert() throws FileNotFoundException {
        SVG svg = SVGParser.getSVGFromResource(getResources(), R.raw.android);
        PictureDrawable pictureDrawable = svg.createPictureDrawable();
        Bitmap bitmap = Bitmap.createBitmap(pictureDrawable.getIntrinsicWidth(), pictureDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);
        canvas.drawPicture(pictureDrawable.getPicture());

        FileOutputStream fos = new FileOutputStream(new File(Environment
                .getExternalStorageDirectory().toString(), "SVG"
                + System.currentTimeMillis() + ".png"));

        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
    }

    /**
     * Android lifecycle. Called when activity is behind other activity.
     */
    @Override
    public void onPause() {
        super.onPause();

        if (ARAppView.getInstance() != null) {
            ARAppView.getInstance().onPause();
        }
        if (ARAppCamera.getInstance() != null) {
            ARAppCamera.getCamera().stopPreview();
        }
    }

    /**
     * Android lifecycle. Called when activity is in background, or phone is locked.
     */
    @Override
    public void onStop() {
        super.onStop();

        if (ARAppSpeech.getInstance() != null) {
            ARAppSpeech.getInstance().onStop();
        }
    }

    /**
     * Android lifecycle. Called when activity is killed.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();

        if (ARAppView.getInstance() != null) {
            ARAppSpeech.getInstance().onDestroy();
        }
        if (ARAppCamera.getInstance() != null) {
            ARAppCamera.getCamera().release();
        }
    }

    /**
     * Method from GvrActivity class (google vr toolkit). It is called when user push the button
     * on google cardboard. Used for starting speech recognition service and also to focus
     * camera every time. Also, always give user a feedback by turning on vibrator for 50ms.
     */
    @Override
    public void onCardboardTrigger() {
        Log.d(mTag, "onCardboardTrigger");

        mVibrator.vibrate(50);
        ARAppCamera.focusCamera();
        mARAppSpeech.startListening();
    }

    /**
     * Camera.PreviewCallback interface.
     * Called as preview frames are displayed.
     * It is inside this class, because it needs a context class to handle it properly.
     * This class was previously set with setPreviewCallback method.
     * Contains QR Code Scanner logic.
     * @param data The contents of the preview frame.
     * @param camera The Camera service object.
     */
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
                        rawResult[finalRawResult * height + height - source - 1] =
                        data[finalRawResult + source * width];
                    }

                    ++source;
                }

                Result mResult = null;
                PlanarYUVLuminanceSource mSource = this.buildLuminanceSource(data, width, height);

                if (mSource != null) {
                    BinaryBitmap mBitMap = new BinaryBitmap(new HybridBinarizer(mSource));

                    try {
                        mResult = this.mMultiFormatReader.decodeWithState(mBitMap);
                    } catch (ReaderException | NullPointerException
                            | ArrayIndexOutOfBoundsException ex) {
                        // If nothing was found, do nothing and continue to scan
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

                            // Turn off scanning after finding one
                            turnOffQRCodeScanner();

                            if (tmpResultHandler != null) {
                                tmpResultHandler.handleResult(mResultCopy);
                            }

                        }
                    });
                }
            } catch (RuntimeException ex) {
                Log.e(mTag, ex.toString(), ex);
                ex.printStackTrace();
            }
        }
    }

    /**
     * Returns this application context.
     * @return Main application context.
     */
    public static ARAppActivity getARAppContext() {
        return mContext;
    }

    /**
     * Method used to turn on QR Code scanner. It set a static variable drawScanningLine, which is
     * checked in ARAppStereoRenderer.onDrawEye. Also, set this class as a result handler (this is
     * value think checked in onPreviewFrame method, which is called when every one camera frame is
     * captured)
     * TODO Check if it would be possible to move handling qr scanner results from here.
     */
    public void turnOnQRCodeScanner() {
        ARAppStereoRenderer.getInstance().setIsScanning(true);
        mResultHandler = this;
    }

    /**
     * Method used to turn off QR Code scanner. Sets static variable drawScanningLine, which is
     * checked in ARAppStereoRenderer.onDrawEye. Set mResultHandler to null value (this is first
     * value checked in onPreviewFrame method, which is called when every one camera frame is
     * captured)
     */
    public void turnOffQRCodeScanner() {
        ARAppStereoRenderer.getInstance().setIsScanning(false);
        mResultHandler = null;
    }

    /**
     * This method is called to cut a rectangle from camera's input to increase performance.
     * Right now it does absolutely nothing useful and performance is still ok.
     * @param data Contents of image
     * @param width Width of rectangle
     * @param height Height of rectangle
     * @return PlanarYUVLuminanceSource, whatever it is
     * TODO Draw rectangle
     */
    private PlanarYUVLuminanceSource buildLuminanceSource(byte[] data, int width, int height) {
        //TODO draw a square or rect on view and tell user to hold qrcode inside of it,
        // then get its coordinates, width and height
//        Rect rect = this.getFramingRectInPreview(width, height);
//        if(rect == null) {
//            return null;
//        } else {
        PlanarYUVLuminanceSource source = null;

        try {
            source = new PlanarYUVLuminanceSource(data, width, height, 0, 0, width, height, false);
        } catch (Exception ex) {
            ex.printStackTrace();
            Log.e(mTag, ex.toString(), ex);
        }

        return source;
        // }
    }

    /**
     * Initialize QR Code possible formats. Called in onCreate.
     */
    private void initMultiFormatReader() {
        EnumMap hints = new EnumMap(DecodeHintType.class);
        hints.put(DecodeHintType.POSSIBLE_FORMATS, ALL_FORMATS);
        mMultiFormatReader = new MultiFormatReader();
        mMultiFormatReader.setHints(hints);
    }

    /**
     * Fill array with possible QR Code formats. Used in initMultiFormatReader.
     */
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

