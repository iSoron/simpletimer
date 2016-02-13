package org.isoron.simpletimer.views;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.Vibrator;
import android.support.wearable.activity.WearableActivity;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import org.isoron.base.AmbientModeListener;
import org.isoron.base.ColorHelper;
import org.isoron.simpletimer.MainActivity;
import org.isoron.simpletimer.model.SimpleTimer;

import java.util.Calendar;


public class TimerView extends View implements AmbientModeListener
{
    private static final String TAG = "TimerView";

    private int primaryColor;
    private int secondaryColor;
    private int tertiaryColor;

    private int backgroundColor;

    private Paint paint;
    private Paint paintInteractive;
    private Paint paintAmbient;
    private Paint pClockInteractive;

    private int width;
    private int height;
    private int fontHeight;
    private int size;
    private final WearableActivity activity;
    private Vibrator vibrator;

    private int step;
    private boolean isRunning;
    private long totalTime;
    private long remainingTime;
    private long lastTick;

    private boolean hasLongPressed;
    private boolean hasMoved = false;
    private boolean ambientMode = false;

    private final int GRANULARITY = 60 * 1000;
    private final long VIBRATION_FINISH[] = {0, 250, 250, 250, 250, 250, 250, 250, 250, 250, 250};

    private RectF screenRect;

    private SimpleTimer stimer;

    public TimerView(Context ctx, AttributeSet attrs)
    {
        super(ctx, attrs);
        this.activity = (WearableActivity) ctx;

        step = 0;
        totalTime = MainActivity.DEFAULT_INITIAL_TIME;
        remainingTime = totalTime;

        isRunning = false;
        hasMoved = false;
        hasLongPressed = false;
        stimer = null;

        initializePaints();
        initializeColors();

        vibrator = (Vibrator) activity.getSystemService(Activity.VIBRATOR_SERVICE);

        setLongClickable(true);
        setOnTouchListener(new TouchListener());
        setOnLongClickListener(new LongClickListener());
    }

    public void setTimer(SimpleTimer stimer)
    {
        this.stimer = stimer;
    }

    private void initializeColors()
    {
        primaryColor = Color.parseColor("#0288d1");
        secondaryColor = Color.WHITE;
        tertiaryColor = ColorHelper.mixColors(primaryColor, Color.BLACK, 0.37f);
        backgroundColor = Color.BLACK;
    }

    private void initializePaints()
    {
        paintInteractive = new Paint();
        paintInteractive.setColor(Color.parseColor("#B2FF59"));
        paintInteractive.setStyle(Paint.Style.FILL);
        paintInteractive.setAntiAlias(true);
        paintInteractive.setTextAlign(Paint.Align.CENTER);

        pClockInteractive = new Paint();
        pClockInteractive.setColor(Color.parseColor("#9E9E9E"));
        pClockInteractive.setStyle(Paint.Style.FILL);
        pClockInteractive.setAntiAlias(true);
        pClockInteractive.setTextAlign(Paint.Align.CENTER);

        paintAmbient = new Paint();
        paintAmbient.setColor(Color.WHITE);
        paintAmbient.setStyle(Paint.Style.FILL_AND_STROKE);
        paintAmbient.setAntiAlias(true);
        paintAmbient.setTextAlign(Paint.Align.CENTER);
    }

    public void tick()
    {
        long currentTime = System.currentTimeMillis();

        step = (step + 1) % 2;

        if (remainingTime <= 0)
        {
            isRunning = false;
        }

        if (isRunning)
        {
            remainingTime -= (currentTime - lastTick);

            if (remainingTime <= 0)
            {
                step = 1;
                isRunning = false;
                remainingTime = totalTime;
                vibrator.vibrate(VIBRATION_FINISH, -1);

                PowerManager powerManager =
                        (PowerManager) activity.getSystemService(Activity.POWER_SERVICE);
                PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
                        (PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.FULL_WAKE_LOCK |
                                PowerManager.ACQUIRE_CAUSES_WAKEUP), "MyWakelockTag");
                wakeLock.acquire(500);
            }
        }

