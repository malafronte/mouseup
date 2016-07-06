package demo.project.hmi.demoapp;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;



public class SpeechRecognizerService extends Service
{

    static protected AudioManager mAudioManager;
    protected SpeechRecognizer mSpeechRecognizer;
    protected Intent mSpeechRecognizerIntent;
    protected final Messenger mServerMessenger = new Messenger(new IncomingHandler(this));
    protected boolean mIsListening;
    static private boolean mIsStreamSolo;
    private static final String TAG = "SpeechRecognizer Intent";
    static final int MSG_RECOGNIZER_START_LISTENING = 1;
    static final int MSG_RECOGNIZER_CANCEL = 2;
    private class Receiver extends BroadcastReceiver {
        @Override
        //Funzione eseguita alla ricezione di un messaggio (generati in GUI)
        public void onReceive(Context context, Intent intent) {
            //Prende la richiesta
            String action = intent.getStringExtra("MessageForSpeech");
            switch (action){
                //Distruggo il servizio
                case "DestroySpeech": {
                    destroy();
                    break;
                }
            }
        }
    }
    private void destroy(){
        this.stopSelf();
    }
    private SharedPreferences prefs;
    @Override
    public void onCreate()
    {
        super.onCreate();
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        registerReceiver(new Receiver(), new IntentFilter("demo.project.hmi.demoapp.speech"));
    }

