package org.isoron.simpletimer;

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
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import org.isoron.base.AmbientModeListener;

import java.util.Timer;

public class TimerView extends View implements AmbientModeListener
{
    private static final String TAG = "TimerView";

    private final int primaryColor;
    private final int secondaryColor;
    private final int tertiaryColor;

    private final int backgroundColor;

    private Paint paint;
    private Paint paintInteractive;
    private Paint paintAmbient;
    private Paint pClockInteractive;

    private int width;
    private int height;
    private int fontHeight;
    private int size;
    private Timer timer;
    private final Activity activity;
    private Vibrator vibrator;

    private int step;
    private boolean isRunning;
    private long totalTime;
    private long remainingTime;
    private long lastTick;

    private int brightnessCountdown;
    private boolean isBright = false;

    private boolean hasLongPressed;
    private boolean hasMoved = false;
    private boolean ambientMode = false;

    private final int DEFAULT_TIME = 5 * 60 * 1000;
    private final int GRANULARITY = 60 * 1000;
    private final int BRIGHTNESS_LENGTH = 5;
    private final long VIBRATION_FINISH[] = {0, 250, 250, 250, 250, 250, 250, 250, 250, 250, 250};

    private RectF screenRect;

    public TimerView(Context ctx, AttributeSet attrs)
    {
        super(ctx, attrs);
        this.activity = (Activity) ctx;

        step = 0;
        isRunning = false;
        totalTime = DEFAULT_TIME;
        remainingTime = totalTime;

        hasMoved = false;
        hasLongPressed = false;

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
        paintAmbient.setAntiAlias(false);
        paintAmbient.setTextAlign(Paint.Align.CENTER);

        primaryColor = Color.parseColor("#0288d1");
        secondaryColor = Color.WHITE;
        tertiaryColor = mixColors(primaryColor, Color.BLACK, 0.37f);
        backgroundColor = Color.BLACK;

        vibrator = (Vibrator) activity.getSystemService(Activity.VIBRATOR_SERVICE);

        setLongClickable(true);
        setOnLongClickListener(new View.OnLongClickListener()
        {
            @Override
            public boolean onLongClick(View v)
            {
                if (hasMoved) return false;
                highBrightness();

                remainingTime = totalTime;
                vibrator.vibrate(250);
                hasLongPressed = true;
                isRunning = false;
                invalidate();

                return true;
            }
        });

        setOnTouchListener(new View.OnTouchListener()
        {
            private float prevY;
            private long prevTime;

            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                int box = 20;
                highBrightness();

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
        });

        highBrightness();
    }