        lastTick = currentTime;
    }

    public long getMillisecondsUntilNextMinute()
    {
        if (!isRunning) return -1;
        return remainingTime % 60000;
    }

    public long getTime()
    {
        return totalTime;
    }

    public void setTime(long totalTime)
    {
        this.totalTime = totalTime;
        this.remainingTime = totalTime;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh)
    {
        width = w;
        height = h;
        size = Math.min(width, height);
        paintInteractive.setTextSize(size * 0.2f);
        paintAmbient.setTextSize(size * 0.15f);
        pClockInteractive.setTextSize(size * 0.08f);

        Rect bounds = new Rect();
        paintInteractive.getTextBounds("00:00", 0, 1, bounds);
        fontHeight = bounds.height();

        screenRect = new RectF(0, 0, width, height);
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        paint = paintInteractive;
        if (ambientMode) paint = paintAmbient;

        clearBackground(canvas);
        drawTimer(canvas);
        drawCurrentTime(canvas);
    }

    private void drawCurrentTime(Canvas canvas)
    {
        if (ambientMode) paint.setColor(Color.WHITE);
        else paint.setColor(primaryColor);

        Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minutes = c.get(Calendar.MINUTE);

        if (ambientMode)
        {
            paint.setColor(Color.WHITE);
        }

        paint.setTextSize(size * 0.08f);
        canvas.drawText(String.format("%02d:%02d", hour, minutes), screenRect.centerX(),
                screenRect.centerY() + (int) (fontHeight * 2.0), paint);

    }

    private void drawTimer(Canvas canvas)
    {
        if (ambientMode) paint.setColor(Color.WHITE);
        else paint.setColor(primaryColor);
        long minutes;

        if (ambientMode)
        {
            minutes = (long) (60 * Math.ceil(remainingTime / 1000 / 60.0));
            paint.setTextSize(size * 0.25f);
            canvas.drawText(String.format("%d", minutes / 60), screenRect.centerX(),
                    screenRect.centerY() + (int) (fontHeight * 0), paint);
        }
        else
        {
            minutes = (long) (60 * Math.floor(remainingTime / 1000 / 60.0));
            long seconds = remainingTime / 1000 % 60;

            if (isRunning || step == 1)
            {
                paint.setTextSize(size * 0.25f);
                float minutesWidth = paint.measureText(String.format("%d", minutes / 60));

                paint.setTextSize(size * 0.15f);
                float secondsWidth = paint.measureText(String.format("%02d", minutes / 60));
                float totalWidth = minutesWidth + secondsWidth;

                paint.setTextSize(size * 0.25f);
                canvas.drawText(String.format("%d", minutes / 60),
                        screenRect.centerX() + minutesWidth / 2 - totalWidth / 2,
                        screenRect.centerY() + (int) (fontHeight * 0), paint);

                paint.setTextSize(size * 0.15f);
                paint.setColor(secondaryColor);
                canvas.drawText(String.format("%02d", seconds),
                        screenRect.centerX() + minutesWidth + secondsWidth / 2 + size * 0.025f -
                                totalWidth / 2, screenRect.centerY() + (int) (fontHeight * 0),
                        paint);
            }
        }

        paint.setTextSize(size * 0.08f);
        if (!ambientMode) paint.setColor(secondaryColor);

        String text = "minutes";
        if (minutes / 60 == 1) text = "minute";

        canvas.drawText(text, screenRect.centerX(), screenRect.centerY() + (int) (fontHeight * 0.7),
                paint);
    }

    private void clearBackground(Canvas canvas)
    {
        paint.setColor(backgroundColor);
        canvas.drawRect(screenRect, paint);
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails)
    {
        if (!isRunning) activity.finishAffinity();

        ambientMode = true;
        Log.d(TAG, "onEnterAmbient()");
    }

    @Override
    public void onExitAmbient()
    {
        ambientMode = false;
        Log.d(TAG, "onExitAmbient()");
    }

    @Override
    public void onUpdateAmbient()
    {
        Log.d(TAG, "onUpdateAmbient()");
    }

    class TouchListener implements View.OnTouchListener
    {
        private float prevY;
        private long prevTime;

        @Override
        public boolean onTouch(View v, MotionEvent event)
        {
            int box = 20;

            switch (event.getAction() & MotionEvent.ACTION_MASK)
            {
                case MotionEvent.ACTION_DOWN:
                    prevY = event.getY();
                    prevTime = remainingTime;
                    hasMoved = false;
                    hasLongPressed = false;
                    break;

                case MotionEvent.ACTION_MOVE:
                    float dy = (event.getY() - prevY) / box;
                    if (Math.abs(dy) < 1) break;
                    hasMoved = true;

                    if (isRunning) break;

                    totalTime = Math.max(GRANULARITY, prevTime - (long) dy * GRANULARITY);
                    totalTime = (totalTime / GRANULARITY) * GRANULARITY;
                    remainingTime = totalTime;

                    step = 1;
                    invalidate();
                    break;

                case MotionEvent.ACTION_UP:
                    if (hasMoved) break;
                    if (hasLongPressed) break;

                    isRunning = !isRunning;

                    vibrator.vibrate(80);
                    invalidate();
                    break;
            }

            return false;
        }
    }

    class LongClickListener implements View.OnLongClickListener
    {
        @Override
        public boolean onLongClick(View v)
        {
            if (hasMoved) return false;

            remainingTime = totalTime;
            isRunning = false;

            vibrator.vibrate(250);
            hasLongPressed = true;

            invalidate();

            return true;
        }
    }

}
