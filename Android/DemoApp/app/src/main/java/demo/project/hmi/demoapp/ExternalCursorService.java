package demo.project.hmi.demoapp;

import android.app.Instrumentation;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
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
import android.view.WindowManager;
import android.widget.Toast;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class ExternalCursorService extends Service {
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothSocket mmSocket;
    private BluetoothDevice mmDevice;
    private OutputStream mmOutputStream;
    private InputStream mmInputStream;
    private Thread workerThread;
    private byte[] readBuffer;
    private int count = 0;

    private int readBufferPosition;
    private double centerX = 0, centerY = 0, saveX = 0, saveY = 0;
    private volatile boolean stopWorker;
    private SharedPreferences prefs;
    private String asseX = "3", asseY = "1";
    private int mBindFlag;
    private Messenger mServiceMessenger;
    private static final String TAG = "SpeechRecognizerService";
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
                case "ShowCursorEx":{
                        X = m_nScreenW / 2;
                        Y = m_nScreenH / 2;
                        ScreenY = Y;
                        ScreenX = X;
                        centerX = 0;
                        centerY = 0;
                        ShowCursor(true);
                        try {
                            findBT();   //trova dispositivo bluetooth
                            openBT();   //instaura connessione bluetooth e riceve dati
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    break;
                }
                //Nasconde il cursore
                case "HideCursorEx":{
                    ShowCursor(false);
                    break;
                }
                case "LungoEx":{
                    OnLongClick();
                    break;
                }
                //Esegue il click
                case "ClickEx":{
                    OnClick();
                    break;
                }
                case "SLeftEx":{
                    OnSwipe(Direction.Left);
                    break;
                }
                case "SRightEx":{
                    OnSwipe(Direction.Right);
                    break;
                }
                case "Calibra":{
                    X = m_nScreenW / 2;
                    Y = m_nScreenH / 2;
                    ScreenY = Y;
                    ScreenX = X;
                    centerX = 0;
                    centerY = 0;
                    break;
                }
                case "SUpEx":{
                    OnSwipe(Direction.Up);
                    break;
                }
                case "SDownEx":{
                    OnSwipe(Direction.Down);
                    break;
                }
                case "DestroyEx":{
                    ShowCursor(false);
                    break;
                }
                /*case "Change":{
                    Float temp = prefs.getFloat("Sensibilità", 0);
                    int laps = 10;
                    if (temp <= 0.2)
                        transition = 15 + 30;
                    if (temp > 0.2 && temp <= 0.4)
                        transition = 15 + 40;
                    if (temp > 0.4 && temp <= 0.6)
                        transition = 15 + 50;
                    if (temp > 0.6 && temp <= 0.8)
                        transition = 15 + 60;
                    if (temp > 0.8)
                        transition = 15 + 70;
                    break;
                }*/
                case "ChangeAsse":{
                    asseY = prefs.getString("lisVerticale", "");
                    asseX = prefs.getString("lisOrizzontale", "");
                    break;
                }
                case "Back":{
                    try{
                        Runtime.getRuntime().exec("input keyevent " + KeyEvent.KEYCODE_BACK);
                    }catch (IOException e){}
                    break;
                }
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
    private int
            X, //Coordinata X
            Y, //Coordinata Y
            ScreenX = 0,
            ScreenY = 0,
            m_nScreenW = 0,//Larghezza schermo
            m_nScreenH = 0;//Altezza schermo
    private float gyroy=0;
    private float gyrox=0;
    private long downTime;
    private int xEnd = 0, yEnd = 0;
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
        // The value for y doesn't change, as we want to swipe straight across
        new Thread() {
            @Override
            public void run() {
                //Esegue un click alle coordinate X e Y
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
    /**
     * metodo per la ricerca di un disposito bluetooth
     */
    void findBT()
    {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();   //inizializzo bluetooth sul dispositivo
        if(mBluetoothAdapter == null)
        {
            Toast.makeText(this.getApplicationContext(), "No bluetooth adapter available", Toast.LENGTH_SHORT).show();
        }
        if(!mBluetoothAdapter.isEnabled())  //bluetooth non attivo
        {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);    //chiede di attivare
            enableBluetooth.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(enableBluetooth);
        }
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0)
        {
            for(BluetoothDevice device : pairedDevices)
            {
                if(device.getName().equals("Vv-HC04"))      //se esiste il modulo bluetooth Vv-HC04A (nome a scelta)
                {
                    mmDevice = device;                      //lo associo al nome che invia dati
                    break;
                }
            }
        }
    }
    /**
     * metodo che instaura una connessione bluetooth aprendo delle socket.
     * Richiama i metodi initializeBT() e beginListenForData() per avere un collegamento più facile per la ricezione dei dati
     * non appena creata la connessione
     * @throws IOException
     */
    void openBT() throws IOException
    {
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard SerialPortService ID
        //apertura socket
        mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
        mmSocket.connect();
        mmOutputStream = mmSocket.getOutputStream();
        mmInputStream = mmSocket.getInputStream();
        initializeBT();             //avvia connessione fra bluetooth
        beginListenForData();       //legge dati da bluetooth
    }

    /**
     * metodo che manda in eseguzione un thread che rimane sempre in ascolto per ricevere dati dal bluetooth
     */
    void beginListenForData()
    {
        final Handler handler = new Handler();
        final byte delimiter = 10; //This is the ASCII code for a newline character
        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];        // il bluetooth invia byte
        //creo thread
        workerThread = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopWorker)
                {
                    try
                    {
                        int bytesAvailable = mmInputStream.available();
                        if(bytesAvailable > 0)
                        {
                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInputStream.read(packetBytes); //leggo dato dal bluetooth
                            for(int i=0;i<bytesAvailable;i++)
                            {
                                //converto dato da byte a String
                                byte b = packetBytes[i];
                                if(b == delimiter)
                                {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;
                                    Log.d("DATAX: ", data);
                                    handler.post(new Runnable()
                                    {
                                        public void run()
                                        {
                                            move(data);
                                            //c.myOnSensorChanged(data);
                                        }
                                    });
                                    break;
                                }
                                else
                                {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    }
                    catch (IOException ex)
                    {
                        stopWorker = true;
                    }
                }
            }
        });
        workerThread.start();   //eseguo thread
    }
    void move(String r){
        String[] parts = r.split("u");   //divide la stringa per ottenere valori singoli
        //Valore 1 x, valore 2 y, valore 3 z
        try {
            switch (asseX.toLowerCase()) {
                case ("1"): {
                    gyrox = Float.parseFloat(parts[0]);
                    break;
                }
                case ("2"): {
                    gyrox = Float.parseFloat(parts[1]);
                    break;
                }
                case ("3"): {
                    gyrox = Float.parseFloat(parts[2]);
                    break;
                }
            }
            switch (asseY.toLowerCase()) {
                case ("1"): {
                    gyroy = Float.parseFloat(parts[0]);
                    break;
                }
                case ("2"): {
                    gyroy = Float.parseFloat(parts[1]);
                    break;
                }
                case ("3"): {
                    gyroy = Float.parseFloat(parts[2]);
                    break;
                }
            }
            //Se è la prima volta che eseguo la move, utilizzo queste coordinate come quelle di riferimento
            if (centerX == 0 && centerY == 0) {
                centerX = gyrox;
                centerY = gyroy;
                saveX = X;
                saveY = Y;
                count = 0;
            } else {
                double div = (double) 9 / (m_nScreenW / 2);
                //Guardo l'escursione
                double deltax = gyrox - centerX;
                deltax = (deltax > 0) ? deltax : deltax * (-1);
                if (deltax <= 9) {
                    if (gyrox < centerX) {
                        //Quindi devo aumentare x
                        //Quanto leggo fratto 9
                        X = (int) Math.round(deltax / div) + (m_nScreenW / 2);
                    } else if (gyrox > centerX) {
                        //Quindi devo aumentare x
                        //Quanto leggo fratto 9
                        X = (m_nScreenW / 2) - (int) Math.round(deltax / div);
                    }
                } else {
                    X = (gyrox < centerX) ? m_nScreenW : 0;
                }
                div = (double) 15 / (m_nScreenH / 2);
                double deltay = Math.abs(gyroy - centerY);
                deltay = (deltay > 0) ? deltay : deltay * (-1);
                if (deltay <= 25) {
                    if (gyroy < centerY) {
                        //Quindi devo aumentare x
                        Y = (int) Math.round(deltay / div) + (m_nScreenH / 2);
                    } else if (gyroy > centerY) {
                        //Quindi devo aumentare x
                        //Quanto leggo fratto 9
                        Y = (m_nScreenH / 2) - (int) Math.round(deltay / div);
                    }
                }else{
                    Y = (gyroy < centerY) ? m_nScreenH : 0;
                }
            }
            Updater(Math.round(X), Math.round(Y), false);
            if(count == 15){
                //Calcolo la distanza tra due punti
                double distance = Math.sqrt((Math.pow((X - saveX),2) - Math.pow((Y - saveY), 2)));
                if(distance < 40) {
                    OnClick();
                }
                saveX = X;
                saveY = Y;
                count = 0;
            }else
                count++;
        }catch (NumberFormatException e){}
    }

    /**
     * metodo che invia il primo messaggio bletooth indicando che
     * il programma è pronto a ricevere
     * @throws IOException
     */
    void initializeBT() throws IOException {
        String msg = "ready";
        msg += "\n";
        mmOutputStream.write(msg.getBytes());       //invia messaggio in byte
    }

    /**
     * metodo che chiude la connessione bluetooth
     * chiudendo la comunicazione via socket
     * @throws IOException
     */
    void closeBT() throws IOException
    {
        stopWorker = true;
        //chiusura socket
        mmOutputStream.close();
        mmInputStream.close();
        mmSocket.close();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
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
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,//TYPE_SYSTEM_ALERT,//TYPE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, //will cover status bar as well!!!
                PixelFormat.TRANSLUCENT);
        params.setTitle("Cursor");
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        wm.addView(mView, params);
        // get screen size
        DisplayMetrics metrics = new DisplayMetrics();
        try {
            ((WindowManager)getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getMetrics(metrics);
            m_nScreenW = metrics.widthPixels;
            m_nScreenH = metrics.heightPixels;
            X = m_nScreenW /2;
            Y = m_nScreenH / 2;
            ScreenY = Y;
            ScreenX = X;
            Updater(Math.round(X), Math.round(Y), false);
        }
        catch (Exception e) { //default to a HVGA 320x480 and let's hope for the best
            e.printStackTrace();
            m_nScreenW = 0;
            m_nScreenH = 0;
        }
        if(prefs.getBoolean("OnBootEx", false)){
            ShowCursor(true);
            prefs.edit().putBoolean("OnBootEx", false).commit();
            Intent i = new Intent(getBaseContext(), SpeechRecognizerService.class);
            startService(i);
            mBindFlag = Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT ? 0 : Context.BIND_ABOVE_CLIENT;
            startRecognition();
            try {
                findBT();   //trova dispositivo bluetooth
                openBT();   //instaura connessione bluetooth e riceve dati
            }catch (Exception e){}
        }
        asseY = prefs.getString("lisVerticale", "1");
        asseX = prefs.getString("lisOrizzontale", "3");

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
            }catch (RemoteException e){e.printStackTrace();}
        }
        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            Log.d(TAG, "onServiceDisconnected"); //$NON-NLS-1$
            mServiceMessenger = null;
        }
    };
}


