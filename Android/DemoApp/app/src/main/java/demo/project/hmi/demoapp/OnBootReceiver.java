package demo.project.hmi.demoapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by Matteo on 15/05/2016.
 */
public class OnBootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        //Lancio i servizi
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        //Controllo se ho attivato l'autoboot dal menù
        if(prefs.getBoolean("AutoBoot", true)) {
            if (prefs.getBoolean("SwitchSensore", false)) {
                prefs.edit().putBoolean("OnBootEx", true).commit();
                prefs.edit().putBoolean("ExIsOn", true).commit();
            } else {
                prefs.edit().putBoolean("OnBootIn", true).commit();
                prefs.edit().putBoolean("InIsOn", true).commit();
            }
            context.startService(new Intent(context, InternalCursorService.class));
            context.startService(new Intent(context, ExternalCursorService.class));
            context.startService(new Intent(context, OverlayService.class));
        }
    }
}
