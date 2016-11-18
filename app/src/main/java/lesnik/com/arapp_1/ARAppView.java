package lesnik.com.arapp_1;

import android.content.Context;

import com.google.vr.sdk.base.GvrView;

/**
 * Contains ARAppRenderer singleton instance.
 */
class ARAppView extends GvrView {
    private static ARAppStereoRenderer mARAppRenderer;
    private static ARAppView mARAppView;

    private ARAppView(Context context) {
        super(context);

        setEGLContextClientVersion(2);

        ARAppStereoRenderer.createInstance(context);
        mARAppRenderer = ARAppStereoRenderer.getInstance();
        setRenderer(mARAppRenderer);
        //this.setTransitionViewEnabled(true);
    }
    public static void createInstance(Context context) {
        mARAppView = new ARAppView(context);
    }

    public static ARAppView getInstance() {
        return mARAppView;
    }

    public static ARAppStereoRenderer getRenderer() {
        return mARAppRenderer;
    }
}

