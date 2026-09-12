package com.almagabi.swipetap;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.res.Configuration;
import android.graphics.Path;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;

public class SwipeAccessibilityService extends AccessibilityService {
    private static SwipeAccessibilityService instance;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private android.content.SharedPreferences prefs;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        prefs = getSharedPreferences("settings", MODE_PRIVATE);
        instance = this;
    }

    public static boolean requestTap() {
        if (instance == null || instance.prefs == null) {
            return false;
        }
        int delay = Math.max(0, Math.min(instance.prefs.getInt("delay_ms", 0), 5000));
        int repeats = Math.max(1, Math.min(instance.prefs.getInt("repeats", 1), 10));
        for (int i = 0; i < repeats; i++) {
            instance.handler.postDelayed(instance::tapConfiguredPoint, delay + (i * 120L));
        }
        return true;
    }

    private void tapConfiguredPoint() {
        boolean landscape = getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;
        int x = prefs.getInt(landscape ? "landscape_x" : "portrait_x", landscape ? 806 : 360);
        int y = prefs.getInt(landscape ? "landscape_y" : "portrait_y", landscape ? 540 : 1200);
        Path path = new Path();
        path.moveTo(Math.max(0, x), Math.max(0, y));
        GestureDescription.StrokeDescription stroke =
                new GestureDescription.StrokeDescription(path, 0, 50);
        dispatchGesture(new GestureDescription.Builder().addStroke(stroke).build(), null, null);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onDestroy() {
        if (instance == this) {
            instance = null;
        }
        super.onDestroy();
    }
}
