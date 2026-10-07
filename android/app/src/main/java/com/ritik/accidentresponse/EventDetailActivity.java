package com.ritik.accidentresponse;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

public class EventDetailActivity extends AppCompatActivity {
    private TextView tvId, tvType, tvStatus, tvLocation, tvCoords, tvDate, tvTime, tvDesc;
    private View btnResolve;
    private String incidentId;
    private JSONObject current;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);
        Ui.toolbar(this, "Event Details");
        incidentId = getIntent().getStringExtra("incident_id");
        tvId = findViewById(R.id.tvDetailId);
        tvType = findViewById(R.id.tvDetailType);
        tvStatus = findViewById(R.id.tvDetailStatus);
        tvLocation = findViewById(R.id.tvDetailLocation);
        tvCoords = findViewById(R.id.tvDetailCoords);
        tvDate = findViewById(R.id.tvDetailDate);
        tvTime = findViewById(R.id.tvDetailTime);
        tvDesc = findViewById(R.id.tvDetailDesc);
        btnResolve = findViewById(R.id.btnResolve);

        findViewById(R.id.btnMap).setOnClickListener(v -> {
            if (current == null || current.isNull("latitude")) {
                Ui.toast(this, "No coordinates for this event.");
                return;
            }
            Ui.openMaps(this, current.optDouble("latitude"), current.optDouble("longitude"));
        });
        findViewById(R.id.btnShare).setOnClickListener(v -> share());
        btnResolve.setOnClickListener(v ->
                Api.call(this, "POST", "/events/" + incidentId + "/resolve", null, (ok, d, e) -> {
                    if (!ok) Ui.toast(this, e);
                    else load();
                }));
        load();
    }

    private void load() {
        Api.call(this, "GET", "/events/" + incidentId, null, (ok, d, e) -> {
            if (isFinishing() || isDestroyed()) return;
            if (!ok) {
                Ui.toast(this, e);
                return;
            }
            current = d.optJSONObject("event");
            if (current == null) return;
            tvId.setText(current.optString("incident_id"));
            tvType.setText(current.optString("event_type"));
            Ui.statusBadge(tvStatus, current.optString("status"));
            tvLocation.setText(current.optString("location_text"));
            tvCoords.setText(current.isNull("latitude") ? "-" : current.optDouble("latitude") + ", " + current.optDouble("longitude"));
            String[] w = Ui.parseWhen(current.optString("occurred_at"));
            tvDate.setText(w[0]);
            tvTime.setText(w[1]);
            tvDesc.setText(current.optString("description"));
            btnResolve.setVisibility("Resolved".equals(current.optString("status")) ? View.GONE : View.VISIBLE);
        });
    }

    private void share() {
        if (current == null) return;
        String[] w = Ui.parseWhen(current.optString("occurred_at"));
        String maps = current.isNull("latitude") ? "-"
                : "https://maps.google.com/?q=" + current.optDouble("latitude") + "," + current.optDouble("longitude");
        String text = "ACCIDENT RESPONSE - INCIDENT REPORT\n"
                + "Incident ID: " + current.optString("incident_id") + "\n"
                + "Type: " + current.optString("event_type") + "\n"
                + "Status: " + current.optString("status") + "\n"
                + "Date/Time: " + w[0] + ", " + w[1] + "\n"
                + "Location: " + maps + "\n"
                + "Details: " + current.optString("description");
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(i, "Share incident report"));
    }
}
