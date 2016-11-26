package lesnik.com.arapp_1;

import android.content.Context;

import com.google.vr.sdk.base.GvrView;

/**
 * Prepares view.
 */
final class ARAppView extends GvrView {

    /**
     * Singleton instance of this class.
     */
    private static ARAppView mARAppView;

    /**
     * Constructor, sets renderer for this view.
     */
    private ARAppView() {
        super(ARAppActivity.getARAppContext());

        setEGLContextClientVersion(2);

        ARAppStereoRenderer.createInstance();
        ARAppStereoRenderer mARAppRenderer = ARAppStereoRenderer.getInstance();
        setRenderer(mARAppRenderer);
        //this.setTransitionViewEnabled(true);
    }

    /**
     * Create singleton instance of this class.
     */
    public static void createInstance() {
        mARAppView = new ARAppView();
    }

    /**
     * Returns instance of this class.
     * @return This class.
     */
    public static ARAppView getInstance() {
        return mARAppView;
    }
}

