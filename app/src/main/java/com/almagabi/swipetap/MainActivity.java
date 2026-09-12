package com.almagabi.swipetap;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class MainActivity extends Activity {
    private android.content.SharedPreferences prefs;
    private TextView status;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("settings", MODE_PRIVATE);
        buildScreen();
    }

    @Override protected void onResume() {
        super.onResume();
        if (status != null) updateStatus();
    }

    private void buildScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(18), dp(20), dp(18));
        root.addView(label("Tap Action", 26));
        TextView help = label("Configure the floating button and target, then start the overlay.", 16);
        help.setTextColor(Color.DKGRAY);
        root.addView(help, margins(0, 4, 0, 16));
        status = label("", 14);
        root.addView(status, margins(0, 0, 0, 10));

        Button overlay = new Button(this);
        overlay.setText("Allow floating button");
        overlay.setOnClickListener(v -> startActivity(new Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:" + getPackageName()))));
        root.addView(overlay);
        Button accessibility = new Button(this);
        accessibility.setText("Enable Accessibility tap service");
        accessibility.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        root.addView(accessibility);
        Button start = new Button(this);
        start.setText("Start floating TAP button");
        start.setOnClickListener(v -> {
            if (Settings.canDrawOverlays(this)) {
                prefs.edit().putBoolean("running", true).apply();
                startService(new Intent(this, FloatingButtonService.class));
                status.setText("Running. The trigger and target remain visible until Stop.");
            } else status.setText("Allow floating windows before starting.");
        });
        root.addView(start, margins(0, 12, 0, 0));
        Button stop = new Button(this);
        stop.setText("Stop");
        stop.setOnClickListener(v -> {
            stopService(new Intent(this, FloatingButtonService.class));
            status.setText("Stopped. No floating overlay is running.");
        });
        root.addView(stop);

        root.addView(section("Trigger button"));
        TextView triggerSizeLabel = label("", 14);
        SeekBar triggerSize = slider(176, Math.max(24, prefs.getInt("trigger_size", 96)) - 24);
        TextView triggerOpacityLabel = label("", 14);
        SeekBar triggerOpacity = slider(100, prefs.getInt("trigger_opacity", 100));
        triggerSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar b, int p, boolean user) {
                int size = p + 24;
                triggerSizeLabel.setText("Size: " + size + " px");
                FloatingButtonService.applyTriggerSettings(size, triggerOpacity.getProgress());
            }
            public void onStartTrackingTouch(SeekBar b) {}
            public void onStopTrackingTouch(SeekBar b) {}
        });
        root.addView(triggerSizeLabel);
        root.addView(triggerSize);
        triggerOpacity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar b, int p, boolean user) {
                triggerOpacityLabel.setText("Opacity: " + p + "%");
                FloatingButtonService.applyTriggerSettings(triggerSize.getProgress() + 24, p);
            }
            public void onStartTrackingTouch(SeekBar b) {}
            public void onStopTrackingTouch(SeekBar b) {}
        });
        root.addView(triggerOpacityLabel);
        root.addView(triggerOpacity);
        triggerSizeLabel.setText("Size: " + (triggerSize.getProgress() + 24) + " px");
        triggerOpacityLabel.setText("Opacity: " + triggerOpacity.getProgress() + "%");

        root.addView(section("Target area"));
        TextView targetSizeLabel = label("", 14);
        SeekBar targetSize = slider(160, Math.max(24, prefs.getInt("target_size", 64)) - 24);
        targetSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar b, int p, boolean user) {
                int size = p + 24;
                targetSizeLabel.setText("Size: " + size + " px");
                FloatingButtonService.applyTargetSize(size);
            }
            public void onStartTrackingTouch(SeekBar b) {}
            public void onStopTrackingTouch(SeekBar b) {}
        });
        root.addView(targetSizeLabel);
        root.addView(targetSize);
        targetSizeLabel.setText("Size: " + (targetSize.getProgress() + 24) + " px");

        Button save = new Button(this);
        save.setText("Save settings");
        save.setOnClickListener(v -> {
            prefs.edit().putInt("trigger_size", triggerSize.getProgress() + 24)
                    .putInt("trigger_opacity", triggerOpacity.getProgress())
                    .putInt("target_size", targetSize.getProgress() + 24).apply();
            FloatingButtonService.applyTriggerSettings(triggerSize.getProgress() + 24,
                    triggerOpacity.getProgress());
            FloatingButtonService.applyTargetSize(targetSize.getProgress() + 24);
            status.setText("Settings saved.");
        });
        root.addView(save, margins(0, 18, 0, 0));
        setContentView(root);
        updateStatus();
    }

    private SeekBar slider(int max, int progress) {
        SeekBar bar = new SeekBar(this);
        bar.setMax(max);
        bar.setProgress(Math.max(0, Math.min(max, progress)));
        return bar;
    }

    private void updateStatus() {
        String enabled = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        boolean active = enabled != null && enabled.contains(
                getPackageName() + "/" + SwipeAccessibilityService.class.getName());
        status.setText(active ? "Accessibility tap service is enabled."
                : "Enable the Accessibility tap service before tapping.");
        status.setTextColor(active ? Color.rgb(20, 100, 50) : Color.rgb(170, 70, 20));
    }

    private TextView section(String text) {
        TextView view = label(text, 15);
        view.setTextColor(Color.rgb(30, 80, 140));
        return view;
    }
    private TextView label(String text, int size) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        return view;
    }
    private LinearLayout.LayoutParams margins(int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(dp(l), dp(t), dp(r), dp(b));
        return p;
    }
    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
