package com.almagabi.swipetap;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
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
    private TextView target;
    private WindowManager.LayoutParams targetParams;
    private TextView trigger;
    private WindowManager.LayoutParams triggerParams;
    private final Handler handler = new Handler();

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
        target = new TextView(this);
        target.setText("TARGET");
        target.setTextColor(Color.WHITE);
        target.setTextSize(10);
        target.setGravity(Gravity.CENTER);
        target.setBackgroundColor(Color.rgb(46, 125, 50));
        targetParams = new WindowManager.LayoutParams(
                88, 56, type(), WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        targetParams.gravity = Gravity.TOP | Gravity.START;
        targetParams.x = Math.max(0, savedCoordinate(true) - 44);
        targetParams.y = Math.max(0, savedCoordinate(false) - 28);
        target.setOnTouchListener(new View.OnTouchListener() {
            private float downX;
            private float downY;
            private int initialX;
            private int initialY;

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
                    saveCoordinate(targetParams.x + 44, targetParams.y + 28);
                    return true;
                }
                return event.getAction() == MotionEvent.ACTION_UP;
            }
        });
        windowManager.addView(target, targetParams);

        trigger = new TextView(this);
        trigger.setText("TRIGGER\n(tap/swipe)");
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

        TextView hide = new TextView(this);
        hide.setText("HIDE");
        hide.setTextColor(Color.WHITE);
        hide.setTextSize(10);
        hide.setGravity(Gravity.CENTER);
        hide.setBackgroundColor(Color.DKGRAY);
        WindowManager.LayoutParams hideParams = new WindowManager.LayoutParams(
                72, 48, type(), WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        hideParams.gravity = Gravity.TOP | Gravity.END;
        hideParams.x = 12;
        hideParams.y = 220;
        hide.setOnClickListener(v -> {
            if (trigger != null) trigger.setVisibility(View.GONE);
            v.setVisibility(View.GONE);
        });
        windowManager.addView(hide, hideParams);
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

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                downX = event.getRawX();
                downY = event.getRawY();
                initialX = triggerParams.x;
                initialY = triggerParams.y;
                downAt = System.currentTimeMillis();
                dragging = false;
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
                    saveTrigger(triggerParams.x, triggerParams.y);
                }
                return true;
            }
            if (event.getAction() == MotionEvent.ACTION_UP && !dragging) {
                String gesture = getSharedPreferences("settings", MODE_PRIVATE)
                        .getString("gesture", "Tap");
                if ("Tap".equalsIgnoreCase(gesture)
                        || matchesDirection(event.getRawX() - downX, event.getRawY() - downY)) {
                    SwipeAccessibilityService.requestGesture();
                }
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
