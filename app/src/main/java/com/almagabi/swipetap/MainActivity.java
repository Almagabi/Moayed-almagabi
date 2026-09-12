package com.almagabi.swipetap;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final String PREFS = "settings";
    private android.content.SharedPreferences prefs;
    private TextView status;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        buildScreen();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (status != null) updateStatus();
    }

    private void buildScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(18), dp(20), dp(18));

        TextView title = label("Swipe Tap", 26);
        root.addView(title);
        TextView help = label("Choose coordinates, enable Accessibility, then use the floating TAP button.", 16);
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
        accessibility.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        root.addView(accessibility);

        Button start = new Button(this);
        start.setText("Start floating TAP button");
        start.setOnClickListener(v -> {
            if (Settings.canDrawOverlays(this)) {
                prefs.edit().putBoolean("running", true).apply();
                startService(new Intent(this, FloatingButtonService.class));
                status.setText("Running. Tap Stop to remove the floating button.");
            } else {
                status.setText("Allow floating windows before starting.");
            }
        });
        root.addView(start, margins(0, 12, 0, 0));

        Button stop = new Button(this);
        stop.setText("Stop");
        stop.setOnClickListener(v -> {
            prefs.edit().putBoolean("running", false).apply();
            stopService(new Intent(this, FloatingButtonService.class));
            status.setText("Stopped. No floating button is running.");
        });
        root.addView(stop);

        root.addView(section("Floating trigger button"));
        android.widget.Spinner icon = new android.widget.Spinner(this);
        icon.setAdapter(new android.widget.ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"●", "▶", "Ⅱ", "⏭", "■", ">>", "<<"}));
        String savedIcon = prefs.getString("icon", "●");
        for (int i = 0; i < icon.getCount(); i++) {
            if (icon.getItemAtPosition(i).toString().equals(savedIcon)) icon.setSelection(i);
        }
        root.addView(icon);
        EditText customIcon = new EditText(this);
        customIcon.setHint("Custom emoji or symbol (optional)");
        customIcon.setText(prefs.getString("custom_icon", ""));
        root.addView(customIcon);

        root.addView(section("Target reticle"));
        TextView sizeLabel = label("", 14);
        root.addView(sizeLabel);
        SeekBar size = new SeekBar(this);
        size.setMax(160);
        size.setProgress(Math.max(24, prefs.getInt("target_size", 64)) - 24);
        size.setOnSeekBarChangeListener(seekListener(sizeLabel, "Size: ", 24));
        root.addView(size);
        TextView opacityLabel = label("", 14);
        root.addView(opacityLabel);
        SeekBar opacity = new SeekBar(this);
        opacity.setMax(100);
        opacity.setProgress(prefs.getInt("target_opacity", 100));
        opacity.setOnSeekBarChangeListener(seekListener(opacityLabel, "Opacity: ", 0));
        root.addView(opacity);
        sizeLabel.setText("Size: " + (size.getProgress() + 24) + " px");
        opacityLabel.setText("Opacity: " + opacity.getProgress() + "%");

        Button save = new Button(this);
        save.setText("Save settings");
        save.setOnClickListener(v -> {
            prefs.edit()
                    .putString("icon", icon.getSelectedItem().toString())
                    .putString("custom_icon", customIcon.getText().toString())
                    .putInt("target_size", size.getProgress() + 24)
                    .putInt("target_opacity", opacity.getProgress())
                    .apply();
            status.setText("Settings saved.");
        });
        root.addView(save, margins(0, 18, 0, 0));
        setContentView(root);
        updateStatus();
    }

    private void updateStatus() {
        String enabledServices = Settings.Secure.getString(
                getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        boolean serviceOn = enabledServices != null && enabledServices.contains(
                getPackageName() + "/" + SwipeAccessibilityService.class.getName());
        status.setText(serviceOn
                ? "Accessibility tap service is enabled."
                : "Enable the Accessibility tap service before tapping.");
        status.setTextColor(serviceOn ? Color.rgb(20, 100, 50) : Color.rgb(170, 70, 20));
    }

    private TextView section(String text) {
        TextView view = label(text, 15);
        view.setTextColor(Color.rgb(30, 80, 140));
        return view;
    }

    private EditText numberField(String hint, int value) {
        EditText field = new EditText(this);
        field.setHint(hint);
        field.setText(String.valueOf(value));
        field.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        return field;
    }

    private SeekBar.OnSeekBarChangeListener seekListener(TextView label, String prefix, int offset) {
        return new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                label.setText(prefix + (progress + offset)
                        + (prefix.startsWith("Opacity") ? "%" : " px"));
            }
            public void onStartTrackingTouch(SeekBar bar) {}
            public void onStopTrackingTouch(SeekBar bar) {}
        };
    }

    private int value(EditText field, int fallback) {
        try {
            return Integer.parseInt(field.getText().toString().trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private TextView label(String text, int size) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        return view;
    }

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        return row;
    }

    private LinearLayout.LayoutParams weight() {
        return new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
    }

    private LinearLayout.LayoutParams margins(int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(dp(left), dp(top), dp(right), dp(bottom));
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
