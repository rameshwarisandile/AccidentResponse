package com.ritik.accidentresponse;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class LocationActivity extends AppCompatActivity {
    private TextView tvCoords, tvStatus, tvUpdated;
    private Double lat = null, lng = null;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable poll = new Runnable() {
        @Override
        public void run() {
            load();
            handler.postDelayed(this, 10000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);
        Ui.toolbar(this, "Live Location");
        tvCoords = findViewById(R.id.tvCoords);
        tvStatus = findViewById(R.id.tvLocStatus);
        tvUpdated = findViewById(R.id.tvUpdated);
        findViewById(R.id.btnRefresh).setOnClickListener(v -> load());
        findViewById(R.id.btnOpenMaps).setOnClickListener(v -> {
            if (lat == null || lng == null) {
                Ui.toast(this, "No GPS location yet.");
                return;
            }
            Ui.openMaps(this, lat, lng);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.removeCallbacks(poll);
        handler.post(poll);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(poll);
    }

    private void load() {
        Api.call(this, "GET", "/location/latest", null, (ok, d, e) -> {
            if (isFinishing() || isDestroyed()) return;
            if (!ok) {
                tvStatus.setText(e);
                return;
            }
            if (d.isNull("lat")) {
                lat = null;
                lng = null;
                tvCoords.setText("GPS location will appear here");
                tvStatus.setText(d.optString("status", "No data"));
                tvUpdated.setText("-");
                return;
            }
            lat = d.optDouble("lat");
            lng = d.optDouble("lng");
            tvCoords.setText(String.format(Locale.US, "%.5f, %.5f", lat, lng));
            tvStatus.setText(d.optString("status"));
            String[] w = Ui.parseWhen(d.optString("last_updated"));
            tvUpdated.setText(w[0] + "  " + w[1]);
        });
    }
}
