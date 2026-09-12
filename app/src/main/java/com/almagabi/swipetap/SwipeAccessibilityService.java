package com.almagabi.swipetap;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.SharedPreferences;
import android.graphics.Path;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;

public class SwipeAccessibilityService extends AccessibilityService {
    private static final String PREFS = "settings";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private SharedPreferences prefs;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
    }

    public static final String ACTION_FLOATING_TAP = "com.almagabi.swipetap.FLOATING_TAP";

    @Override
    public int onStartCommand(android.content.Intent intent, int flags, int startId) {
        if (intent != null && ACTION_FLOATING_TAP.equals(intent.getAction()) && prefs != null) {
            int delay = Math.max(0, Math.min(prefs.getInt("delay_ms", 0), 5000));
            int repeats = Math.max(1, Math.min(prefs.getInt("repeats", 1), 10));
            for (int i = 0; i < repeats; i++) {
                handler.postDelayed(this::tapConfiguredPoint, delay + (i * 120L));
            }
        }
        return START_NOT_STICKY;
    }

    private void tapConfiguredPoint() {
        boolean landscape = (getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE);
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
        // Gesture callbacks are sufficient; view events are not used.
    }

    @Override
    public void onInterrupt() {
        handler.removeCallbacksAndMessages(null);
    }
}
