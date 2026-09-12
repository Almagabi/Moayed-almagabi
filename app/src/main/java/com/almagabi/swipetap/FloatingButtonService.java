package com.almagabi.swipetap;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

public class FloatingButtonService extends Service {
    private WindowManager windowManager;
    private TextView button;

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
        int type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
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
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
