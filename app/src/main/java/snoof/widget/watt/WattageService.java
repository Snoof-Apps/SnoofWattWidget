package snoof.widget.watt;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;
import android.widget.RemoteViews;
import androidx.core.app.NotificationCompat;

public class WattageService extends Service {
    private static final String CHANNEL_ID = "WattageMeasureChannel";
    private static final String PREFS_NAME = "WattPrefs";
    private static final String IS_RUNNING_KEY = "isRunning";

    private Handler handler = new Handler();
    private boolean isMeasuring = false;

    private Runnable updateTask = new Runnable() {
        @Override
        public void run() {
            if (isMeasuring) {
                updateWidgetUI();
                handler.postDelayed(this, 500);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean currentState = prefs.getBoolean(IS_RUNNING_KEY, false);
        boolean newState = !currentState;

        prefs.edit().putBoolean(IS_RUNNING_KEY, newState).apply();
        isMeasuring = newState;

        if (isMeasuring) {
            // Start the foreground loop
            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .build();

            startForeground(1, notification);
            handler.post(updateTask);
        } else {
            // Stop everything
            handler.removeCallbacks(updateTask);
            resetWidgetUI();
            stopForeground(true);
            stopSelf();
        }

        return START_NOT_STICKY;
    }

    private void updateWidgetUI() {
        BatteryManager bm = (BatteryManager) getSystemService(Context.BATTERY_SERVICE);
        Intent batteryStatus = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        if (batteryStatus == null) return;

        int voltage = batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
        long currentNow = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);

        // Power calculation
        double currentAmps = currentNow / 1000000.0;
        // Correction for devices reporting in mA
        if (Math.abs(currentAmps) > 50) currentAmps /= 1000.0;

        double watts = (voltage / 1000.0) * currentAmps;
        String display = String.format("%s%.2f W", (watts > 0 ? "+" : ""), watts);

        RemoteViews views = new RemoteViews(getPackageName(), R.layout.watt_widget);
        views.setTextViewText(R.id.watt_text, display);
        views.setViewVisibility(R.id.watt_text, View.VISIBLE);
        views.setViewVisibility(R.id.charge, View.INVISIBLE);

        pushUpdate(views);
    }

    private void resetWidgetUI() {
        RemoteViews views = new RemoteViews(getPackageName(), R.layout.watt_widget);
        views.setViewVisibility(R.id.watt_text, View.INVISIBLE);
        views.setTextViewText(R.id.watt_text, "");
        views.setViewVisibility(R.id.charge, View.VISIBLE);

        pushUpdate(views);
    }

    private void pushUpdate(RemoteViews views) {
        ComponentName widget = new ComponentName(this, WattWidgetProvider.class);
        AppWidgetManager.getInstance(this).updateAppWidget(widget, views);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Wattage Tracking", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

}