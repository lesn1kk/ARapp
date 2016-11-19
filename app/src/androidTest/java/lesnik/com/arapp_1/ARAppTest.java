package lesnik.com.arapp_1;

import android.content.Context;
import android.os.Vibrator;
import android.support.test.runner.AndroidJUnit4;
import android.test.AndroidTestCase;

import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.util.List;

import static android.support.test.InstrumentationRegistry.getContext;
import static android.support.test.InstrumentationRegistry.getTargetContext;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(AndroidJUnit4.class)
public class ARAppTest {
    @BeforeClass
    public static void setup() {
        ARAppView.createInstance(getTargetContext());
        ARAppSpeech m = ARAppSpeech.getInstance();
        m.init(getTargetContext());
    }
    @Test
    public void testOnCreate() throws NoSuchFieldException, IllegalAccessException {
        assertThat("ARAppView should be created", ARAppView.getInstance(), is(notNullValue()));
        assertThat("ARAppSpeech should be created and initialized", ARAppSpeech.getInstance(),
                is(notNullValue()));
    }

//    @Test
//    public void testHandleResult() throws Exception {
//        ARAppStereoRenderer.setTexture(R.drawable.green);
//        assertThat("setTexture sets proper id", ARAppStereoRenderer.texture, is(equalTo(R.drawable.green)));
//    }

}
