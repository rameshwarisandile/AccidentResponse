# AccidentResponse - Python Backend + Android App

Do folders hain:
- `backend/`  -> Python (Flask) API + SQLite database (koi setup nahi)
- `android/`  -> Android Studio project (Java). Isse APK banegi.

## STEP 1: Backend chalao (laptop par, 2 minute)
```
cd backend
pip install -r requirements.txt
python app.py
```
Server `http://0.0.0.0:5000` par chalega. Test: browser me `http://localhost:5000/health` kholo -> {"status":"ok"}.
(Optional test: `python test_backend.py` -> "ALL BACKEND TESTS PASSED")

Windows firewall popup aaye to "Allow access" (Private networks) dabao.

## STEP 2: Laptop ka IP nikalo
Windows: `ipconfig` -> "IPv4 Address" (jaise 192.168.1.5).
Server address = `http://192.168.1.5:5000`

IMPORTANT: Phone aur laptop SAME WiFi par hone chahiye.
College/office WiFi aksar devices ko block karta hai. Safe tarika: phone ka HOTSPOT on karo,
laptop ko us hotspot se connect karo, phir `ipconfig` se IP lo.

## STEP 3: APK banao (Android Studio)
1. Android Studio -> File > Open -> `android` folder chuno. Gradle sync hone do (pehli baar 5-10 min, internet chahiye).
   Agar "Upgrade Android Gradle Plugin" puche to "Update" dabao.
2. Menu: Build > Build Bundle(s) / APK(s) > Build APK(s)
3. Ban jaye to "locate" dabao. File: `android/app/build/outputs/apk/debug/app-debug.apk`
4. APK phone me bhejo (WhatsApp/USB/Drive) aur install karo ("Install unknown apps" allow karna padega).
Emulator me chalana ho to server address `http://10.0.2.2:5000` (default wahi hai).

## STEP 4: App me
1. App kholo -> "Server address" me `http://<laptop-ip>:5000` daalo.
2. "New here? Create Account" -> register karo -> dashboard khulega.
3. Emergency Contacts me 2 contacts add karo.

## DEMO SCRIPT (kal dikhane ke liye)
1. Dashboard dikhao: "System Safe", 0 events.
2. Emergency Contacts: contact add karo, primary set karo.
3. Dashboard par "Simulate Safety Unit Event (Demo)" -> "Confirmed Accident".
   -> Phone vibrate karega, "Emergency Detected" popup aayega, status card laal ho jayega.
   -> Laptop ke terminal me dikhega ki contacts ko SMS bheja gaya (console log: `[NOTIFY] -> ...`).
4. Live Location: coordinates + "Open in Google Maps".
5. Event History -> event kholo -> Share Report / Mark as Resolved.
6. Profile: Emergency Alerts switch band karo, phir accident simulate karo -> koi notification nahi jayega.

## Asli ESP32 se connect karna
Register ke baad device key chahiye. App me nahi dikhti, isliye:
```
# 1) login karke token lo
curl -X POST http://IP:5000/api/v1/auth/login -H "Content-Type: application/json" -d "{\"email\":\"you@mail.com\",\"password\":\"yourpass\"}"
# 2) token se naya device key lo
curl -X POST http://IP:5000/api/v1/devices/rotate-key -H "Authorization: Bearer <TOKEN>"
# 3) ESP32 ye bhejega (location)
curl -X POST http://IP:5000/api/v1/device/location -H "X-Device-Key: <KEY>" -H "Content-Type: application/json" -d "{\"lat\":20.59,\"lng\":78.96}"
# 4) ESP32 ye bhejega (accident)
curl -X POST http://IP:5000/api/v1/device/events -H "X-Device-Key: <KEY>" -H "Content-Type: application/json" -d "{\"event_type\":\"Confirmed Accident\",\"description\":\"Crash detected\",\"lat\":20.59,\"lng\":78.96}"
```
Valid event_type: Possible Accident, Confirmed Accident, Obstacle Detected, System Check, System Warning, Confirmed Emergency

## Abhi jo NAHI hai (baad me add kar sakte hain)
- Asli SMS (abhi sirf console log; `notify_contacts()` function me Twilio/MSG91 lag sakta hai)
- Forgot password / email verification
- Inline map (abhi "Open in Google Maps" button hai)
- PostgreSQL (abhi SQLite file `backend/accident.db`; pgAdmin se dekhna ho to PostgreSQL pe shift karna padega)
- Internet par hosting (abhi laptop par chalta hai)
