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
import android.widget.Toast;
import android.view.MotionEvent;
import android.content.res.Configuration;

public class FloatingButtonService extends Service {
    private WindowManager windowManager;
    private TextView button;
    private TextView target;
    private WindowManager.LayoutParams targetParams;

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

        button = new TextView(this);
        button.setText("TAP");
        button.setTextColor(Color.WHITE);
        button.setTextSize(12);
        button.setGravity(Gravity.CENTER);
        button.setBackgroundColor(Color.rgb(21, 101, 192));
        button.setOnClickListener(v -> {
            if (!SwipeAccessibilityService.requestGesture()) {
                Toast.makeText(this, "Enable Swipe Tap in Accessibility settings first.",
                        Toast.LENGTH_SHORT).show();
            }
        });
        int type = type();
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                72, 56, type, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.END;
        params.x = 12;
        params.y = 220;
        windowManager.addView(button, params);
    }

    @Override
    public void onDestroy() {
        getSharedPreferences("settings", MODE_PRIVATE).edit().putBoolean("running", false).apply();
        if (button != null && windowManager != null) {
            windowManager.removeView(button);
        }
        if (target != null && windowManager != null) {
            windowManager.removeView(target);
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
}
