package com.ritik.accidentresponse;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class Ui {
    public static int statusColor(String s) {
        if ("Danger".equals(s)) return 0xFFD93025;
        if ("Warning".equals(s)) return 0xFFF0A202;
        return 0xFF1E9E5A; // Normal / Resolved
    }

    public static float dp(Context c, float v) {
        return v * c.getResources().getDisplayMetrics().density;
    }

    public static void roundBg(View v, int color, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(v.getContext(), radiusDp));
        v.setBackground(g);
    }

    public static void badge(TextView tv, String text, int color) {
        roundBg(tv, color, 12);
        tv.setText(text);
        tv.setTextColor(Color.WHITE);
        int h = (int) dp(tv.getContext(), 10);
        int v = (int) dp(tv.getContext(), 4);
        tv.setPadding(h, v, h, v);
        tv.setVisibility(View.VISIBLE);
    }

    public static void statusBadge(TextView tv, String status) {
        badge(tv, status, statusColor(status));
    }

    public static void toast(Context c, String m) {
        Toast.makeText(c, m, Toast.LENGTH_LONG).show();
    }

    public static void toolbar(AppCompatActivity a, String title) {
        ((TextView) a.findViewById(R.id.tvToolbarTitle)).setText(title);
        a.findViewById(R.id.btnBack).setOnClickListener(v -> a.finish());
    }

    public static boolean validPhone(String p) {
        return p != null && p.matches("\\d{10}");
    }

    public static boolean validEmail(String e) {
        return e != null && android.util.Patterns.EMAIL_ADDRESS.matcher(e).matches();
    }

    /** Server sends UTC like 2026-10-07T10:15:00Z. Returns {date, time} in phone's timezone. */
    public static String[] parseWhen(String iso) {
        try {
            SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
            in.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date d = in.parse(iso);
            return new String[]{
                    new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(d),
                    new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(d)};
        } catch (Exception e) {
            return new String[]{iso == null || iso.isEmpty() ? "-" : iso, "-"};
        }
    }

    public static void openMaps(Context c, double lat, double lng) {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/maps/search/?api=1&query=" + lat + "," + lng));
            c.startActivity(i);
        } catch (Exception e) {
            toast(c, "No app found to open the map.");
        }
    }
}
