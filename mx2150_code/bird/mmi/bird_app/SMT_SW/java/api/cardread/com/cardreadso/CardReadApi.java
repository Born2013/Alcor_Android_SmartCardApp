package api.cardread.com.cardreadso;

public class CardReadApi {
    static {
        System.loadLibrary("CardReadJni");
    }

    public native static int CardreaderReadDataFromUart(char[] buf);
    public native static int CardreaderWriteDataToUart(char[] buf, int length);

}
