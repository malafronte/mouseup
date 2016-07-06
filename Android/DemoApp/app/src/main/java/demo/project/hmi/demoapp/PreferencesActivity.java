package demo.project.hmi.demoapp;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by Matteo on 12/04/2016.
 */
public class PreferencesActivity extends PreferenceActivity {
    private AppCompatDelegate mDelegate;
    private int mBindFlag;
    private Messenger mServiceMessenger;
    private static final String TAG = "SpeechRecognizerService";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getDelegate().installViewFactory();
        getDelegate().onCreate(savedInstanceState);
        super.onCreate(savedInstanceState);
        //Aggiungo l'xml delle preferenze
        addPreferencesFromResource(R.xml.preference_scenario);
        //Creo un'istanza delle sp
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Creo il listener
        SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                // Implementation
                switch (key){
                    case("Sensibilità"):{
                        //Float temp = prefs.getFloat(key, 0);
                        //comunico il cambio di sensibilità
                        Intent intent = new Intent();
                        intent.setAction("demo.project.hmi.demoapp.msg");
                        intent.putExtra("Action", "Change");
                        sendBroadcast(intent);
                        break;
                    }
                    case("lisVerticale"):{
                        Intent intent = new Intent();
                        intent.setAction("demo.project.hmi.demoapp.msg");
                        intent.putExtra("Action", "ChangeAsse");
                        sendBroadcast(intent);
                        break;
                    }
                    case("lisOrizzontale"):{
                        Intent intent = new Intent();
                        intent.setAction("demo.project.hmi.demoapp.msg");
                        intent.putExtra("Action", "ChangeAsse");
                        sendBroadcast(intent);
                        break;
                    }
                    case("SwitchSensore"):{
                        //Se attivo i sensori esterni
                        if(prefs.getBoolean(key, false)){
                            //Comunico il cambio
                            Intent intent = new Intent();
                            intent.setAction("demo.project.hmi.demoapp.msg");
                            intent.putExtra("Action", "Destroy");
                            sendBroadcast(intent);
                            //Controllo se il servizio era già attivo
                            if(prefs.getBoolean("InIsOn", false)) {
                                //Abilito il nuovo servizio
                                intent = new Intent();
                                intent.setAction("demo.project.hmi.demoapp.msg");
                                intent.putExtra("Action", "ShowCursorEx");
                                sendBroadcast(intent);
                                prefs.edit().putBoolean("ExIsOn", true).commit();
                                prefs.edit().putBoolean("InIsOn", false).commit();
                            }
                        }else{
                            //Comunico il cambio
                            Intent intent = new Intent();
                            intent.setAction("demo.project.hmi.demoapp.msg");
                            intent.putExtra("Action", "DestroyEx");
                            sendBroadcast(intent);
                            //Controllo se il servizio precedente era già attivo
                            if(prefs.getBoolean("ExIsOn", false)) {
                                intent = new Intent();
                                intent.setAction("demo.project.hmi.demoapp.msg");
                                intent.putExtra("Action", "ShowCursor");
                                sendBroadcast(intent);
                                prefs.edit().putBoolean("InIsOn", true).commit();
                                prefs.edit().putBoolean("ExIsOn", false).commit();
                            }
                        }
                        break;
                    }
                }
            }
        };
        //Registro il listener
        prefs.registerOnSharedPreferenceChangeListener(listener);
    }
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        getDelegate().onPostCreate(savedInstanceState);
    }

    public ActionBar getSupportActionBar() {
        return getDelegate().getSupportActionBar();
    }

    public void setSupportActionBar(@Nullable Toolbar toolbar) {
        getDelegate().setSupportActionBar(toolbar);
    }

    @Override
    public MenuInflater getMenuInflater() {
        return getDelegate().getMenuInflater();
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        getDelegate().setContentView(layoutResID);
    }

    @Override
    public void setContentView(View view) {
        getDelegate().setContentView(view);
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        getDelegate().setContentView(view, params);
    }

    @Override
    public void addContentView(View view, ViewGroup.LayoutParams params) {
        getDelegate().addContentView(view, params);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        getDelegate().onPostResume();
    }

    @Override
    protected void onTitleChanged(CharSequence title, int color) {
        super.onTitleChanged(title, color);
        getDelegate().setTitle(title);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        getDelegate().onConfigurationChanged(newConfig);
    }

    @Override
    protected void onStop() {
        super.onStop();
        getDelegate().onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getDelegate().onDestroy();
    }

    public void invalidateOptionsMenu() {
        getDelegate().invalidateOptionsMenu();
    }

    private AppCompatDelegate getDelegate() {
        if (mDelegate == null) {
            mDelegate = AppCompatDelegate.create(this, null);
        }
        return mDelegate;
    }
}