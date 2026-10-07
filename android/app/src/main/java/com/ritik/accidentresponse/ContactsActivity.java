package com.ritik.accidentresponse;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;

public class ContactsActivity extends AppCompatActivity {
    private static final String[] REL = {"Family", "Friend", "Brother", "Sister", "Mother", "Father", "Husband", "Other"};
    private LinearLayout container;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        Ui.toolbar(this, "Emergency Contacts");
        container = findViewById(R.id.listContainer);
        tvEmpty = findViewById(R.id.tvEmpty);
        Button add = findViewById(R.id.btnAdd);
        add.setVisibility(View.VISIBLE);
        add.setOnClickListener(v -> showDialog(null));
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        Api.call(this, "GET", "/contacts", null, (ok, d, e) -> {
            if (isFinishing() || isDestroyed()) return;
            if (!ok) {
                Ui.toast(this, e);
                return;
            }
            render(d.optJSONArray("items"));
        });
    }

    private void section(String title) {
        TextView t = (TextView) getLayoutInflater().inflate(R.layout.item_section, container, false);
        t.setText(title);
        container.addView(t);
    }

    private void render(JSONArray items) {
        container.removeAllViews();
        if (items == null || items.length() == 0) {
            tvEmpty.setText("No Emergency Contacts\nAdd a trusted contact who can be notified during an emergency.");
            tvEmpty.setVisibility(View.VISIBLE);
            return;
        }
        tvEmpty.setVisibility(View.GONE);
        boolean primaryHeader = false, otherHeader = false;
        for (int i = 0; i < items.length(); i++) {
            JSONObject c = items.optJSONObject(i);
            if (c == null) continue;
            boolean primary = c.optBoolean("is_primary");
            if (primary && !primaryHeader) {
                section("PRIMARY CONTACT");
                primaryHeader = true;
            } else if (!primary && !otherHeader) {
                section("OTHER CONTACTS");
                otherHeader = true;
            }
            addRow(c, primary);
        }
    }

    private void addRow(JSONObject c, boolean primary) {
        View row = getLayoutInflater().inflate(R.layout.item_contact, container, false);
        final int id = c.optInt("id");
        final String name = c.optString("name");
        final String phone = c.optString("phone");
        ((TextView) row.findViewById(R.id.tvContactName)).setText(name);
        TextView tvPhone = row.findViewById(R.id.tvContactPhone);
        tvPhone.setText(phone);
        tvPhone.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone))));
        ((TextView) row.findViewById(R.id.tvContactRelationship)).setText(c.optString("relationship"));
        TextView badge = row.findViewById(R.id.tvBadgePrimary);
        Button btnPrimary = row.findViewById(R.id.btnPrimary);
        if (primary) {
            Ui.badge(badge, "PRIMARY", 0xFF0B3D91);
            btnPrimary.setVisibility(View.GONE);
        } else {
            badge.setVisibility(View.GONE);
            btnPrimary.setOnClickListener(v ->
                    Api.call(this, "POST", "/contacts/" + id + "/set-primary", null, (ok, d, e) -> {
                        if (!ok) Ui.toast(this, e);
                        else load();
                    }));
        }
        row.findViewById(R.id.btnEdit).setOnClickListener(v -> showDialog(c));
        row.findViewById(R.id.btnDelete).setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("Delete Emergency Contact?")
                        .setMessage("Are you sure you want to delete this contact?")
                        .setPositiveButton("Delete", (dlg, w) ->
                                Api.call(this, "DELETE", "/contacts/" + id, null, (ok, d, e) -> {
                                    if (!ok) Ui.toast(this, e);
                                    else load();
                                }))
                        .setNegativeButton("Cancel", null)
                        .show());
        container.addView(row);
    }

    private void showDialog(final JSONObject existing) {
        View v = getLayoutInflater().inflate(R.layout.dialog_contact, null);
        final EditText etName = v.findViewById(R.id.etName);
        final EditText etPhone = v.findViewById(R.id.etPhone);
        final Spinner sp = v.findViewById(R.id.spRelationship);
        final CheckBox cb = v.findViewById(R.id.cbPrimary);
        sp.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, REL));
        if (existing != null) {
            etName.setText(existing.optString("name"));
            etPhone.setText(existing.optString("phone"));
            int idx = Arrays.asList(REL).indexOf(existing.optString("relationship"));
            sp.setSelection(Math.max(idx, 0));
            cb.setChecked(existing.optBoolean("is_primary"));
        }
        final AlertDialog dlg = new AlertDialog.Builder(this)
                .setTitle(existing == null ? "Add Emergency Contact" : "Edit Emergency Contact")
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
            if (!Ui.validPhone(phone)) {
                etPhone.setError("Please enter a valid 10-digit phone number");
                return;
            }
            JSONObject body = Api.obj("name", name, "phone", phone,
                    "relationship", REL[sp.getSelectedItemPosition()], "is_primary", cb.isChecked());
            String method = existing == null ? "POST" : "PUT";
            String path = existing == null ? "/contacts" : "/contacts/" + existing.optInt("id");
            Api.call(this, method, path, body, (ok, d, e) -> {
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
