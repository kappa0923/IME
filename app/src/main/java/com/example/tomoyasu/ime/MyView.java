package com.example.tomoyasu.ime;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by tomoyasu on 2015/10/30.
 * モールス信号表のためのviewを作成
 */
public class MyView extends View {
    private Bitmap bitmap = null;
    private static final Map<String, Integer> map; //五十音表を格納

    static {
        map = new HashMap<String, Integer>();
        map.put("11011", 0);map.put("01", 1);map.put("001", 2);map.put("10111", 3);map.put("01000", 4);
        map.put("0100", 5);map.put("10100", 6);map.put("0001", 7);map.put("1011", 8);map.put("1111", 9);
        map.put("10101", 10);map.put("11010", 11);map.put("11101", 12);map.put("01110", 13);map.put("1110", 14);
        map.put("10", 15);map.put("0010", 16);map.put("0110", 17);map.put("01011", 18);map.put("00100", 19);
        map.put("010", 20);map.put("1010", 21);map.put("0000", 22);map.put("1101", 23);map.put("0011", 24);
        map.put("1000", 25);map.put("11001", 26);map.put("1100", 27);map.put("0", 28);map.put("100", 29);
        map.put("1001", 30);map.put("00101", 31);map.put("1", 32);map.put("10001", 33);map.put("10010", 34);
        map.put("011", 35);map.put("10011", 37);map.put("11", 39);
        map.put("000", 40);map.put("110", 41);map.put("10110", 42);map.put("111", 43);map.put("0101", 44);
        map.put("101", 45);map.put("01001", 46);map.put("01100", 48);map.put("0111", 49);
        map.put("01010", 50);map.put("01101", 51);map.put("00", 52);map.put("00110",53);map.put("010101", 54);
    }

    public MyView(Context context) {
        super(context);
        setBackgroundColor(Color.LTGRAY);
        bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.morse);
        //bitmap = Bitmap.createScaledBitmap(bitmap, 728, 161, true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.translate(0, 0);
        double hoge = (double)ImeService.size.x / bitmap.getWidth();
        //android.util.Log.v("tag", Double.toString(hoge));
        float scaleSize = (float)hoge/*ImeService.size.x/728*/;
        canvas.scale(scaleSize, scaleSize * 2);
        Paint paint = new Paint();
        canvas.drawBitmap(bitmap, 0, 0, null);
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(12);

        if (map.containsKey(ImeService.morse)) {
            int fuga1 = bitmap.getWidth() / 11;
            int fuga2 = (int)(fuga1 * 0.49);
            int posX = bitmap.getWidth() - (map.get(ImeService.morse)/5 + 1 ) * fuga1;
            int posY = (map.get(ImeService.morse) % 5) * fuga2;
            //android.util.Log.v("tag:", Integer.toString(fuga1));
            canvas.drawRect(posX, posY, posX+fuga1, posY+fuga2, paint);
        }

    }
}
