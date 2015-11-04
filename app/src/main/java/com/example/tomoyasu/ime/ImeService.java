package com.example.tomoyasu.ime;

/**
 * Created by Tomoyasu on 2015/09/24.
 */

        import java.io.BufferedReader;
        import java.io.InputStream;
        import java.io.InputStreamReader;
        import java.io.OutputStream;
        import java.io.OutputStreamWriter;
        import java.io.PrintWriter;
        import java.util.ArrayList;
        import java.util.HashMap;
        import java.util.Map;

        import android.content.Context;
        import android.graphics.Color;
        import android.graphics.Point;
        import android.hardware.Sensor;
        import android.hardware.SensorEvent;
        import android.hardware.SensorEventListener;
        import android.hardware.SensorManager;
        import android.inputmethodservice.InputMethodService;
        import android.os.Handler;
        import android.view.Display;
        import android.view.Gravity;
        import android.view.KeyEvent;
        import android.view.View;
        import android.view.ViewGroup;
        import android.view.WindowManager;
        import android.view.inputmethod.EditorInfo;
        import android.widget.ImageView;
        import android.widget.LinearLayout;
        import android.widget.TextView;

public class ImeService extends InputMethodService implements SensorEventListener {
    Handler countHandler;   //イベントハンドラ
    private static final Map<String, String> map, voiced, semivoiced; //五十音表を格納
    private float proxi = 0; //近接センサ値
    static public String morse = ""; //モールス信号の2進数表記
    private boolean onsw = false; //最初の近接センサの誤動作を防止,falseでoff
    private long startTime = 0, endTime = 0; //手をかざし始めた時間,終えた時間
    OutputStream os;    //output stream
    InputStream is;     //input stream
    PrintWriter writer; //writer file
    BufferedReader reader; //reader file
    private long aveBorder = 383; //読み込み時のボーダー
    private long aveTon = 0, aveZi = 0; //トンツーそれぞれの平均
    private long countTon = 0, countZi = 0; //トンツーそれぞれの個数
    private ArrayList<Long> arrayTon = new ArrayList<Long>();
    private ArrayList<Long> arrayZi = new ArrayList<Long>();
    private long hoge = 0; //aveBorder算出のための仮変数
    private ArrayList<String> arrayMst = new ArrayList<String>(); //文字列の最後尾を管理
    private String tv = ""; //入力中のモールス信号表示のための変数
    private TextView text; //動的Textview用
    static public Point size = new Point(); //画面サイズ共有用
    MyView image;

    static {
        map = new HashMap<String, String>();
        map.put("11011","あ");    map.put("01","い");     map.put("001","う");   map.put("10111","え");    map.put("01000","お");
        map.put("0100","か");   map.put("10100","き");   map.put("0001","く");    map.put("1011","け");     map.put("1111","こ");
        map.put("10101","さ");   map.put("11010","し");     map.put("11101","す");   map.put("01110","せ");   map.put("1110","そ");
        map.put("10","た");   map.put("0010","ち");  map.put("0110","つ");   map.put("01011","て");   map.put("00100","と");
        map.put("010","な");    map.put("1010","に");  map.put("0000","ぬ");   map.put("1101","ね");   map.put("0011","の");
        map.put("1000","は");    map.put("11001","ひ");    map.put("1100","ふ");   map.put("0","へ");   map.put("100","ほ");
        map.put("1001","ま");    map.put("00101","み");    map.put("1","む");   map.put("10001","め");   map.put("10010","も");
        map.put("011","や");   map.put("10011","ゆ");   map.put("11","よ");
        map.put("000","ら");   map.put("110","り");   map.put("10110","る");    map.put("111","れ");   map.put("0101","ろ");
        map.put("101","わ");  map.put("01001","ゐ");     map.put("01100","ゑ");   map.put("0111","を");   map.put("01010","ん");
        map.put("01101", "ー"); map.put ("010101", "、");

        voiced = new HashMap<String, String>();
        voiced.put("か", "が"); voiced.put("き", "ぎ"); voiced.put("く", "ぐ"); voiced.put("け", "げ"); voiced.put("こ", "ご");
        voiced.put("さ", "ざ"); voiced.put("し", "じ"); voiced.put("す", "ず"); voiced.put("せ", "ぜ"); voiced.put("そ", "ぞ");
        voiced.put("た", "だ"); voiced.put("ち", "ぢ"); voiced.put("つ", "づ"); voiced.put("て", "で"); voiced.put("と", "ど");
        voiced.put("は", "ば"); voiced.put("ひ", "び"); voiced.put("ふ", "ぶ"); voiced.put("へ", "べ"); voiced.put("ほ", "ぼ");

        semivoiced = new HashMap<String, String>();
        semivoiced.put("は", "ぱ"); semivoiced.put("ひ", "ぴ"); semivoiced.put("ふ", "ぷ"); semivoiced.put("へ", "ぺ"); semivoiced.put("ほ", "ぽ");
        semivoiced.put("あ", "ぁ"); semivoiced.put("い", "ぃ"); semivoiced.put("う", "ぅ"); semivoiced.put("え", "ぇ"); semivoiced.put("お", "ぉ");
        semivoiced.put("や", "ゃ"); semivoiced.put("ゆ", "ゅ"); semivoiced.put("よ", "ょ");
        semivoiced.put("つ", "っ");
    }

