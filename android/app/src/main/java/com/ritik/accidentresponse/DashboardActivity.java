package com.ritik.accidentresponse;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

public class DashboardActivity extends AppCompatActivity {
    private TextView tvWelcome, tvStatusTitle, tvStatusMsg, tvLatestType, tvLatestStatus, tvLatestTime, tvTotal, tvDevice;
    private View cardStatus, cardLatest;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private String lastIncident = null;
    private String latestId = null;
    private boolean loadedOnce = false;

    private final Runnable poll = new Runnable() {
        @Override
        public void run() {
            load();
            handler.postDelayed(this, 15000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!Session.isLoggedIn(this)) {
            goAuth();
            return;
        }
        setContentView(R.layout.activity_dashboard);
        tvWelcome = findViewById(R.id.tvWelcome);
        tvStatusTitle = findViewById(R.id.tvStatusTitle);
        tvStatusMsg = findViewById(R.id.tvStatusMsg);
        tvLatestType = findViewById(R.id.tvLatestType);
        tvLatestStatus = findViewById(R.id.tvLatestStatus);
        tvLatestTime = findViewById(R.id.tvLatestTime);
        tvTotal = findViewById(R.id.tvTotal);
        tvDevice = findViewById(R.id.tvDevice);
        cardStatus = findViewById(R.id.cardStatus);
        cardLatest = findViewById(R.id.cardLatest);

        findViewById(R.id.btnLiveLocation).setOnClickListener(v -> startActivity(new Intent(this, LocationActivity.class)));
        findViewById(R.id.btnEventHistory).setOnClickListener(v -> startActivity(new Intent(this, EventsActivity.class)));
        findViewById(R.id.btnEmergencyContacts).setOnClickListener(v -> startActivity(new Intent(this, ContactsActivity.class)));
        findViewById(R.id.btnProfile).setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        findViewById(R.id.btnSimulate).setOnClickListener(v -> showSimulateDialog());
        cardLatest.setOnClickListener(v -> {
            if (latestId != null) openEvent(latestId);
        });

        Api.call(this, "GET", "/auth/me", null, (ok, d, e) -> {
            if (ok && d.optJSONObject("user") != null) {
                tvWelcome.setText("Hello, " + d.optJSONObject("user").optString("full_name"));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!Session.isLoggedIn(this)) return;
        handler.removeCallbacks(poll);
        handler.post(poll);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(poll);
    }

    private void load() {
        Api.call(this, "GET", "/events/summary", null, (ok, d, e) -> {
            if (isFinishing() || isDestroyed()) return;
            if (!ok) {
                if (!Session.isLoggedIn(this)) {
                    goAuth();
                    return;
                }
                tvStatusTitle.setText("Offline");
                tvStatusMsg.setText(e);
                Ui.roundBg(cardStatus, 0xFF6B7685, 14);
                return;
            }
            String sys = d.optString("system_status", "Normal");
            if ("Danger".equals(sys)) {
                tvStatusTitle.setText("Emergency Detected");
                tvStatusMsg.setText("A possible accident was detected. Review the latest event now.");
            } else if ("Warning".equals(sys)) {
                tvStatusTitle.setText("System Warning");
                tvStatusMsg.setText("A warning was detected. Please review the latest event.");
            } else {
                tvStatusTitle.setText("System Safe");
                tvStatusMsg.setText("All safety systems are operating normally.");
            }
            Ui.roundBg(cardStatus, Ui.statusColor(sys), 14);

            tvTotal.setText(String.valueOf(d.optInt("total")));
            JSONObject dev = d.optJSONObject("device");
            tvDevice.setText(dev == null ? "No device" : dev.optString("name") + " \u2022 " + dev.optString("status"));

            JSONObject latest = d.optJSONObject("latest_event");
            if (latest == null) {
                latestId = null;
                tvLatestType.setText("No recent incident");
                tvLatestStatus.setVisibility(View.GONE);
                tvLatestTime.setText("");
            } else {
                String id = latest.optString("incident_id");
                latestId = id;
                tvLatestType.setText(latest.optString("event_type"));
                Ui.statusBadge(tvLatestStatus, latest.optString("status"));
                String[] w = Ui.parseWhen(latest.optString("occurred_at"));
                tvLatestTime.setText(w[0] + "  " + w[1]);
                if (loadedOnce && !id.equals(lastIncident) && "Danger".equals(latest.optString("status"))) {
                    showEmergencyAlert(latest);
                }
                lastIncident = id;
            }
            loadedOnce = true;
        });
    }

    private void showEmergencyAlert(JSONObject ev) {
        try {
            Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vib != null) {
                if (Build.VERSION.SDK_INT >= 26) {
                    vib.vibrate(VibrationEffect.createOneShot(800, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vib.vibrate(800);
                }
            }
        } catch (Exception ignored) {
        }
        final String id = ev.optString("incident_id");
        new AlertDialog.Builder(this)
                .setTitle("\u26A0 " + ev.optString("event_type"))
                .setMessage(ev.optString("description") + "\n\nIncident " + id)
                .setPositiveButton("View Event", (dlg, w) -> openEvent(id))
                .setNegativeButton("Dismiss", null)
                .show();
    }

    private void showSimulateDialog() {
        final String[] types = {"Confirmed Accident", "Possible Accident", "Obstacle Detected", "System Warning", "System Check"};
        new AlertDialog.Builder(this)
                .setTitle("Simulate safety unit event")
                .setItems(types, (dlg, which) ->
                        Api.call(this, "POST", "/demo/simulate", Api.obj("event_type", types[which]), (ok, d, e) -> {
                            if (!ok) Ui.toast(this, e);
                            else load();
                        }))
                .show();
    }

    private void openEvent(String id) {
        startActivity(new Intent(this, EventDetailActivity.class).putExtra("incident_id", id));
    }

    private void goAuth() {
        Intent i = new Intent(this, AuthActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }
}
