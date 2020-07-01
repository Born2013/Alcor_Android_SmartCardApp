package com.android.systemui.screenshot;

import android.graphics.Bitmap; 
import android.util.Log;
public class BitmapUtils {
    
    private static int sum = 0;

    public static String getHash(Bitmap bitmap){
        // Bitmap temp = Bitmap.createScaledBitmap(bitmap, 64, 64, false);
        Bitmap temp = Bitmap.createBitmap(bitmap, 0, (int)(bitmap.getHeight()/4), 
                bitmap.getWidth(), (int)(bitmap.getHeight() - bitmap.getHeight()/4) );
        temp = Bitmap.createScaledBitmap(temp, 32, 32, false);
        int[] grayValues = reduceColor(temp);
        int average = sum/grayValues.length;
        String reslut = computeBits(grayValues, average);
        return reslut;
    }

    private static String computeBits(int[] grayValues, int average) {
        char[] result = new char[grayValues.length];
        for (int i = 0; i < grayValues.length; i++)
        {
            if (grayValues[i] < average)
                result[i] = '0';
            else
                result[i] = '1';
        }
        return new String(result);
    }

    private static int[] reduceColor(Bitmap bitmap) {
        sum = 0;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Log.i("th", "scaled bitmap's width*heith:" + width + "*" + height);

        int[] grayValues = new int[width * height];
        int[] pix = new int[width * height];
        bitmap.getPixels(pix, 0, width, 0, 0, width, height);
        for (int i = 0; i < width; i++)
            for (int j = 0; j < height; j++) {
                int x = j * width + i;
                int r = (pix[x] >> 16) & 0xff;
                int g = (pix[x] >> 8) & 0xff;
                int b = pix[x] & 0xff;
                int grayValue = (r * 30 + g * 59 + b * 11) / 100;
                sum+=grayValue;
                grayValues[x] = grayValue;
            }
        return grayValues;
    }
}
