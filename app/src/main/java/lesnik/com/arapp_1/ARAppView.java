package lesnik.com.arapp_1;

import android.content.Context;

import com.google.vr.sdk.base.GvrView;

class ARAppView extends GvrView {
    ARAppStereoRenderer mARAppRenderer;

    public ARAppView(Context context) {
        super(context);

        setEGLContextClientVersion(2);

        mARAppRenderer = new ARAppStereoRenderer((ARAppActivity) context);
        setRenderer(mARAppRenderer);
        //this.setTransitionViewEnabled(true);
    }

    public ARAppStereoRenderer getRenderer() {
        return mARAppRenderer;
    }
}

