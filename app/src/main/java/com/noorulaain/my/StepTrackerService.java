package com.noorulaain.my;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

public class StepTrackerService extends Service implements SensorEventListener {
    public static final String ACTION_STEP_COUNT_UPDATED = "steptracker.ACTION_STEP_COUNT_UPDATED";

    private static final String CHANNEL_ID = "StepTrackerChannel";
    private static final String PREFS_NAME = "StepTrackerPrefs";
    private static final String STEP_COUNT_KEY = "StepCount";
    private SensorManager sensorManager;
    private Sensor stepCounterSensor;
    private int stepCount = 0;
    private int initialStepCount = -1;

    @Override
    public void onCreate() {
        super.onCreate();

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        if (stepCounterSensor != null) {
            sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

        createNotificationChannel();
        startForeground(1, getNotification("Steps counted: " + stepCount));
    }

    private Notification getNotification(String text) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Step Tracker")
                .setContentText(text)
                .setSmallIcon(R.drawable.walking)
                .setContentIntent(pendingIntent)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Step Tracker Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            if (initialStepCount == -1) {
                initialStepCount = (int) event.values[0];
            }
            stepCount = (int) event.values[0] - initialStepCount;

            SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(STEP_COUNT_KEY, stepCount);
            editor.apply();

            updateNotification("Steps counted: " + stepCount);

            // Send a broadcast to notify about the step count update
            Intent intent = new Intent(ACTION_STEP_COUNT_UPDATED);
            intent.putExtra(STEP_COUNT_KEY, stepCount);
            sendBroadcast(intent);
        }
    }

//
//    @Override
//    public void onSensorChanged(SensorEvent event) {
//        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
//            if (initialStepCount == -1) {
//                initialStepCount = (int) event.values[0];
//            }
//            stepCount = (int) event.values[0] - initialStepCount;
//
//            SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
//            SharedPreferences.Editor editor = sharedPreferences.edit();
//            editor.putInt(STEP_COUNT_KEY, stepCount);
//            editor.apply();
//
//            updateNotification("Steps counted: " + stepCount);
//        }
//    }

    public int getCurrentStepCount() {
        return stepCount;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public IBinder onBind(Intent intent) {
        return new StepTrackerBinder();
    }

    public class StepTrackerBinder extends Binder {
        StepTrackerService getService() {
            return StepTrackerService.this;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(this);
    }

    public void updateNotification(String text) {
        Notification notification = getNotification(text);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, notification);
    }

    public void resetStepCount() {
        initialStepCount = -1;
        stepCount = 0;
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(STEP_COUNT_KEY, stepCount);
        editor.apply();
        updateNotification("Steps counted: " + stepCount);
    }
}



