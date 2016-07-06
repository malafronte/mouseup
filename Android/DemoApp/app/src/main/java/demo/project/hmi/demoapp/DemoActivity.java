package demo.project.hmi.demoapp;
//Ottimizzata
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

/**
 * Created by Matteo on 04/03/2016.
 */
public class DemoActivity extends Activity {
    @Override
    //Carico il layout della demo
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.demo_layout);
    }
}
