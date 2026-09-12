package com.almagabi.swipetap;

import android.app.Service;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

public class FloatingButtonService extends Service {
    private WindowManager manager;
    private View target;
    private TextView trigger;
    private TextView close;
    private WindowManager.LayoutParams targetParams;
    private WindowManager.LayoutParams triggerParams;
    private WindowManager.LayoutParams closeParams;
    private final Handler handler = new Handler();
    private long lastTriggerTap;
    private final Runnable hideTrigger = () -> {
        trigger.setVisibility(View.GONE);
        close.setVisibility(View.GONE);
    };

    @Override public void onCreate() {
        super.onCreate();
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return; }
        manager = (WindowManager) getSystemService(WINDOW_SERVICE);
        createTarget();
        createTrigger();
    }

    private void createTarget() {
        int size = prefs().getInt("target_size", 64);
        target = new View(this) {
            private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            @Override protected void onDraw(Canvas canvas) {
                float c = getWidth() / 2f;
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(4);
                paint.setColor(Color.rgb(35, 125, 65));
                canvas.drawCircle(c, c, c - 5, paint);
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.RED);
                canvas.drawCircle(c, c, 7, paint);
            }
        };
        targetParams = params(size, size);
        targetParams.x = Math.max(0, coordinate(true, "target", 360) - size / 2);
        targetParams.y = Math.max(0, coordinate(false, "target", 1200) - size / 2);
        target.setOnTouchListener(new PositionTouch(target, targetParams, true));
        manager.addView(target, targetParams);
    }

    private void createTrigger() {
        int size = prefs().getInt("trigger_size", 96);
        trigger = new TextView(this);
        String custom = prefs().getString("custom_icon", "");
        trigger.setText(custom.isEmpty() ? prefs().getString("icon", "●") : custom);
        trigger.setTextColor(Color.WHITE);
        trigger.setTextSize(Math.max(10, size / 7f));
        trigger.setGravity(Gravity.CENTER);
        trigger.setBackgroundColor(Color.rgb(210, 35, 45));
        trigger.setAlpha(prefs().getInt("trigger_opacity", 100) / 100f);
        triggerParams = params(size, size);
        triggerParams.x = coordinate(true, "trigger", 60);
        triggerParams.y = coordinate(false, "trigger", 500);
        trigger.setOnTouchListener(new TriggerTouch());
        manager.addView(trigger, triggerParams);

        close = new TextView(this);
        close.setText("X");
        close.setTextColor(Color.BLACK);
        close.setTextSize(16);
        close.setGravity(Gravity.CENTER);
        close.setBackgroundColor(Color.TRANSPARENT);
        closeParams = params(40, 40);
        manager.addView(close, closeParams);
        close.setOnClickListener(v -> stopSelf());
        updateClose();
        showTrigger();
    }

    private WindowManager.LayoutParams params(int width, int height) {
        return new WindowManager.LayoutParams(width, height,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
    }

    private android.content.SharedPreferences prefs() {
        return getSharedPreferences("settings", MODE_PRIVATE);
    }

    private int coordinate(boolean x, String prefix, int portraitDefault) {
        boolean landscape = getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;
        String key = (landscape ? prefix + "_landscape" : prefix + "_portrait")
                + (x ? "_x" : "_y");
        int fallback = landscape ? (x ? 806 : 540) : portraitDefault;
        return prefs().getInt(key, fallback);
    }

    private void savePosition(String prefix, int x, int y, int size) {
        boolean landscape = getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;
        String suffix = landscape ? "_landscape" : "_portrait";
        prefs().edit().putInt(prefix + suffix + "_x", x + size / 2)
                .putInt(prefix + suffix + "_y", y + size / 2).apply();
    }

    private void updateClose() {
        closeParams.x = triggerParams.x + triggerParams.width - 16;
        closeParams.y = triggerParams.y - 18;
        if (close.getWindowToken() != null) manager.updateViewLayout(close, closeParams);
    }

    private void showTrigger() {
        trigger.setVisibility(View.VISIBLE);
        close.setVisibility(View.VISIBLE);
        handler.removeCallbacks(hideTrigger);
        handler.postDelayed(hideTrigger, 4000);
    }

    private final class PositionTouch implements View.OnTouchListener {
        private final View view; private final WindowManager.LayoutParams lp; private final boolean targetPosition;
        private float downX, downY; private int startX, startY;
        private final Runnable showOnHold = FloatingButtonService.this::showTrigger;
        PositionTouch(View view, WindowManager.LayoutParams lp, boolean targetPosition) {
            this.view = view; this.lp = lp; this.targetPosition = targetPosition;
        }
        @Override public boolean onTouch(View v, MotionEvent e) {
            if (e.getAction() == MotionEvent.ACTION_DOWN) {
                downX = e.getRawX(); downY = e.getRawY(); startX = lp.x; startY = lp.y;
                handler.postDelayed(showOnHold, 2000);
                return true;
            }
            if (e.getAction() == MotionEvent.ACTION_MOVE) {
                lp.x = Math.max(0, startX + Math.round(e.getRawX() - downX));
                lp.y = Math.max(0, startY + Math.round(e.getRawY() - downY));
                manager.updateViewLayout(view, lp);
                savePosition(targetPosition ? "target" : "trigger", lp.x, lp.y, lp.width);
                if (!targetPosition) updateClose();
                return true;
            }
            if (e.getAction() == MotionEvent.ACTION_UP || e.getAction() == MotionEvent.ACTION_CANCEL) {
                handler.removeCallbacks(showOnHold);
            }
            return true;
        }
    }

    private final class TriggerTouch implements View.OnTouchListener {
        private float downX, downY; private int startX, startY; private boolean moved;
        @Override public boolean onTouch(View v, MotionEvent e) {
            if (e.getAction() == MotionEvent.ACTION_DOWN) {
                downX = e.getRawX(); downY = e.getRawY();
                startX = triggerParams.x; startY = triggerParams.y; moved = false; return true;
            }
            if (e.getAction() == MotionEvent.ACTION_MOVE) {
                if (Math.abs(e.getRawX() - downX) > 12 || Math.abs(e.getRawY() - downY) > 12) {
                    moved = true;
                    triggerParams.x = Math.max(0, startX + Math.round(e.getRawX() - downX));
                    triggerParams.y = Math.max(0, startY + Math.round(e.getRawY() - downY));
                    manager.updateViewLayout(trigger, triggerParams);
                    savePosition("trigger", triggerParams.x, triggerParams.y, triggerParams.width);
                    updateClose();
                }
                return true;
            }
            if (e.getAction() == MotionEvent.ACTION_UP && !moved) {
                long now = System.currentTimeMillis();
                if (now - lastTriggerTap < 350) {
                    trigger.setVisibility(trigger.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
                    close.setVisibility(trigger.getVisibility());
                    lastTriggerTap = 0;
                } else {
                    lastTriggerTap = now;
                    handler.postDelayed(() -> {
                        if (lastTriggerTap == now) SwipeAccessibilityService.requestTap();
                    }, 350);
                    showTrigger();
                }
                return true;
            }
            return true;
        }
    }

    @Override public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (manager != null) {
            if (target != null) manager.removeView(target);
            if (trigger != null) manager.removeView(trigger);
            if (close != null) manager.removeView(close);
        }
        prefs().edit().putBoolean("running", false).apply();
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
