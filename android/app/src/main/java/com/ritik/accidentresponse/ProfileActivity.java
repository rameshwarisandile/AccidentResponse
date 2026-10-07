package com.ritik.accidentresponse;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import org.json.JSONArray;
import org.json.JSONObject;

public class ProfileActivity extends AppCompatActivity {
    private TextView tvName, tvEmail, tvPhone, tvRole, tvDevice, tvStatus;
    private SwitchCompat swAlerts, swNotif;
    private boolean binding = false;
    private JSONObject user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        Ui.toolbar(this, "Profile & Settings");
        tvName = findViewById(R.id.tvName);
        tvEmail = findViewById(R.id.tvEmail);
        tvPhone = findViewById(R.id.tvPhone);
        tvRole = findViewById(R.id.tvRole);
        tvDevice = findViewById(R.id.tvDevice);
        tvStatus = findViewById(R.id.tvStatus);
        swAlerts = findViewById(R.id.switchAlerts);
        swNotif = findViewById(R.id.switchNotifications);

        swAlerts.setOnCheckedChangeListener((b, checked) -> saveSettings());
        swNotif.setOnCheckedChangeListener((b, checked) -> saveSettings());
        findViewById(R.id.btnEditProfile).setOnClickListener(v -> showEditDialog());
        findViewById(R.id.btnLogout).setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("Logout")
                        .setMessage("Are you sure you want to log out?")
                        .setPositiveButton("Logout", (dlg, w) -> {
                            Session.clear(this);
                            Intent i = new Intent(this, AuthActivity.class);
                            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(i);
                            finish();
                        })
                        .setNegativeButton("Cancel", null)
                        .show());
        load();
    }

    private void load() {
        Api.call(this, "GET", "/auth/me", null, (ok, d, e) -> {
            if (isFinishing() || isDestroyed()) return;
            if (!ok) {
                Ui.toast(this, e);
                return;
            }
            user = d.optJSONObject("user");
            if (user == null) return;
            tvName.setText(user.optString("full_name"));
            tvEmail.setText(user.optString("email"));
            String phone = user.optString("phone");
            tvPhone.setText(phone.isEmpty() ? "-" : phone);
            tvRole.setText(user.optString("role"));
            binding = true;
            swAlerts.setChecked(user.optBoolean("emergency_alerts_enabled"));
            swNotif.setChecked(user.optBoolean("notifications_enabled"));
            binding = false;
        });
        Api.call(this, "GET", "/devices", null, (ok, d, e) -> {
            if (isFinishing() || isDestroyed() || !ok) return;
            JSONArray items = d.optJSONArray("items");
            if (items != null && items.length() > 0) {
                JSONObject dev = items.optJSONObject(0);
                tvDevice.setText(dev.optString("name"));
                tvStatus.setText(dev.optString("status"));
            } else {
                tvDevice.setText("No device paired");
                tvStatus.setText("-");
            }
        });
    }

    private void saveSettings() {
        if (binding) return;
        Api.call(this, "PUT", "/profile/settings",
                Api.obj("emergency_alerts_enabled", swAlerts.isChecked(), "notifications_enabled", swNotif.isChecked()),
                (ok, d, e) -> {
                    if (!ok) Ui.toast(this, e);
                });
    }

    private void showEditDialog() {
        if (user == null) return;
        View v = getLayoutInflater().inflate(R.layout.dialog_edit_profile, null);
        final EditText etName = v.findViewById(R.id.etName);
        final EditText etPhone = v.findViewById(R.id.etPhone);
        etName.setText(user.optString("full_name"));
        etPhone.setText(user.optString("phone"));
        final AlertDialog dlg = new AlertDialog.Builder(this)
                .setTitle("Edit Profile")
                .setView(v)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create();
        dlg.setOnShowListener(x -> dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(b -> {
            String name = etName.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            if (name.isEmpty()) {
                etName.setError("Name is required");
                return;
            }
            if (!phone.isEmpty() && !Ui.validPhone(phone)) {
                etPhone.setError("Please enter a valid 10-digit phone number");
                return;
            }
            Api.call(this, "PUT", "/profile", Api.obj("full_name", name, "phone", phone), (ok, d, e) -> {
                if (!ok) {
                    Ui.toast(this, e);
                    return;
                }
                dlg.dismiss();
                load();
            });
        }));
        dlg.show();
    }
}
