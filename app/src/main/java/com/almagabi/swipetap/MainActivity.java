package com.almagabi.swipetap;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
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

        root.addView(section("Gesture"));
        Spinner gesture = spinner(new String[]{"Tap", "Swipe"}, prefs.getString("gesture", "Tap"));
        root.addView(gesture);
        Spinner direction = spinner(new String[]{"Down", "Up", "Left", "Right"},
                prefs.getString("direction", "Down"));
        root.addView(direction, margins(0, 4, 0, 8));

        root.addView(section("Portrait coordinates (720 x 1612)"));
        LinearLayout portrait = row();
        EditText portraitX = numberField("Tap/start X", prefs.getInt("portrait_x", 360));
        EditText portraitY = numberField("Tap/start Y", prefs.getInt("portrait_y", 1200));
        portrait.addView(portraitX, weight()); portrait.addView(portraitY, weight());
        root.addView(portrait);

        root.addView(section("Landscape coordinates (1612 x 720)"));
        LinearLayout landscape = row();
        EditText landscapeX = numberField("Tap/start X", prefs.getInt("landscape_x", 806));
        EditText landscapeY = numberField("Tap/start Y", prefs.getInt("landscape_y", 540));
        landscape.addView(landscapeX, weight()); landscape.addView(landscapeY, weight());
        root.addView(landscape);

        root.addView(section("Timing"));
        LinearLayout timing = row();
        EditText delay = numberField("Delay ms", prefs.getInt("delay_ms", 0));
        EditText repeats = numberField("Repeats", prefs.getInt("repeats", 1));
        timing.addView(delay, weight());
        timing.addView(repeats, weight());
        root.addView(timing);

        Button save = new Button(this);
        save.setText("Save settings");
        save.setOnClickListener(v -> {
            prefs.edit()
                    .putInt("portrait_x", value(portraitX, 360))
                    .putInt("portrait_y", value(portraitY, 1200))
                    .putInt("landscape_x", value(landscapeX, 806))
                    .putInt("landscape_y", value(landscapeY, 540))
                    .putString("gesture", gesture.getSelectedItem().toString())
                    .putString("direction", direction.getSelectedItem().toString())
                    .putInt("delay_ms", Math.min(value(delay, 0), 5000))
                    .putInt("repeats", Math.max(1, Math.min(value(repeats, 1), 10)))
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

    private Spinner spinner(String[] values, String selected) {
        Spinner spinner = new Spinner(this);
        spinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, values));
        for (int i = 0; i < values.length; i++) {
            if (values[i].equalsIgnoreCase(selected)) spinner.setSelection(i);
        }
        return spinner;
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
