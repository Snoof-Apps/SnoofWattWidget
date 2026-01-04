package snoof.widget.watt;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.widget.RemoteViews;

public class WattWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.watt_widget);
        Intent intent = new Intent(context, WattageService.class);

        SharedPreferences prefs = context.getSharedPreferences("WattPrefs", Context.MODE_PRIVATE);

        int squareSizeDp = prefs.getInt("square_size", 100);// Pass it directly as DP
        float dynamicTextSize = squareSizeDp * 0.18f;
        float dynamicChargeSize = squareSizeDp * 0.36f;

        views.setViewLayoutWidth(R.id.squareContainer, squareSizeDp, TypedValue.COMPLEX_UNIT_DIP);
        views.setViewLayoutHeight(R.id.squareContainer, squareSizeDp, TypedValue.COMPLEX_UNIT_DIP);
        views.setTextViewTextSize(R.id.watt_text, TypedValue.COMPLEX_UNIT_DIP, dynamicTextSize);
        views.setViewLayoutWidth(R.id.charge, dynamicChargeSize, TypedValue.COMPLEX_UNIT_DIP);
        views.setViewLayoutHeight(R.id.charge, dynamicChargeSize, TypedValue.COMPLEX_UNIT_DIP);


        PendingIntent pendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            pendingIntent = PendingIntent.getForegroundService(
                    context, appWidgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        } else {
            pendingIntent = PendingIntent.getService(
                    context, appWidgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        }

        views.setOnClickPendingIntent(R.id.widgetLayout, pendingIntent);
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        // Clear the state when the last widget is removed
        SharedPreferences prefs = context.getSharedPreferences("WattPrefs", Context.MODE_PRIVATE);
        prefs.edit().putBoolean("isRunning", false).apply();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisAppWidget = new ComponentName(context.getPackageName(), WattWidgetProvider.class.getName());
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);

        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }


    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
                                          int appWidgetId, Bundle newOptions) {
        int minWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH);
        int minHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
        int squareSize = Math.min(minWidth, minHeight);

        context.getSharedPreferences("WattPrefs", Context.MODE_PRIVATE)
                .edit().putInt("square_size", squareSize).apply();

        updateAppWidget(context, appWidgetManager, appWidgetId);
    }


}