package com.ritik.accidentresponse;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

/** Login + Register in one screen. This is also the launcher (routes to Dashboard if already signed in). */
public class AuthActivity extends AppCompatActivity {
    private boolean registerMode = false;
    private EditText etServer, etName, etPhone, etEmail, etPassword, etConfirm;
    private CheckBox cbTerms;
    private Button btnSubmit;
    private TextView tvTitle, tvSubtitle, tvToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Session.isLoggedIn(this)) {
            goDashboard();
            return;
        }
        setContentView(R.layout.activity_auth);
        etServer = findViewById(R.id.etServer);
        etName = findViewById(R.id.etName);
        etPhone = findViewById(R.id.etPhone);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirm = findViewById(R.id.etConfirm);
        cbTerms = findViewById(R.id.cbTerms);
        btnSubmit = findViewById(R.id.btnSubmit);
        tvTitle = findViewById(R.id.tvTitle);
        tvSubtitle = findViewById(R.id.tvSubtitle);
        tvToggle = findViewById(R.id.tvToggle);

        etServer.setText(Session.baseUrl(this));
        btnSubmit.setOnClickListener(v -> submit());
        tvToggle.setOnClickListener(v -> {
            registerMode = !registerMode;
            applyMode();
        });
        applyMode();
    }

    private void applyMode() {
        int vis = registerMode ? View.VISIBLE : View.GONE;
        etName.setVisibility(vis);
        etPhone.setVisibility(vis);
        etConfirm.setVisibility(vis);
        cbTerms.setVisibility(vis);
        tvTitle.setText(registerMode ? "Create Account" : "Welcome Back");
        tvSubtitle.setText(registerMode
                ? "Create your account to access your vehicle safety dashboard."
                : "Sign in to continue to your vehicle safety dashboard.");
        btnSubmit.setText(registerMode ? "Create Account" : "Sign In");
        tvToggle.setText(registerMode ? "Already have an account? Sign In" : "New here? Create Account");
    }

    private void setBusy(boolean busy) {
        btnSubmit.setEnabled(!busy);
        if (busy) btnSubmit.setText(registerMode ? "Creating Account..." : "Please wait...");
        else btnSubmit.setText(registerMode ? "Create Account" : "Sign In");
    }

    private void submit() {
        String server = etServer.getText().toString().trim();
        if (server.isEmpty()) {
            etServer.setError("Server address is required");
            return;
        }
        Session.setBaseUrl(this, server);
        String email = etEmail.getText().toString().trim();
        String pw = etPassword.getText().toString();
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (registerMode && name.isEmpty()) {
            etName.setError("Name is required");
            return;
        }
        if (!Ui.validEmail(email)) {
            etEmail.setError("Please enter a valid email address");
            return;
        }
        if (registerMode && !Ui.validPhone(phone)) {
            etPhone.setError("Please enter a valid 10-digit phone number");
            return;
        }
        if (pw.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            return;
        }
        if (registerMode && !pw.equals(etConfirm.getText().toString())) {
            etConfirm.setError("Passwords do not match");
            return;
        }
        if (registerMode && !cbTerms.isChecked()) {
            Ui.toast(this, "Please accept the terms and conditions");
            return;
        }

        setBusy(true);
        JSONObject body = registerMode
                ? Api.obj("full_name", name, "email", email, "phone", phone, "password", pw)
                : Api.obj("email", email, "password", pw);
        Api.call(this, "POST", registerMode ? "/auth/register" : "/auth/login", body, (ok, d, e) -> {
            setBusy(false);
            if (!ok) {
                Ui.toast(this, e);
                return;
            }
            Session.setToken(this, d.optString("access_token"));
            goDashboard();
        });
    }

    private void goDashboard() {
        startActivity(new Intent(this, DashboardActivity.class));
        finish();
    }
}
