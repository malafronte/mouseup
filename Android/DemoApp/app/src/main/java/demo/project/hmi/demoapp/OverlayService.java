package demo.project.hmi.demoapp;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

/**
 * Created by Matteo on 26/05/2016.
 */
//Servizio per l'overlay del widget
public class OverlayService extends Service {
    private int mBindFlag;
    private Messenger mServiceMessenger;
    private static final String TAG = "SpeechRecognizerService";
    private boolean isMoved = false, isOn = true;
    private WindowManager windowManager;
    private ImageView chatHead;
    private WindowManager.LayoutParams params;
    @Override public IBinder onBind(Intent intent) {
        // Not used
        return null;
    }
    @Override public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        chatHead = new ImageView(this);
        chatHead.setImageResource(R.drawable.bar2);
        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = 0;
        params.y = 100;
        windowManager.addView(chatHead, params);
        chatHead.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;
            @Override public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        isMoved = false;
                        return true;
                    case MotionEvent.ACTION_UP:
                        if(isMoved){
                            isMoved = false;
                            return true;
                        }
                        if((initialTouchX > 130)&&(initialTouchX < 200)){
                            //indietro
                            Intent intent=new Intent();
                            intent.setAction("demo.project.hmi.demoapp.msg");
                            intent.putExtra("Action", "Back");
                            sendBroadcast(intent);
                        }else if((initialTouchX > 500)&&(initialTouchX < 600)){
                            //home
                            Intent intent=new Intent();
                            intent.setAction("demo.project.hmi.demoapp.msg");
                            intent.putExtra("Action", "Home");
                            sendBroadcast(intent);
                        }else if ((initialTouchX > 900)&&(initialTouchX < 1000)){
                            //Speech recognition
                            if(isOn) {
                                Intent intent = new Intent();
                                intent.setAction("demo.project.hmi.demoapp.speech");
                                intent.putExtra("MessageForSpeech", "DestroySpeech");
                                sendBroadcast(intent);
                                isOn = false;
                                chatHead.setImageResource(R.drawable.bar);
                            }else{
                                Intent i = new Intent(getBaseContext(), SpeechRecognizerService.class);
                                startService(i);
                                mBindFlag = Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT ? 0 : Context.BIND_ABOVE_CLIENT;
                                startRecognition();
                                isOn = true;
                                chatHead.setImageResource(R.drawable.bar2);
                            }
                        }
                        return true;
                    //Nel caso in cui si trascini il widget
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        if(Math.abs(params.y - initialY) > 40)
                            isMoved = true;
                        windowManager.updateViewLayout(chatHead, params);
                        return true;
                }
                return false;
            }
        });

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
    @Override
    public void onDestroy() {
        super.onDestroy();
        windowManager.removeView(chatHead);
        this.stopSelf();
    }
}