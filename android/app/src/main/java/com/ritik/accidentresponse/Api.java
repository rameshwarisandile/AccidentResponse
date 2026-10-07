package com.ritik.accidentresponse;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Tiny HTTP client for the Python (Flask) backend. Callbacks run on the main thread. */
public class Api {
    public interface Callback {
        void onResult(boolean ok, JSONObject data, String error);
    }

    private static final ExecutorService EXEC = Executors.newFixedThreadPool(3);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public static JSONObject obj(Object... kv) {
        JSONObject o = new JSONObject();
        try {
            for (int i = 0; i + 1 < kv.length; i += 2) o.put((String) kv[i], kv[i + 1]);
        } catch (Exception ignored) {
        }
        return o;
    }

    public static void call(Context ctx, String method, String path, JSONObject body, Callback cb) {
        final Context app = ctx.getApplicationContext();
        EXEC.execute(() -> {
            boolean ok = false;
            JSONObject data = null;
            String error = null;
            HttpURLConnection c = null;
            try {
                URL url = new URL(Session.baseUrl(app) + "/api/v1" + path);
                c = (HttpURLConnection) url.openConnection();
                c.setConnectTimeout(8000);
                c.setReadTimeout(12000);
                c.setRequestMethod(method);
                c.setRequestProperty("Accept", "application/json");
                String token = Session.token(app);
                if (!token.isEmpty()) c.setRequestProperty("Authorization", "Bearer " + token);
                if (body != null) {
                    c.setDoOutput(true);
                    c.setRequestProperty("Content-Type", "application/json");
                    OutputStream os = c.getOutputStream();
                    os.write(body.toString().getBytes(StandardCharsets.UTF_8));
                    os.close();
                }
                int code = c.getResponseCode();
                InputStream is = code >= 400 ? c.getErrorStream() : c.getInputStream();
                String text = read(is);
                data = text.isEmpty() ? new JSONObject() : new JSONObject(text);
                if (code >= 400) {
                    JSONObject err = data.optJSONObject("error");
                    error = err != null ? err.optString("message", "Request failed") : "Request failed (" + code + ")";
                    if (code == 401 && !token.isEmpty()) {
                        Session.clear(app);
                        error = "Session expired. Please sign in again.";
                    }
                } else {
                    ok = true;
                }
            } catch (java.io.IOException ex) {
                error = "Cannot reach server. Check the server address and that phone and PC are on the same WiFi.";
            } catch (Exception ex) {
                error = "Unexpected response from server.";
            } finally {
                if (c != null) c.disconnect();
            }
            final boolean fOk = ok;
            final JSONObject fData = data;
            final String fErr = error;
            MAIN.post(() -> cb.onResult(fOk, fData, fErr));
        });
    }

    private static String read(InputStream is) throws java.io.IOException {
        if (is == null) return "";
        BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) sb.append(line);
        r.close();
        return sb.toString();
    }
}