    private void createRecognizer(){
        mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        mSpeechRecognizer.setRecognitionListener(new SpeechRecognitionListener());
        mSpeechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, Locale.getDefault());
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getApplication().getPackageName());
    }

    protected static class IncomingHandler extends Handler
    {
        private WeakReference<SpeechRecognizerService> mtarget;
        IncomingHandler(SpeechRecognizerService target)
        {
            mtarget = new WeakReference<SpeechRecognizerService>(target);
        }


        @Override
        public void handleMessage(Message msg)
        {
            final SpeechRecognizerService target = mtarget.get();
            switch (msg.what)
            {
                case MSG_RECOGNIZER_START_LISTENING:
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                    {
                        // turn off beep sound  
                        if (!mIsStreamSolo)
                        {
                            mAudioManager.setStreamSolo(AudioManager.STREAM_VOICE_CALL, true);
                            mIsStreamSolo = true;
                        }
                    }
                    if (!target.mIsListening)
                    {
                        if(target.mSpeechRecognizer != null)
                            target.mSpeechRecognizer.destroy();
                        target.createRecognizer();
                        target.mSpeechRecognizer.startListening(target.mSpeechRecognizerIntent);
                        target.mIsListening = true;
                        Log.d(TAG, "message start listening"); //$NON-NLS-1$
                    }
                    break;
                case MSG_RECOGNIZER_CANCEL:
                    if (mIsStreamSolo)
                    {
                        mAudioManager.setStreamSolo(AudioManager.STREAM_VOICE_CALL, false);
                        mIsStreamSolo = false;
                    }
                    target.mSpeechRecognizer.destroy();
                    target.mIsListening = false;
                    Log.d(TAG, "message canceled recognizer"); //$NON-NLS-1$
                    break;
            }
        }
    }
    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if (mSpeechRecognizer != null)
        {
            mSpeechRecognizer.destroy();
        }
    }
    protected class SpeechRecognitionListener implements RecognitionListener
    {
        @Override
        public void onBeginningOfSpeech(){Log.d(TAG, "onBeginingOfSpeech"); //$NON-NLS-1$
        }

        @Override
        public void onBufferReceived(byte[] buffer){}

        @Override
        public void onEndOfSpeech(){Log.d(TAG, "onEndOfSpeech"); //$NON-NLS-1$
        }
        @Override
        public void onError(int error)
        {
            mIsListening = false;
            Message message = Message.obtain(null, MSG_RECOGNIZER_CANCEL);
            try
            {
                mServerMessenger.send(message);
                message = Message.obtain(null, MSG_RECOGNIZER_START_LISTENING);
                mServerMessenger.send(message);
            }
            catch (RemoteException e){}
            Log.d(TAG, "error = " + error); //$NON-NLS-1$
        }
        @Override
        public void onEvent(int eventType, Bundle params){}
        @Override
        public void onPartialResults(Bundle partialResults)
        {}
        @Override
        public void onReadyForSpeech(Bundle params){Log.d(TAG, "onReadyForSpeech"); //$NON-NLS-1$
        }
        @Override
        public void onResults(Bundle results)
        {
            ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            String[] words = new String[10];
            String text = "";
            for (String result : matches) {
                text += result + " ";
            }
            words = text.split(" ");
            matches = new ArrayList<String>();
            for(int count = 0; count < words.length; count++){
                matches.add(words[count]);
            }
            Toast.makeText(SpeechRecognizerService.this, text , Toast.LENGTH_SHORT).show();
            //Crea l'iteratore
            Iterator<String> it = matches.iterator();
            //Indica l'avvenuta operazione di uno scenario
            Boolean operation = false;
            //Itera l'arraylist dei risultati fino a quando non riconosce una parola
            while (it.hasNext() && !operation){
                String word = it.next().toLowerCase().trim();
                switch (word){
                    case "calibro":{
                        Intent intent=new Intent();
                        intent.setAction("demo.project.hmi.demoapp.msg");
                        intent.putExtra("Action", "Calibra");
                        sendBroadcast(intent);
                        operation = true;
                        break;
                    }
                    //Okay e click: invia un messaggio di broadcast richiedendo un click nella posizione corrente
                    case "ok":{
                        Intent intent=new Intent();
                        intent.setAction("demo.project.hmi.demoapp.msg");
                        if(prefs.getBoolean("SwitchSensore", false))
                            intent.putExtra("Action", "ClickEx");
                        else
                            intent.putExtra("Action", "Click");
                        sendBroadcast(intent);
                        operation = true;
                        break;
                    }
                    //Vedi sopra
                    case "click":{
                        Intent intent=new Intent();
                        intent.setAction("demo.project.hmi.demoapp.msg");
                        if(prefs.getBoolean("SwitchSensore", false))
                            intent.putExtra("Action", "ClickEx");
                        else
                            intent.putExtra("Action", "Click");
                        sendBroadcast(intent);
                        operation = true;
                        break;
                    }
                    case "indietro":{
                        Intent intent=new Intent();
                        intent.setAction("demo.project.hmi.demoapp.msg");
                        intent.putExtra("Action", "Back");
                        sendBroadcast(intent);
                        break;
                    }
                    case "casa":{
                        Intent intent=new Intent();
                        intent.setAction("demo.project.hmi.demoapp.msg");
                        intent.putExtra("Action", "Home");
                        sendBroadcast(intent);
                        break;
                    }
                    //Caso swipe
                    case "sinistra":{
                        Intent intent=new Intent();
                        intent.setAction("demo.project.hmi.demoapp.msg");
                        if(prefs.getBoolean("SwitchSensore", false))
                            intent.putExtra("Action", "SRightEx");
                        else
                            intent.putExtra("Action", "SRight");
                        sendBroadcast(intent);
                        operation = true;
                        break;
                    }
                    case "destra":{
                        Intent intent=new Intent();
                        intent.setAction("demo.project.hmi.demoapp.msg");
                        if(prefs.getBoolean("SwitchSensore", false))
                            intent.putExtra("Action", "SLeftEx");
                        else
                            intent.putExtra("Action", "SLeft");
                        sendBroadcast(intent);
                        operation = true;
                        break;
                    }
                    case "basso":{
                        Intent intent=new Intent();
                        intent.setAction("demo.project.hmi.demoapp.msg");
                        if(prefs.getBoolean("SwitchSensore", false))
                            intent.putExtra("Action", "SUpEx");
                        else
                            intent.putExtra("Action", "SUp");
                        sendBroadcast(intent);
                        operation = true;
                        break;
                    }
                    case "alto":{
                        Intent intent=new Intent();
                        intent.setAction("demo.project.hmi.demoapp.msg");
                        if(prefs.getBoolean("SwitchSensore", false))
                            intent.putExtra("Action", "SDownEx");
                        else
                            intent.putExtra("Action", "SDown");
                        sendBroadcast(intent);
                        operation = true;
                        break;
                    }
                    case "lungo":{
                        Intent intent=new Intent();
                        intent.setAction("demo.project.hmi.demoapp.msg");
                        if(prefs.getBoolean("SwitchSensore", false))
                            intent.putExtra("Action", "LungoEx");
                        else
                            intent.putExtra("Action", "Lungo");
                        sendBroadcast(intent);
                        operation = true;
                        break;
                    }
                }
                //Controllo se è una parola chiave scelta dall'utente
                String clickString = prefs.getString("Click", "");
                if (clickString != ""){
                    String[] clickList = clickString.split(" ");
                    for(String clickKey : clickList){
                        if (clickKey.trim().toLowerCase().equals(word)){
                            Intent intent=new Intent();
                            intent.setAction("demo.project.hmi.demoapp.msg");
                            if(prefs.getBoolean("SwitchSensore", false))
                                intent.putExtra("Action", "ClickEx");
                            else
                                intent.putExtra("Action", "Click");
                            sendBroadcast(intent);
                            operation = true;
                        }
                    }
                }
                String swipeString = prefs.getString("Swipe", "");
                if (swipeString != ""){
                    String[] swipeList = clickString.split(" ");
                    for(String swipeKey : swipeList){
                        if (swipeKey.trim().toLowerCase().equals(word)){
                            if(it.hasNext()){
                                String nextString = it.next().trim().toLowerCase();
                                if((nextString.equals("a") || (nextString.equals("in"))) && it.hasNext())
                                    //controllo se sia la direzione
                                    switch (it.next().trim().toLowerCase()){
                                        case "destra":{
                                            Intent intent=new Intent();
                                            intent.setAction("demo.project.hmi.demoapp.msg");
                                            if(prefs.getBoolean("SwitchSensore", false))
                                                intent.putExtra("Action", "SRightEx");
                                            else
                                                intent.putExtra("Action", "SRight");
                                            sendBroadcast(intent);
                                            operation = true;
                                            break;
                                        }
                                        case "sinistra":{
                                            Intent intent=new Intent();
                                            intent.setAction("demo.project.hmi.demoapp.msg");
                                            if(prefs.getBoolean("SwitchSensore", false))
                                                intent.putExtra("Action", "SLeftEx");
                                            else
                                                intent.putExtra("Action", "SLeft");
                                            sendBroadcast(intent);
                                            operation = true;
                                            break;
                                        }
                                        case "alto":{
                                            Intent intent=new Intent();
                                            intent.setAction("demo.project.hmi.demoapp.msg");
                                            if(prefs.getBoolean("SwitchSensore", false))
                                                intent.putExtra("Action", "SUpEx");
                                            else
                                                intent.putExtra("Action", "SUp");
                                            sendBroadcast(intent);
                                            operation = true;
                                            break;
                                        }
                                        case "basso":{
                                            Intent intent=new Intent();
                                            intent.setAction("demo.project.hmi.demoapp.msg");
                                            if(prefs.getBoolean("SwitchSensore", false))
                                                intent.putExtra("Action", "SDownEx");
                                            else
                                                intent.putExtra("Action", "SDown");
                                            sendBroadcast(intent);
                                            operation = true;
                                            break;
                                        }
                                    }
                            }else{
                                Intent intent=new Intent();
                                intent.setAction("demo.project.hmi.demoapp.msg");
                                if(prefs.getBoolean("SwitchSensore", false))
                                    intent.putExtra("Action", "SDownEx");
                                else
                                    intent.putExtra("Action", "SDown");
                                sendBroadcast(intent);
                                operation = true;
                                break;
                            }
                        }
                    }
                }
            }
            Log.d(TAG, "onResults"); //$NON-NLS-1$
            Message message = Message.obtain(null, MSG_RECOGNIZER_CANCEL);
            try
            {
                mServerMessenger.send(message);
                message = Message.obtain(null, MSG_RECOGNIZER_START_LISTENING);
                mServerMessenger.send(message);
            }
            catch (RemoteException e){}
        }
        @Override
        public void onRmsChanged(float rmsdB){}
    }
    @Override
    public IBinder onBind (Intent intent)
    {
        Log.d(TAG, "onBind");
        return mServerMessenger.getBinder();
    }
}
/*
case "sfoglia":{
                        //Se segue un altra parola controllo che sia "a" seguita da qualsiasi altra parola
                        if(it.hasNext()){
                            String nextString = it.next().trim().toLowerCase();
                            if((nextString.equals("a") || (nextString.equals("in"))) && it.hasNext())
                            //controllo se sia la direzione
                            switch (it.next().trim().toLowerCase()){
                                case "destra":{
                                    Intent intent=new Intent();
                                    intent.setAction("demo.project.hmi.demoapp.msg");
                                    if(prefs.getBoolean("SwitchSensore", false))
                                        intent.putExtra("Action", "SRightEx");
                                    else
                                        intent.putExtra("Action", "SRight");
                                    sendBroadcast(intent);
                                    operation = true;
                                    break;
                                }
                                case "sinistra":{
                                    Intent intent=new Intent();
                                    intent.setAction("demo.project.hmi.demoapp.msg");
                                    if(prefs.getBoolean("SwitchSensore", false))
                                        intent.putExtra("Action", "SLeftEx");
                                    else
                                        intent.putExtra("Action", "SLeft");
                                    sendBroadcast(intent);
                                    operation = true;
                                    break;
                                }
                                case "alto":{
                                    Intent intent=new Intent();
                                    intent.setAction("demo.project.hmi.demoapp.msg");
                                    if(prefs.getBoolean("SwitchSensore", false))
                                        intent.putExtra("Action", "SUpEx");
                                    else
                                        intent.putExtra("Action", "SUp");
                                    sendBroadcast(intent);
                                    operation = true;
                                    break;
                                }
                                case "basso":{
                                    Intent intent=new Intent();
                                    intent.setAction("demo.project.hmi.demoapp.msg");
                                    if(prefs.getBoolean("SwitchSensore", false))
                                        intent.putExtra("Action", "SDownEx");
                                    else
                                        intent.putExtra("Action", "SDown");
                                    sendBroadcast(intent);
                                    operation = true;
                                    break;
                                }
                            }
                        }else{
                            Intent intent=new Intent();
                            intent.setAction("demo.project.hmi.demoapp.msg");
                            if(prefs.getBoolean("SwitchSensore", false))
                                intent.putExtra("Action", "SDownEx");
                            else
                                intent.putExtra("Action", "SDown");
                            sendBroadcast(intent);
                            operation = true;
                            break;
                        }
                        break;
                    }
                }
                //Controllo se è una parola chiave scelta dall'utente
                String clickString = prefs.getString("Click", "");
                if (clickString != ""){
                    String[] clickList = clickString.split(" ");
                    for(String clickKey : clickList){
                        if (clickKey.trim().toLowerCase().equals(word)){
                            Intent intent=new Intent();
                            intent.setAction("demo.project.hmi.demoapp.msg");
                            if(prefs.getBoolean("SwitchSensore", false))
                                intent.putExtra("Action", "ClickEx");
                            else
                                intent.putExtra("Action", "Click");
                            sendBroadcast(intent);
                            operation = true;
                        }
                    }
                }
                String swipeString = prefs.getString("Swipe", "");
                if (swipeString != ""){
                    String[] swipeList = clickString.split(" ");
                    for(String swipeKey : swipeList){
                        if (swipeKey.trim().toLowerCase().equals(word)){
                            if(it.hasNext()){
                                String nextString = it.next().trim().toLowerCase();
                                if((nextString.equals("a") || (nextString.equals("in"))) && it.hasNext())
                                    //controllo se sia la direzione
                                    switch (it.next().trim().toLowerCase()){
                                        case "destra":{
                                            Intent intent=new Intent();
                                            intent.setAction("demo.project.hmi.demoapp.msg");
                                            if(prefs.getBoolean("SwitchSensore", false))
                                                intent.putExtra("Action", "SRightEx");
                                            else
                                                intent.putExtra("Action", "SRight");
                                            sendBroadcast(intent);
                                            operation = true;
                                            break;
                                        }
                                        case "sinistra":{
                                            Intent intent=new Intent();
                                            intent.setAction("demo.project.hmi.demoapp.msg");
                                            if(prefs.getBoolean("SwitchSensore", false))
                                                intent.putExtra("Action", "SLeftEx");
                                            else
                                                intent.putExtra("Action", "SLeft");
                                            sendBroadcast(intent);
                                            operation = true;
                                            break;
                                        }
                                        case "alto":{
                                            Intent intent=new Intent();
                                            intent.setAction("demo.project.hmi.demoapp.msg");
                                            if(prefs.getBoolean("SwitchSensore", false))
                                                intent.putExtra("Action", "SUpEx");
                                            else
                                                intent.putExtra("Action", "SUp");
                                            sendBroadcast(intent);
                                            operation = true;
                                            break;
                                        }
                                        case "basso":{
                                            Intent intent=new Intent();
                                            intent.setAction("demo.project.hmi.demoapp.msg");
                                            if(prefs.getBoolean("SwitchSensore", false))
                                                intent.putExtra("Action", "SDownEx");
                                            else
                                                intent.putExtra("Action", "SDown");
                                            sendBroadcast(intent);
                                            operation = true;
                                            break;
                                        }
                                    }
                            }else{
                                Intent intent=new Intent();
                                intent.setAction("demo.project.hmi.demoapp.msg");
                                if(prefs.getBoolean("SwitchSensore", false))
                                    intent.putExtra("Action", "SDownEx");
                                else
                                    intent.putExtra("Action", "SDown");
                                sendBroadcast(intent);
                                operation = true;
                                break;
                            }
                        }
                    }
                }
 */