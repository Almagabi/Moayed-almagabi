package com.almagabi.swipetap;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.View;
import android.widget.TextView;
import android.view.MotionEvent;
import android.os.Handler;
import android.content.res.Configuration;

public class FloatingButtonService extends Service {
    private WindowManager windowManager;
    private View target;
    private WindowManager.LayoutParams targetParams;
    private TextView trigger;
    private TextView close;
    private WindowManager.LayoutParams triggerParams;
    private WindowManager.LayoutParams closeParams;
    private final Handler handler = new Handler();
    private final Runnable hideTarget = () -> {
        if (target != null) target.setVisibility(View.GONE);
    };

    @Override
    public void onCreate() {
        super.onCreate();
        getSharedPreferences("settings", MODE_PRIVATE).edit().putBoolean("running", true).apply();
        if (!Settings.canDrawOverlays(this)) {
            getSharedPreferences("settings", MODE_PRIVATE).edit().putBoolean("running", false).apply();
            stopSelf();
            return;
        }
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        target = new View(this) {
            private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

            @Override
            protected void onDraw(Canvas canvas) {
                float center = getWidth() / 2f;
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(4);
                paint.setColor(Color.rgb(46, 125, 50));
                canvas.drawCircle(center, getHeight() / 2f, center - 5, paint);
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.RED);
                canvas.drawCircle(center, getHeight() / 2f, 7, paint);
            }
        };
        target.setAlpha(getSharedPreferences("settings", MODE_PRIVATE)
                .getInt("target_opacity", 100) / 100f);
        int targetSize = getSharedPreferences("settings", MODE_PRIVATE).getInt("target_size", 64);
        targetParams = new WindowManager.LayoutParams(
                targetSize, targetSize, type(), WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        targetParams.gravity = Gravity.TOP | Gravity.START;
        targetParams.x = Math.max(0, savedCoordinate(true) - targetSize / 2);
        targetParams.y = Math.max(0, savedCoordinate(false) - targetSize / 2);
        target.setOnTouchListener(new View.OnTouchListener() {
            private float downX;
            private float downY;
            private int initialX;
            private int initialY;
            private long lastTap;

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    downX = event.getRawX();
                    downY = event.getRawY();
                    initialX = targetParams.x;
                    initialY = targetParams.y;
                    return true;
                }
                if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    targetParams.x = initialX + Math.round(event.getRawX() - downX);
                    targetParams.y = initialY + Math.round(event.getRawY() - downY);
                    targetParams.x = Math.max(0, targetParams.x);
                    targetParams.y = Math.max(0, targetParams.y);
                    windowManager.updateViewLayout(target, targetParams);
                    int center = targetParams.width / 2;
                    saveCoordinate(targetParams.x + center, targetParams.y + center);
                    return true;
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    long now = System.currentTimeMillis();
                    if (now - lastTap < 350) {
                        target.setVisibility(target.getVisibility() == View.VISIBLE
                                ? View.GONE : View.VISIBLE);
                        if (target.getVisibility() == View.VISIBLE) scheduleTargetHide();
                    } else {
                        scheduleTargetHide();
                    }
                    lastTap = now;
                    return true;
                }
                return false;
            }
        });
        windowManager.addView(target, targetParams);

        trigger = new TextView(this);
        String customIcon = getSharedPreferences("settings", MODE_PRIVATE)
                .getString("custom_icon", "");
        String icon = customIcon.isEmpty()
                ? getSharedPreferences("settings", MODE_PRIVATE).getString("icon", "●")
                : customIcon;
        trigger.setText(icon);
        trigger.setTextColor(Color.WHITE);
        trigger.setTextSize(10);
        trigger.setGravity(Gravity.CENTER);
        trigger.setBackgroundColor(Color.rgb(123, 31, 162));
        triggerParams = new WindowManager.LayoutParams(
                150, 100, type(), WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        triggerParams.gravity = Gravity.TOP | Gravity.START;
        triggerParams.x = savedTrigger(true);
        triggerParams.y = savedTrigger(false);
        trigger.setOnTouchListener(new TriggerTouchListener());
        windowManager.addView(trigger, triggerParams);

        close = new TextView(this);
        close.setText("X");
        close.setTextColor(Color.WHITE);
        close.setTextSize(14);
        close.setGravity(Gravity.CENTER);
        close.setBackgroundColor(Color.DKGRAY);
        closeParams = new WindowManager.LayoutParams(
                40, 40, type(), WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        closeParams.gravity = Gravity.TOP | Gravity.START;
        updateClosePosition();
        close.setOnClickListener(v -> stopSelf());
        windowManager.addView(close, closeParams);
        scheduleTargetHide();
    }

    @Override
    public void onDestroy() {
        getSharedPreferences("settings", MODE_PRIVATE).edit().putBoolean("running", false).apply();
        if (target != null && windowManager != null) {
            windowManager.removeView(target);
        }
        if (trigger != null && windowManager != null) {
            windowManager.removeView(trigger);
        }
        if (close != null && windowManager != null) {
            windowManager.removeView(close);
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private int type() {
        return WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
    }

    private int savedCoordinate(boolean x) {
        boolean landscape = getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;
        String key = landscape
                ? (x ? "landscape_x" : "landscape_y")
                : (x ? "portrait_x" : "portrait_y");
        return getSharedPreferences("settings", MODE_PRIVATE).getInt(key,
                landscape ? (x ? 806 : 540) : (x ? 360 : 1200));
    }

    private void saveCoordinate(int x, int y) {
        boolean landscape = getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;
        getSharedPreferences("settings", MODE_PRIVATE).edit()
                .putInt(landscape ? "landscape_x" : "portrait_x", x)
                .putInt(landscape ? "landscape_y" : "portrait_y", y)
                .apply();
    }

    private void updateClosePosition() {
        closeParams.x = triggerParams.x + triggerParams.width - 18;
        closeParams.y = triggerParams.y - 18;
        if (close != null && windowManager != null) {
            windowManager.updateViewLayout(close, closeParams);
        }
    }

    private void scheduleTargetHide() {
        handler.removeCallbacks(hideTarget);
        handler.postDelayed(hideTarget, 4000);
    }

    private void showTarget() {
        target.setVisibility(View.VISIBLE);
        scheduleTargetHide();
    }

    private int savedTrigger(boolean x) {
        boolean landscape = getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;
        String key = landscape ? (x ? "trigger_landscape_x" : "trigger_landscape_y")
                : (x ? "trigger_portrait_x" : "trigger_portrait_y");
        return getSharedPreferences("settings", MODE_PRIVATE).getInt(key, x ? 60 : 500);
    }

    private void saveTrigger(int x, int y) {
        boolean landscape = getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;
        getSharedPreferences("settings", MODE_PRIVATE).edit()
                .putInt(landscape ? "trigger_landscape_x" : "trigger_portrait_x", x)
                .putInt(landscape ? "trigger_landscape_y" : "trigger_portrait_y", y)
                .apply();
    }

    private final class TriggerTouchListener implements View.OnTouchListener {
        private float downX;
        private float downY;
        private int initialX;
        private int initialY;
        private boolean dragging;
        private long downAt;
        private long lastTap;

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                downX = event.getRawX();
                downY = event.getRawY();
                initialX = triggerParams.x;
                initialY = triggerParams.y;
                downAt = System.currentTimeMillis();
                dragging = false;
                handler.postDelayed(() -> {
                    if (downAt != 0 && !dragging) showTarget();
                }, 2000);
                return true;
            }
            if (event.getAction() == MotionEvent.ACTION_MOVE) {
                if (!dragging && System.currentTimeMillis() - downAt > 600) {
                    dragging = true;
                }
                if (dragging) {
                    triggerParams.x = Math.max(0, initialX + Math.round(event.getRawX() - downX));
                    triggerParams.y = Math.max(0, initialY + Math.round(event.getRawY() - downY));
                    windowManager.updateViewLayout(trigger, triggerParams);
                    updateClosePosition();
                    saveTrigger(triggerParams.x, triggerParams.y);
                }
                return true;
            }
            if (event.getAction() == MotionEvent.ACTION_UP && !dragging) {
                handler.removeCallbacksAndMessages(null);
                long now = System.currentTimeMillis();
                if (now - lastTap < 350) {
                    if (target.getVisibility() == View.VISIBLE) {
                        target.setVisibility(View.GONE);
                    } else {
                        showTarget();
                    }
                    lastTap = 0;
                    return true;
                }
                lastTap = now;
                String gesture = getSharedPreferences("settings", MODE_PRIVATE)
                        .getString("gesture", "Tap");
                if ("Tap".equalsIgnoreCase(gesture)
                        || matchesDirection(event.getRawX() - downX, event.getRawY() - downY)) {
                    SwipeAccessibilityService.requestGesture();
                }
                scheduleTargetHide();
            }
            return true;
        }

        private boolean matchesDirection(float dx, float dy) {
            if (Math.abs(dx) < 30 && Math.abs(dy) < 30) return false;
            String direction = getSharedPreferences("settings", MODE_PRIVATE)
                    .getString("direction", "Down");
            if ("Up".equalsIgnoreCase(direction)) return dy < -30;
            if ("Left".equalsIgnoreCase(direction)) return dx < -30;
            if ("Right".equalsIgnoreCase(direction)) return dx > 30;
            return dy > 30;
        }
    }
}