    public void tick()
    {
        Log.d(TAG, "tick()");
        long currentTime = System.currentTimeMillis();

        step = (step + 1) % 2;

        if (brightnessCountdown-- == 0) lowBrightness();

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
                PowerManager.WakeLock mWakeLock = powerManager.newWakeLock(
                        (PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.FULL_WAKE_LOCK |
                                PowerManager.ACQUIRE_CAUSES_WAKEUP), "MyWakelockTag");
                mWakeLock.acquire();

                highBrightness();
            }
        }

        lastTick = currentTime;
    }

    public long getMillisecondsUntilNextMinute()
    {
        if (!isRunning) return -1;
        return remainingTime % 60000;
    }

    public long getRemainingTime()
    {
        if(!isRunning) return -1;
        return remainingTime;
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
//        Log.d(TAG, "onDraw()");
        paint = paintInteractive;

        if (ambientMode) paint = paintAmbient;

        clearBackground(canvas);
        drawOuterRing(canvas);
        drawInnerRing(canvas);
        drawTimer(canvas);
        drawCurrentTime(canvas);
    }

    private void drawCurrentTime(Canvas canvas)
    {
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
                    screenRect.centerY() + (int) (fontHeight * 0.3), paint);
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
                        screenRect.centerY() + (int) (fontHeight * 0.3), paint);

                paint.setTextSize(size * 0.15f);
                paint.setColor(secondaryColor);
                canvas.drawText(String.format("%02d", seconds),
                        screenRect.centerX() + minutesWidth + secondsWidth / 2 + size * 0.025f -
                                totalWidth / 2, screenRect.centerY() + (int) (fontHeight * 0.3),
                        paint);
            }
        }

        paint.setTextSize(size * 0.08f);
        if (!ambientMode) paint.setColor(secondaryColor);

        String text = "minutes";
        if (minutes / 60 == 1) text = "minute";

        canvas.drawText(text, screenRect.centerX(), screenRect.centerY() + (int) (fontHeight * 1.0),
                paint);
    }

    private void drawInnerRing(Canvas canvas)
    {
        int totalPieces = (int) Math.min(16, totalTime / 60000);
        int remainingPieces = (int) Math.ceil(remainingTime / 60000.0);

        float pieceAngle = 360.0f / totalPieces;
        float gap = 1.0f;

        RectF r = new RectF(screenRect);
        r.inset(size * 0.1f, size * 0.1f);

        if (ambientMode) paint.setColor(Color.WHITE);
        else paint.setColor(primaryColor);

        for (int i = 0; i < remainingPieces; i++)
            canvas.drawArc(r, -90 - (i + 1) * pieceAngle, pieceAngle - gap, true, paint);

//        canvas.drawArc(r, -90.0f, -360.0f * (remainingTime / 1000) / (totalTime / 1000), true, paint);

        r.inset(size * 0.015f, size * 0.015f);
        paint.setColor(backgroundColor);
        canvas.drawArc(r, 0, 360, true, paint);
    }

    private void drawOuterRing(Canvas canvas)
    {
        if (ambientMode) return;

        int totalPieces = 60 / 5;
        float remainingPercentage = (remainingTime / 1000 % 60) / 60.0f;
        int remainingPieces = (int) Math.ceil(remainingPercentage * totalPieces);

        if (remainingPieces == 0) remainingPieces = totalPieces;

        float pieceAngle = 360.0f / totalPieces;
        float gap = 0.5f;

        RectF r = new RectF(screenRect);
        r.inset(size * 0.075f, size * 0.075f);

        paint.setColor(tertiaryColor);
        for (int i = 0; i < remainingPieces; i++)
            canvas.drawArc(r, -90 - (i + 1) * pieceAngle, pieceAngle - gap, true, paint);

        r.inset(size * 0.015f, size * 0.015f);
        paint.setColor(backgroundColor);
        canvas.drawArc(r, 0, 360, true, paint);
    }

    private void clearBackground(Canvas canvas)
    {
        paint.setColor(backgroundColor);
        canvas.drawRect(screenRect, paint);
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails)
    {
        ambientMode = true;
        Log.d(TAG, "onEnterAmbient()");
    }

    @Override
    public void onExitAmbient()
    {
        ambientMode = false;
        highBrightness();
        Log.d(TAG, "onExitAmbient()");
    }

    @Override
    public void onUpdateAmbient()
    {
        Log.d(TAG, "onUpdateAmbient()");
    }

    public static int mixColors(int color1, int color2, float amount)
    {
        final byte ALPHA_CHANNEL = 24;
        final byte RED_CHANNEL = 16;
        final byte GREEN_CHANNEL = 8;
        final byte BLUE_CHANNEL = 0;

        final float inverseAmount = 1.0f - amount;

        int a = ((int) (((float) (color1 >> ALPHA_CHANNEL & 0xff) * amount) +
                ((float) (color2 >> ALPHA_CHANNEL & 0xff) * inverseAmount))) & 0xff;
        int r = ((int) (((float) (color1 >> RED_CHANNEL & 0xff) * amount) +
                ((float) (color2 >> RED_CHANNEL & 0xff) * inverseAmount))) & 0xff;
        int g = ((int) (((float) (color1 >> GREEN_CHANNEL & 0xff) * amount) +
                ((float) (color2 >> GREEN_CHANNEL & 0xff) * inverseAmount))) & 0xff;
        int b = ((int) (((float) (color1 & 0xff) * amount) +
                ((float) (color2 & 0xff) * inverseAmount))) & 0xff;

        return a << ALPHA_CHANNEL | r << RED_CHANNEL | g << GREEN_CHANNEL | b << BLUE_CHANNEL;
    }

    public void changeBrightness(float brightness)
    {
        WindowManager.LayoutParams layout = activity.getWindow().getAttributes();
        layout.screenBrightness = brightness;
        activity.getWindow().setAttributes(layout);
    }

    public void lowBrightness()
    {
        if (!isBright) return;

        Log.d(TAG, "lowBrightness()");
        changeBrightness(0F);
        isBright = false;
    }

    public void highBrightness()
    {
        brightnessCountdown = BRIGHTNESS_LENGTH;
        if (isBright) return;

        Log.d(TAG, "highBrightness()");
        changeBrightness(0.8F);
        isBright = true;
    }
}
