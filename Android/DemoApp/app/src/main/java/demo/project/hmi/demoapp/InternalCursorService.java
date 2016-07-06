package demo.project.hmi.demoapp;

import android.app.Instrumentation;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.gesture.Gesture;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.WindowManager;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;


//http://www.pocketmagic.net/android-overlay-cursor/
/* @project 
 * 
 * License to access, copy or distribute this file.
 * This file or any portions of it, is Copyright (C) 2012, Radu Motisan ,  http://www.pocketmagic.net . All rights reserved.
 * @author Radu Motisan, radu.motisan@gmail.com
 * 
 * This file is protected by copyright law and international treaties. Unauthorized access, reproduction 
 * or distribution of this file or any portions of it may result in severe civil and criminal penalties.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * 
 * @purpose 
 * Cursor Overlay Sample
 * (C) 2012 Radu Motisan , all rights reserved.
 */
public class InternalCursorService extends Service implements SensorEventListener {

    private int count = 0;
    private class Receiver extends BroadcastReceiver {
        @Override
        //Funzione eseguita alla ricezione di un messaggio (generati in GUI)
        public void onReceive(Context context, Intent intent) {
            //Prende la richiesta
            String action = intent.getStringExtra("Action");
            if(action == null)
                return;
            switch (action){
                //Visualizza il cursore
                case "ShowCursor":{
                    centerX = 0;
                    centerY = 0;
                    ShowCursor(true);
                    break;
                }
                //Nasconde il cursore
                case "HideCursor":{
                    ShowCursor(false);
                    //sensorManager.unregisterListener();
                    break;
                }
                //Esegue il click
                case "Click":{
                    OnClick();
                    break;
                }
                //Esegue il click lungo
                case "Lungo":{
                    OnLongClick();
                    break;
                }
                //Swipe sinistro
                case "SLeft":{
                    OnSwipe(Direction.Left);
                    break;
                }
                //swipe destro
                case "SRight":{
                    OnSwipe(Direction.Right);
                    break;
                }
                //swipe alto
                case "SUp":{
                    OnSwipe(Direction.Up);
                    break;
                }
                //swipe basso
                case "SDown":{
                    OnSwipe(Direction.Down);
                    break;
                }
                //Ferma il servizio
                case "Destroy":{
                    ShowCursor(false);
                    break;
                }
                //Esegue il click indietro
                case "Back":{
                    try{
                        Runtime.getRuntime().exec("input keyevent " + KeyEvent.KEYCODE_BACK);
                    }catch (IOException e){}
                    break;
                }
                //Esegue il click sul tasto home
                case "Home":{
                    try{
                        Runtime.getRuntime().exec("input keyevent " + KeyEvent.KEYCODE_HOME);
                    }catch (IOException e){}
                    break;
                }
            }
        }
    }
    private OverlayView mView;
    private SensorManager sensorManager;
    private Sensor accelerometer, gyro;
    private int prevSecond = 0;
    private double centerX = 0, centerY = 0, saveX = 0, saveY = 0;
    private int transition = 1, //Transizione del cursore (da rimuovere)
            X = 0, //Coordinata X
            Y = 0, //Coordinata Y
            m_nScreenW = 0,//Larghezza schermo
            m_nScreenH = 0;//Altezza schermo
    private double pi = Math.PI; //PiGreca
    private double accx=0, tempx=0;
    private double accy=0, tempy=0;
    private double gyroy=0;
    private double gyrox=0;
    private double previousX = 0, previousY = 0;
    private double[] h = new double[]{
            0.041275130475134675,
            0.09980846342214969,
            0.16876227509747477,
            0.21514995135890577,
            0.21514995135890577,
            0.16876227509747477,
            0.09980846342214969,
            0.041275130475134675
    };
    private long downTime;
    private int xEnd = 0, yEnd = 0;
    private int mBindFlag;
    private Messenger mServiceMessenger;
    private static final String TAG = "SpeechRecognizerService";
    public enum Direction {//enumerativo per la direzione dello swipe
        Left, Right, Up, Down;
    }
    private void OnSwipe(Direction direction) {
        Instrumentation inst = new Instrumentation();
        downTime = SystemClock.uptimeMillis();
        xEnd = 0; yEnd = 0;
        switch (direction){
            case Right: {
                xEnd = m_nScreenW;
                yEnd = Y;
                break;
            }
            case Left:{
                xEnd = 0;
                yEnd = Y;
                break;
            }
            case Up:{
                yEnd = 0;
                xEnd = X;
                break;
            }
            case Down:{
                yEnd = m_nScreenH;
                xEnd = X;
                break;
            }
        }
        new Thread() {
            @Override
            public void run() {
                Instrumentation m_Instrumentation = new Instrumentation();
                try {
                    m_Instrumentation.sendPointerSync(MotionEvent.obtain(downTime, SystemClock.uptimeMillis(),
                            MotionEvent.ACTION_DOWN, X, Y, 0));
                    m_Instrumentation.sendPointerSync(MotionEvent.obtain(downTime, SystemClock.uptimeMillis(),
                            MotionEvent.ACTION_MOVE, xEnd, yEnd, 0));
                    m_Instrumentation.sendPointerSync(MotionEvent.obtain(downTime, SystemClock.uptimeMillis() + 1000,
                            MotionEvent.ACTION_UP, xEnd, yEnd, 0));
                } catch (Exception e) {}

            }
        }.start();

    }
    //Thread per l'esecuzione del click
    private final Thread thread = new Thread() {
        @Override
        public void run() {
            //Esegue un click alle coordinate X e Y
            Instrumentation m_Instrumentation = new Instrumentation();
            try {
                m_Instrumentation.sendPointerSync(MotionEvent.obtain(
                        SystemClock.uptimeMillis(),
                        SystemClock.uptimeMillis() + 10,
                        MotionEvent.ACTION_DOWN, X, Y, 0));
                m_Instrumentation.sendPointerSync(MotionEvent.obtain(
                        SystemClock.uptimeMillis(),
                        SystemClock.uptimeMillis() + 10,
                        MotionEvent.ACTION_UP, X, Y, 0));
            } catch (Exception e) {}
        }
    };
    /**
     * @param x
     * @param y
     * @param autoenable if set, it will automatically show the cursor when movement is detected
     */
    public void Updater(final int x, final int y, final boolean autoenable) {
        mView.Update(x, y);
        if ((x!=0 || y!= 0) && autoenable && !mView.isCursorShown() )
            ShowCursor(true); //will also post invalidate
        else
            mView.postInvalidate();
    }
    private void OnLongClick(){
        //Controlla se il cursore è abilitato
        if(!mView.isCursorShown())
            return;
        //Eseguo il thread che effettua il click
        new Thread() {
            @Override
            public void run() {
                //Esegue un click alle coordinate X e Y
                try {
                    Instrumentation inst = new Instrumentation();
                    long downTime = SystemClock.uptimeMillis();
                    long eventTime = SystemClock.uptimeMillis();
                    MotionEvent event = MotionEvent.obtain(downTime, eventTime,
                            MotionEvent.ACTION_DOWN, X, Y, 0);
                    MotionEvent event2 = MotionEvent.obtain(downTime, eventTime,
                            MotionEvent.ACTION_UP, X, Y, 0);
                    inst.sendPointerSync(event);
                    try {
                        Thread.sleep(750);
                    }catch (Exception e){}
                    inst.sendPointerSync(event2);
                } catch (NullPointerException e) {
                    // TODO: handle exception
                    e.printStackTrace();
                }
            }
        }.start();
    }
    private void OnClick(){
        //Controlla se il cursore è abilitato
        if(!mView.isCursorShown())
            return;
        //Eseguo il thread che effettua il click
        new Thread() {
            @Override
            public void run() {
                //Esegue un click alle coordinate X e Y
                Instrumentation m_Instrumentation = new Instrumentation();
                try {
                    m_Instrumentation.sendPointerSync(MotionEvent.obtain(
                            SystemClock.uptimeMillis(),
                            SystemClock.uptimeMillis() + 10,
                            MotionEvent.ACTION_DOWN, X, Y, 0));
                    m_Instrumentation.sendPointerSync(MotionEvent.obtain(
                            SystemClock.uptimeMillis(),
                            SystemClock.uptimeMillis() + 10,
                            MotionEvent.ACTION_UP, X, Y, 0));
                } catch (Exception e) {}
            }
        }.start();
    }
    //Imposta il booleano per la visualizzazione o meno del cursore
    public void ShowCursor(boolean status) {
        mView.ShowCursor(status);
        mView.postInvalidate();
    }
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    @Override
    public void onDestroy(){
        PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("ExIsOn", false).commit();
        PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("InIsOn", false).commit();
        super.onDestroy();
    }
    @Override
    public void onCreate() {
        super.onCreate();
        //Registro il receiver dei messaggi
        registerReceiver(new Receiver(), new IntentFilter("demo.project.hmi.demoapp.msg"));
        mView = new OverlayView(this);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        params.setTitle("Cursor");
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        wm.addView(mView, params);
        // get screen size
        DisplayMetrics metrics = new DisplayMetrics();
        try {
            ((WindowManager)getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getMetrics(metrics);
            m_nScreenW = metrics.widthPixels;
            m_nScreenH = metrics.heightPixels;
            X = m_nScreenW/2;
            Y = m_nScreenH/2;
        }
        catch (Exception e) { //default to a HVGA 320x480 and let's hope for the best
            m_nScreenW = 0;
            m_nScreenH = 0;
        }
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        sensorManager.registerListener(this, gyro, SensorManager.SENSOR_DELAY_NORMAL);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if(prefs.getBoolean("OnBootIn", false)){
            ShowCursor(true);
            prefs.edit().putBoolean("OnBootIn", false).commit();
            Intent i = new Intent(getBaseContext(), SpeechRecognizerService.class);
            startService(i);
            mBindFlag = Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT ? 0 : Context.BIND_ABOVE_CLIENT;
            startRecognition();
        }
    }
    private void startRecognition(){
        bindService(new Intent(this, SpeechRecognizerService.class), mServiceConnection, mBindFlag);
    }
    private final ServiceConnection mServiceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            Log.d(TAG, "onServiceConnected"); //$NON-NLS-1$
            mServiceMessenger = new Messenger(service);
            Message msg = new Message();
            msg.what = SpeechRecognizerService.MSG_RECOGNIZER_START_LISTENING;
            try{mServiceMessenger.send(msg);
            }catch (RemoteException e){}
        }
        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            Log.d(TAG, "onServiceDisconnected"); //$NON-NLS-1$
            mServiceMessenger = null;
        }
    };


    public void onSensorChanged(SensorEvent event) {
        if(!mView.isCursorShown())
            return;
        switch(event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:		//prende i dati da accelerometro
                float axisXa = event.values[0];
                float axisYa = event.values[1];
                accx=axisXa;
                accy=axisYa;
                break;
            case Sensor.TYPE_GYROSCOPE:			//prende i dati da girocoscopio
                float axisY = event.values[0];
                float axisX = event.values[1];
                //converte rad in angoli
                int rx =(int)(axisX*(180/pi));
                int ry =(int)(axisY*(180/pi));
                gyroy = (double)ry;
                gyrox = (double)rx;
                break;
        }
        if(previousX == 0 && previousY == 0){
            previousX = accx;
            previousY = accy;
            return;
        }
        double rate = 0.05;
        accx = rate * accx + (1.0 - rate) * previousX;
        accy = rate * accy + (1.0 - rate) * previousY;
        Calendar cal = Calendar.getInstance();
        //Se è la prima volta che eseguo la move, utilizzo queste coordinate come quelle di riferimento
        if (centerX == 0 && centerY == 0) {
            centerX = accx;
            centerY = accy;
            saveX = X;
            saveY = Y;
            count = 0;
        } else {
            double div = (double) 6 / (m_nScreenW / 2);
            //Guardo l'escursione
            double deltax = accx - centerX;
            deltax = (deltax > 0) ? deltax : deltax * (-1);
            if (deltax <= 9) {
                if (accx < centerX) {
                    //Quindi devo aumentare x
                    //Quanto leggo fratto 9
                    X = (int) Math.round(deltax / div) + (m_nScreenW / 2);
                } else if (accx > centerX) {
                    //Quindi devo diminuire x
                    //Quanto leggo fratto 9
                    X = (m_nScreenW / 2) - (int) Math.round(deltax / div);
                }
            } else {
                X = (accx < centerX) ? m_nScreenW : 0;
            }
            div = (double) 5 / (m_nScreenH / 2);
            double deltay = Math.abs(accy - centerY);
            deltay = (deltay > 0) ? deltay : deltay * (-1);
            if (deltay <= 25) {
                if (accy > centerY) {
                    //Quindi devo aumentare x
                    Y = (int) Math.round(deltay / div) + (m_nScreenH / 2);
                } else if (accy < centerY) {
                    //Quindi devo aumentare x
                    //Quanto leggo fratto 9
                    Y = (m_nScreenH / 2) - (int) Math.round(deltay / div);		//da invertire con + teoricamente
                }
            }else{
                Y = (accy < centerY) ? m_nScreenH : 0;
            }
        }
        Updater(Math.round(X), Math.round(Y), false);
        previousX = accx;
        previousY = accy;
        SimpleDateFormat sdf = new SimpleDateFormat("ss");
        int now = Integer.parseInt(sdf.format(cal.getTime()));
        if((now - prevSecond) >= 1){
            //Calcolo la distanza tra due punti
            double distance = Math.sqrt((Math.pow((X - saveX),2) - Math.pow((Y - saveY), 2)));
            if(distance < 50) {
                OnClick();
            }
            saveX = X;
            saveY = Y;
            prevSecond = now;
        }else if(prevSecond == 59)
            prevSecond = now;

    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}

