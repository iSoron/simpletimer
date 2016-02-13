package org.isoron.simpletimer.model;

public class SimpleTimer
{
    private enum TimerState {
        PAUSED, RUNNING
    }

    private long startTime;
    private long totalTime;
    private long remainingTime;
    private TimerState state;

    public static final int DEFAULT_INITIAL_TIME = 5 * 60 * 1000;

    public SimpleTimer()
    {
        startTime = -1;
        remainingTime = totalTime = DEFAULT_INITIAL_TIME;
    }

    public void resume()
    {
        if(isRunning()) return;

        startTime = System.currentTimeMillis();
        state = TimerState.RUNNING;
    }

    public void pause()
    {
        if(isPaused()) return;
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

    public void reset()
    {
        state = TimerState.PAUSED;
        remainingTime = totalTime;
    }

    public long getRemainingTime()
    {
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - startTime;

        return totalTime - elapsedTime;
    }
}