    //裏で走らせてるハンドラ
    private final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            //getCurrentInputConnection().commitText("test", 1);
//            if (proxi == 0 && timer[0] < 100) timer[0]++;
//            else if (timer[1] < 100) timer[1]++;

            // 近接センサが一定時間変更されなかったら、入力を確定
            if ( (System.currentTimeMillis() - endTime) >= aveBorder*2 && (System.currentTimeMillis() - endTime <= aveBorder*2 + 10) && onsw && proxi != 0) {
//                android.util.Log.v("tag2", Long.toString(System.currentTimeMillis() - endTime) );
//            if (timer[1] == 40 && onsw) {
                //文字の削除
                if (morse.equals("00010")) {
                    sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL);
                }

                if (arrayMst.size() > 0) {
                    //濁音へ置換
                    if (morse.equals("00") && voiced.containsKey(arrayMst.get(arrayMst.size() -1 ))) {
                        arrayMst.add(voiced.get(arrayMst.get(arrayMst.size() - 1)));
                        arrayMst.remove(arrayMst.size() - 2);
                        sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL);
                        getCurrentInputConnection().commitText(arrayMst.get(arrayMst.size() -1 ), 1);
                    }

                    //半濁音へ置換
                    if (morse.equals("00110") && semivoiced.containsKey(arrayMst.get(arrayMst.size() - 1 ))) {
                        arrayMst.add(semivoiced.get(arrayMst.get(arrayMst.size() - 1)));
                        arrayMst.remove(arrayMst.size() - 2);
                        sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL);
                        getCurrentInputConnection().commitText(arrayMst.get(arrayMst.size() - 1 ), 1);
                    }
                }

                //文字の割り当て
                if (map.containsKey(morse)) {
                    arrayMst.add(map.get(morse));
                    getCurrentInputConnection().commitText(arrayMst.get(arrayMst.size() - 1), 1);
                }

                morse = "";
                tv = "";
                text.setText(tv);
            }

            //文字の削除
            //文字の入力中に長くなった場合を考慮
            //削除するときに、listが空じゃないかも確認
            if (System.currentTimeMillis() - startTime > aveBorder*3 && onsw && proxi == 0 && morse.equals("") && arrayMst.size() > 0) {
                onsw = false;
                startTime = System.currentTimeMillis();
                sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL);
                arrayMst.remove( arrayMst.size() - 1 );

                tv = "削除";
                text.setText(tv);
            }

            countHandler.postDelayed(runnable, 10); // 呼び出し間隔(ミリ秒)
        }
    };

    @Override
    public void onCreate() {
        // life cycle 1
        super.onCreate();
    }

    @Override
    public View onCreateInputView() {
        // life cycle 2
        // 入力ビューを作成する

        // 非同期処理の開始
        countHandler = new Handler();
        countHandler.post(runnable);

        // レイアウトの生成
        final int MP = ViewGroup.LayoutParams.MATCH_PARENT;
        final int WC = ViewGroup.LayoutParams.WRAP_CONTENT;
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(WC, MP);

        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        text = new TextView(this);
        text.setText(tv);
        text.setTextSize(28);
        text.setGravity(Gravity.CENTER);
        text.setBackgroundColor(Color.WHITE);
        linearLayout.addView(text);

        /*
        ImageView image = new ImageView(this);
        image.setImageResource(R.drawable.morse);
        image.setScaleType(ImageView.ScaleType.FIT_XY);
        linearLayout.addView(image);
        */

        // 画面サイズの取得
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();

        // 画面サイズを元にViewのサイズを設定する
        display.getSize(size);
        LinearLayout.LayoutParams params1 = new LinearLayout.LayoutParams( size.x, (int)(0.44*size.x) );
        image = new MyView(getApplication());
        image.setLayoutParams(params1);
        linearLayout.addView(image);

        // ボタンの追加
//		Button button1 = new Button(this);
//		button1.setText("まぐろ");
//		button1.setLayoutParams(params);
//		button1.setOnClickListener(new View.OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				getCurrentInputConnection().commitText("まぐろ", 1);
////				sendDownUpKeyEvents(97);
////				sendDownUpKeyEvents(KeyEvent.KEYCODE_E);
////				sendDownUpKeyEvents(38);
////				sendDownUpKeyEvents(KeyEvent.KEYCODE_T);
//			}
//		});
//		linearLayout.addView(button1);
//
//		Button button2 = new Button(this);
//		button2.setText("はまち");
//		button2.setLayoutParams(params);
//		button2.setOnClickListener(new View.OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				getCurrentInputConnection().commitText("はまち", 1);
//			}
//		});
//		linearLayout.addView(button2);

        SensorManager sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        if (sensor != null) {
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME);
        }
        Sensor sensor1 = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        if (sensor1 != null) {
            sensorManager.registerListener(this, sensor1, SensorManager.SENSOR_DELAY_GAME);
        }

        return linearLayout;
    }

    @Override
    public View onCreateCandidatesView() {
        // life cycle 3
        // 候補ビューを作成する(null可)
        return null;
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        // life cycle 4
        super.onStartInputView(info, restarting);

        // aveからaveBorderの読み込み
        try {
            is = openFileInput("ave.txt");
            reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            aveBorder = Long.parseLong(reader.readLine());
            android.util.Log.v("aveBorder", Long.toString(aveBorder));
            reader.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // borderの算出
        try {
            is = openFileInput("track.txt");
            reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            String fuga = reader.readLine();
            if (fuga != null) hoge = Long.parseLong(fuga);
            else hoge = 0;
            while(hoge != 0) {
                if (hoge < aveBorder) {
                    aveTon += hoge;
                    countTon++;
                } else {
                    aveZi += hoge;
                    countZi++;
                }

                fuga = reader.readLine();
                if (fuga != null) hoge = Long.parseLong(fuga);
                else hoge = 0;
            }

            android.util.Log.v("aveTon", Long.toString( aveTon / countTon ));
            android.util.Log.v("aveZi", Long.toString( aveZi / countZi ));

            reader.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // ave.txtへのボーダーの書き込み
        try {
            os = openFileOutput("ave.txt", MODE_PRIVATE);
            writer = new PrintWriter(new OutputStreamWriter(os, "UTF-8"));
            writer.write( Long.toString( ((aveTon / countTon) + (aveZi / countZi)) / 2 ) + "\n");
            android.util.Log.v("aveBorder", Long.toString( ((aveTon / countTon) + (aveZi / countZi)) / 2));
            writer.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // ローカルファイルの読み込み
        try {
            os = openFileOutput("track.txt", MODE_APPEND);
            writer = new PrintWriter(new OutputStreamWriter(os, "UTF-8"));
        }
        catch(Exception e) {
            e.printStackTrace();
        }



        // 出力テスト:data/data/com.example.tomoyasu.ime
//        try {
//            OutputStream os = openFileOutput("track.txt", MODE_PRIVATE|MODE_APPEND);
//            PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, "UTF-8"));
//            writer.append("See you again !!\n");
//            writer.close();
//        }
//        catch(Exception e) {
//            e.printStackTrace();
//        }
    }

    @Override
    public void onFinishInput() {
        // life cycle 5
        super.onFinishInput();
        writer.close();
    }

    @Override
    public void onDestroy() {
        // life cycle 6
        super.onDestroy();

        // Handlerの終了
        countHandler.removeCallbacks(runnable);

        // SensorManagerの開放
        SensorManager sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        sensorManager.unregisterListener(this);
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        // 近接センサ
        if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            proxi = event.values[0];
            if (proxi == 0) {
                //近接時
                if (!onsw) {
                    onsw = true;
                    tv = "";
                }
                startTime = System.currentTimeMillis();
            } else if (onsw) {
                //非近接時
                endTime = System.currentTimeMillis();

                //モールス信号の判別
                if ( (endTime - startTime) <= aveBorder) {
                    morse = morse + "0";
                    tv = tv + "・";
                } else {
                    morse = morse + "1";
                    tv = tv + "―";
                }

                text.setText(tv);
                image.invalidate();

                writer.append(Long.toString(endTime - startTime) + "\n");
            }

            //これはなんかの参考になるかもしれんし残しておこう...
            //if (event.values[0] == 0) getCurrentInputConnection().commitText("たらこ", 1);
        }

        // 直線加速度センサ
        if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            if ( (event.values[0] + event.values[1] + event.values[2]) > 5) {
                morse = "";
                tv = "";
                text.setText(tv);
            }
        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO 自動生成されたメソッド・スタブ

    }
}

