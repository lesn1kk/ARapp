package lesnik.com.arapp_1;

import com.google.zxing.Result;

/**
 * ZXing QR Code scanner result handler interface.
 */
interface IResultHandler {

    /**
     * Handles result from decoded QR Code.
     * @param mResult Contains results.
     */
    void handleResult(Result mResult);
}
