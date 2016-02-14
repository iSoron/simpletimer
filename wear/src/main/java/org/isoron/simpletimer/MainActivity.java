package org.isoron.simpletimer;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.AlarmClock;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;

import org.isoron.base.AmbientModeListener;
import org.isoron.simpletimer.model.SimpleTimer;
import org.isoron.simpletimer.views.TimerView;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends WearableActivity
{

    private static final String TAG = "MainActivity";
    public static final String PREFS_NAME = "SimpleTimerPrefs";
    public static final int DEFAULT_INITIAL_TIME = 5 * 60 * 1000;

    AmbientModeListener ambientModeListener = null;
    TimerView timerView;

    private AlarmManager alarmManager;
    private PendingIntent ambientModePendingIntent;
    private Timer fixedRateTimer;
    private SimpleTimer stimer;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setAmbientEnabled();

        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent ambientModeIntent = new Intent(getApplicationContext(), MainActivity.class);
        ambientModeIntent.setAction("REFRESH");
        ambientModePendingIntent = PendingIntent
                .getActivity(getApplicationContext(), 0, ambientModeIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);

        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, 0);
        final long initialTime = preferences.getLong("initialTime", DEFAULT_INITIAL_TIME);

        stimer = new SimpleTimer();
        stimer.setTotalTime(initialTime);

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener()
        {
            @Override
            public void onLayoutInflated(WatchViewStub stub)
            {
                timerView = (TimerView) findViewById(R.id.timerview);
                timerView.setTimer(stimer);
                stimer.setListener(timerView);
                setAmbientModeListener(timerView);
                startFixedRateTimer();
                refreshViews();
            }
        });

        executeIntent();
    }

    private void executeIntent()
    {
        Intent intent = getIntent();

        if(intent == null) return;
        if(!intent.getAction().equals(AlarmClock.ACTION_SET_TIMER)) return;

        Integer seconds = intent.getIntExtra(AlarmClock.EXTRA_LENGTH, -1);
        if(seconds < 0) return;

        stimer.setTotalTime(seconds * 1000);
        stimer.resume();
    }

    private void startFixedRateTimer()
    {
        if (fixedRateTimer != null) fixedRateTimer.cancel();

        fixedRateTimer = new Timer();
        fixedRateTimer.scheduleAtFixedRate(new TimerTask()
        {
            @Override
            public void run()
            {
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        refreshViews();
                    }
                });
            }
        }, 0, 1000);
    }

    private void stopFixedRateTimer()
    {
        if (fixedRateTimer != null) fixedRateTimer.cancel();
    }

    private void refreshViews()
    {
        if (timerView != null)
        {
            timerView.invalidate();
        }

        if (isAmbient()) scheduleNextRefresh();
    }

    private void scheduleNextRefresh()
    {
        long delay = stimer.getMillisecondsUntilNextMinute();
        if (delay < 0) return;

        Log.d(TAG, "sleeping for " + delay + " milliseconds (" + delay / 1000 / 60.0 +
                " minutes)");
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + delay + 100,
                ambientModePendingIntent);
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails)
    {
        super.onEnterAmbient(ambientDetails);
        if (ambientModeListener != null) ambientModeListener.onEnterAmbient(ambientDetails);

        stopFixedRateTimer();

        refreshViews();
    }

    @Override
    public void onExitAmbient()
    {
        super.onExitAmbient();
        if (ambientModeListener != null) ambientModeListener.onExitAmbient();

        startFixedRateTimer();

        refreshViews();
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

        refreshViews();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        Log.d(TAG, "onPause()");

        stopFixedRateTimer();

        refreshViews();
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        stopFixedRateTimer();

        savePreferences();

        if (!isAmbient()) finishAffinity();

        Log.d(TAG, "onStop()");
    }

    private void savePreferences()
    {
        long totalTime = stimer.getTotalTime();
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong("initialTime", totalTime);
        editor.commit();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        Log.d(TAG, "onResume()");
        if (!isAmbient()) startFixedRateTimer();
    }

    @Override
    protected void onRestart()
    {
        super.onRestart();
        Log.d(TAG, "onRestart()");
    }
}
