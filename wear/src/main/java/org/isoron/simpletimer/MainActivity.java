package org.isoron.simpletimer;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;

import org.isoron.base.AmbientModeListener;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends WearableActivity
{

    private static final String TAG = "MainActivity";
    AmbientModeListener ambientModeListener = null;
    TimerView timerView;

    private AlarmManager ambientModeAlarmManager;
    private PendingIntent ambientModePendingIntent;
    private Timer timer;
    private boolean isActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setAmbientEnabled();

        ambientModeAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        Intent ambientModeIntent = new Intent(getApplicationContext(), MainActivity.class);
        ambientModeIntent.setAction("REFRESH");

        ambientModePendingIntent =
                PendingIntent.getActivity(getApplicationContext(), 0, ambientModeIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener()
        {
            @Override
            public void onLayoutInflated(WatchViewStub stub)
            {
                timerView = (TimerView) findViewById(R.id.timerview);
                setAmbientModeListener(timerView);

                startTimer();
                refresh();
            }
        });
    }

    private void startTimer()
    {
        if (timer != null) timer.cancel();
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask()
        {
            @Override
            public void run()
            {
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        refresh();
                    }
                });
            }
        }, 0, 1000);
    }

    private void stopTimer()
    {
        if (timer != null) timer.cancel();
    }

    private void refresh()
    {
        Log.d(TAG, "refresh()  ambient? " + isAmbient() + "  active? " + isActive);

        if (timerView != null)
        {
            timerView.tick();
            timerView.invalidate();
        }

        if (isAmbient() || !isActive)
        {
            long delay = -1;
            if (timerView != null)
            {
                if (isAmbient()) delay = timerView.getMillisecondsUntilNextMinute();
                else delay = timerView.getRemainingTime();
            }

            if (delay > 0)
            {
                Log.d(TAG, "sleeping for " + delay + " milliseconds (" + delay / 1000 / 60.0 + " minutes)");
                ambientModeAlarmManager.setExact(AlarmManager.RTC_WAKEUP,
                        System.currentTimeMillis() + delay + 100, ambientModePendingIntent);
            }
        }
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails)
    {
        super.onEnterAmbient(ambientDetails);
        if (ambientModeListener != null) ambientModeListener.onEnterAmbient(ambientDetails);
        stopTimer();
        refresh();
    }

    @Override
    public void onExitAmbient()
    {
        super.onExitAmbient();
        if (ambientModeListener != null) ambientModeListener.onExitAmbient();
        startTimer();
        refresh();
    }

    @Override
    public void onUpdateAmbient()
    {
        Log.d(TAG, "onUpdateAmbient()");
        super.onUpdateAmbient();
        if (ambientModeListener != null) ambientModeListener.onUpdateAmbient();
    }

    public void setAmbientModeListener(AmbientModeListener ambientModeListener)
    {
        this.ambientModeListener = ambientModeListener;
    }

    @Override
    protected void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);
        setIntent(intent);

        Log.d(TAG, "onNewIntent: " + intent.getAction());

        refresh();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        Log.d(TAG, "onPause()");
        isActive = false;
        stopTimer();
        refresh();
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        Log.d(TAG, "onStop()");
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        Log.d(TAG, "onResume()");

        isActive = true;
        if(!isAmbient()) startTimer();
        refresh();
    }

    @Override
    protected void onRestart()
    {
        super.onRestart();
        Log.d(TAG, "onRestart()");
    }
}
