package org.isoron.simpletimer.model;

import android.util.Log;

public class SimpleTimer
{
    private static final String TAG = "TimerView";

    private enum TimerState
    {
        PAUSED, RUNNING
    }

    private long startTime;
    private long totalTime;
    private long remainingTime;
    private TimerState state;

    private SimpleTimerListener listener;

    private static final int DEFAULT_INITIAL_TIME = 5 * 60 * 1000;

    public SimpleTimer()
    {
        remainingTime = totalTime = DEFAULT_INITIAL_TIME;
        state = TimerState.PAUSED;
    }

    public void setListener(SimpleTimerListener listener)
    {
        this.listener = listener;
    }

    public void resume()
    {
        if (isRunning()) return;

        startTime = System.currentTimeMillis();
        startTime -= totalTime - remainingTime;
        state = TimerState.RUNNING;
    }

    public void pause()
    {
        if (isPaused()) return;

        remainingTime = getRemainingTime();
        state = TimerState.PAUSED;
    }

    public boolean isRunning()
    {
        return state == TimerState.RUNNING;
    }

    public boolean isPaused()
    {
        return state == TimerState.PAUSED;
    }

    public void flip()
    {
        if (isRunning()) pause();
        else resume();
    }

    public void reset()
    {
        state = TimerState.PAUSED;
        remainingTime = totalTime;
    }

    public long getRemainingTime()
    {
        if (isPaused())
        {
            return remainingTime;
        }
        else
        {
            long currentTime = System.currentTimeMillis();
            long elapsedTime = currentTime - startTime;
            long answer = totalTime - elapsedTime;

            if (answer >= 0)
            {
                return answer;
            }
            else
            {
                if (listener != null) listener.onTimeout();
                reset();

                return remainingTime;
            }
        }
    }

    public long getTotalTime()
    {
        return totalTime;
    }

    public void setTotalTime(long totalTime)
    {
        this.totalTime = totalTime;
        this.remainingTime = totalTime;
    }

    public long getMillisecondsUntilNextMinute()
    {
        if (!isRunning()) return -1;
        return getRemainingTime() % 60000;
    }

    public void increment(int direction)
    {
        if (isRunning()) return;

        int granularity = 60000;
        if (totalTime + direction <= 3 * 60000) granularity = 10000;

        totalTime = Math.max(0, totalTime + direction * granularity);
        totalTime = (totalTime / granularity) * granularity;
        remainingTime = totalTime;
    }
}
