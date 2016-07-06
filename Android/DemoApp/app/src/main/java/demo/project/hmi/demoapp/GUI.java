package demo.project.hmi.demoapp;
//android:sharedUserId="android.uid.system"
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class GUI extends AppCompatActivity {
    RelativeLayout m_panel;
    private int mBindFlag;
    private Messenger mServiceMessenger;
    private static final String TAG = "SpeechRecognizerService";
    //utilizzate per accedere alla preferences activity
    private SharedPreferences prefs;
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, PreferencesActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    public void onDestroy(){
        PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("ExIsOn", false).commit();
        PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("InIsOn", false).commit();
        super.onDestroy();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Disegna il layout
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        m_panel = new RelativeLayout(this);
        setContentView(m_panel);
        LinearLayout panelV = new LinearLayout(this);
        panelV.setOrientation(LinearLayout.VERTICAL);
        RelativeLayout.LayoutParams lp_iv = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp_iv.addRule(RelativeLayout.CENTER_IN_PARENT);
        m_panel.addView(panelV, lp_iv);
        //Inizializzo le preference activity
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Aggiunge i vari bottoni (StartService, StartDemo, StopService, Hide)
        Button bStartService = new Button(this);
        bStartService.setText("Start");
        panelV.addView(bStartService);
        Button bStopService = new Button(this);
        bStopService.setText("Stop");
        panelV.addView(bStopService);
        Button bHide = new Button(this);
        bHide.setText("Hide");
        panelV.addView(bHide);
        Button bStartDemo = new Button(this);
        bStartDemo.setText("Start demo");
        panelV.addView(bStartDemo);
        TextView tv = new TextView(this);
        tv.setTextColor(Color.BLACK);
        tv.setText("Press Start then Hide to see the cursor move");
        panelV.addView(tv);
        cmdTurnCursorServiceOn();
        //Implementa i listener sui vari bottoni
        bStartService.setOnClickListener(new View.OnClickListener() {
            //Visualizza il cursore e lancia il servizio di riconoscimento vocale
            public void onClick(View v) {
                Intent i = new Intent(getBaseContext(), SpeechRecognizerService.class);
                startService(i);
                mBindFlag = Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT ? 0 : Context.BIND_ABOVE_CLIENT;
                cmdShowCursor();
                startRecognition();
                startService(new Intent(getBaseContext(), OverlayService.class));
            }
        });
        bStopService.setOnClickListener(new View.OnClickListener() {
            //Ferma lo spostamento del cursore
            public void onClick(View v) {
                cmdHideCursor();
            }
        });
        bHide.setOnClickListener(new View.OnClickListener() {
            //Crea una nuova activity con layout trasparente
            public void onClick(View v) {
                Intent newActivity = new Intent(Intent.ACTION_MAIN);
                newActivity.addCategory(Intent.CATEGORY_HOME);
                newActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(newActivity);
            }
        });
        bStartDemo.setOnClickListener(new View.OnClickListener(){
            //Apre una nuova activity, contenente i bottoni della demo
            public void onClick(View v){
                Intent intent = new Intent(GUI.this, DemoActivity.class);
                startActivity(intent);
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
            }catch (RemoteException e){e.printStackTrace();}
        }
        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            Log.d(TAG, "onServiceDisconnected"); //$NON-NLS-1$
            mServiceMessenger = null;
        }
    };
    //Funzione per creare il servizio del cursore
    private void cmdTurnCursorServiceOn() {
        Intent newService = new Intent(GUI.this, InternalCursorService.class);
        startService(newService);
        //startService(new Intent(this, ExternalCursorService.class));
        startService(new Intent(this, ExternalCursorService.class));
    }
    //Crea un messaggio di broadcast per notificare al servizio la visualizzazione del servizio
    private void cmdShowCursor() {
        Intent intent=new Intent();
        intent.setAction("demo.project.hmi.demoapp.msg");
        if (prefs.getBoolean("SwitchSensore", false)){
            intent.putExtra("Action", "ShowCursorEx");
            prefs.edit().putBoolean("ExIsOn", true).commit();
        }else{
            intent.putExtra("Action", "ShowCursor");
            prefs.edit().putBoolean("InIsOn", true).commit();
        }
        sendBroadcast(intent);
    }
    //Crea un messaggio di broadcast per notificare al servizio il termine della visualizzazione
    private void cmdHideCursor() {
        Intent intent=new Intent();
        intent.setAction("demo.project.hmi.demoapp.msg");
        if (prefs.getBoolean("SwitchSensore", false)){
            intent.putExtra("Action", "HideCursorEx");
            prefs.edit().putBoolean("ExIsOn", false).commit();
        }else{
            intent.putExtra("Action", "HideCursor");
            prefs.edit().putBoolean("InIsOn", false).commit();
        }
        sendBroadcast(intent);
    }
}
