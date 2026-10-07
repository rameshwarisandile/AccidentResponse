package com.ritik.accidentresponse;

import android.content.Context;
import android.content.SharedPreferences;

/** Stores server address + login token. */
public class Session {
    private static SharedPreferences p(Context c) {
        return c.getApplicationContext().getSharedPreferences("AccidentResponseAuth", Context.MODE_PRIVATE);
    }

    /** 10.0.2.2 = your PC when using the Android emulator. On a real phone type your PC's WiFi IP. */
    public static String baseUrl(Context c) {
        return p(c).getString("base_url", "http://10.0.2.2:5000");
    }

    public static void setBaseUrl(Context c, String url) {
        url = url.trim();
        if (url.isEmpty()) return;
        while (url.endsWith("/")) url = url.substring(0, url.length() - 1);
        if (!url.startsWith("http://") && !url.startsWith("https://")) url = "http://" + url;
        p(c).edit().putString("base_url", url).apply();
    }

    public static String token(Context c) {
        return p(c).getString("token", "");
    }

    public static void setToken(Context c, String t) {
        p(c).edit().putString("token", t).apply();
    }

    public static boolean isLoggedIn(Context c) {
        return !token(c).isEmpty();
    }

    public static void clear(Context c) {
        p(c).edit().remove("token").apply();
    }
}
