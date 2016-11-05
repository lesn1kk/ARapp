package lesnik.com.arapp_1;

import android.content.Context;

import com.google.vr.sdk.base.GvrView;

class ARAppGvrView extends GvrView {
    ARAppStereoRenderer mARAppRenderer;

    public ARAppGvrView(Context context) {
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

