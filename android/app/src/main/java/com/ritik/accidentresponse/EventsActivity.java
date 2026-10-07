package com.ritik.accidentresponse;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

public class EventsActivity extends AppCompatActivity {
    private LinearLayout container;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        Ui.toolbar(this, "Event History");
        container = findViewById(R.id.listContainer);
        tvEmpty = findViewById(R.id.tvEmpty);
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        Api.call(this, "GET", "/events?limit=100", null, (ok, d, e) -> {
            if (isFinishing() || isDestroyed()) return;
            if (!ok) {
                Ui.toast(this, e);
                return;
            }
            container.removeAllViews();
            JSONArray items = d.optJSONArray("items");
            if (items == null || items.length() == 0) {
                tvEmpty.setText("No safety events yet\nYour vehicle and system events will appear here.");
                tvEmpty.setVisibility(View.VISIBLE);
                return;
            }
            tvEmpty.setVisibility(View.GONE);
            for (int i = 0; i < items.length(); i++) {
                JSONObject ev = items.optJSONObject(i);
                if (ev == null) continue;
                View row = getLayoutInflater().inflate(R.layout.item_event, container, false);
                ((TextView) row.findViewById(R.id.tvEventType)).setText(ev.optString("event_type"));
                Ui.statusBadge((TextView) row.findViewById(R.id.tvEventStatus), ev.optString("status"));
                String[] w = Ui.parseWhen(ev.optString("occurred_at"));
                ((TextView) row.findViewById(R.id.tvEventTime)).setText(ev.optString("incident_id") + "  \u2022  " + w[0] + ", " + w[1]);
                ((TextView) row.findViewById(R.id.tvEventDescription)).setText(ev.optString("description"));
                final String id = ev.optString("incident_id");
                row.setOnClickListener(v -> startActivity(new Intent(this, EventDetailActivity.class).putExtra("incident_id", id)));
                container.addView(row);
            }
        });
    }
}
