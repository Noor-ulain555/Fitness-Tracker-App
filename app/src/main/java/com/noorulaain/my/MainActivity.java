package com.noorulaain.my;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView stepCountTv, time, distance, stepCountTargetTv, caloriesBurnedTv;
    private Button pause, resetButton;
    private static final String ACTION_STEP_COUNT_UPDATED = "steptracker.ACTION_STEP_COUNT_UPDATED";

    private float userWeight = 70.0f;
    private float MET = 0.035f;

    private int stepCount = 0;
    private ProgressBar progressBar;
    private boolean isPaused = false;
    private long timePaused = 0;
    private float stepLengthInMeters = 0.762f;
    private long startTime;
    private StepTrackerService stepTrackerService;
    private boolean isBound = false;
    private static final String PREFS_NAME = "StepTrackerPrefs";
    private static final String STEP_COUNT_KEY = "StepCount";

    private int stepCountTarget = 2000;
    private Handler timeHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        resetButton = findViewById(R.id.resetButton);
        stepCountTv = findViewById(R.id.stepCountTv);
        time = findViewById(R.id.TimeCountTv);
        distance = findViewById(R.id.DistanceCountTv);
        pause = findViewById(R.id.pausebtn);
        stepCountTargetTv = findViewById(R.id.tvTargetcounterstep);
        progressBar = findViewById(R.id.PB);
        caloriesBurnedTv = findViewById(R.id.CaloriesBurnedTv);

        progressBar.setMax(stepCountTarget);
        stepCountTargetTv.setText("Step Goal: " + stepCountTarget);

        Intent serviceIntent = new Intent(this, StepTrackerService.class);
        ContextCompat.startForegroundService(this, serviceIntent);

        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onResetButtonClicked(v);
            }
        });
    }

    private Runnable timeRunnable = new Runnable() {
        @Override
        public void run() {
            long millis = System.currentTimeMillis() - startTime;
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;
            time.setText(String.format(Locale.getDefault(), "Time: %02d:%02d", minutes, seconds));
            timeHandler.postDelayed(this, 1000);
        }
    };
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            StepTrackerService.StepTrackerBinder binder = (StepTrackerService.StepTrackerBinder) service;
            stepTrackerService = binder.getService();
            isBound = true;
            stepCount = stepTrackerService.getCurrentStepCount();
            updateUI();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };
    private BroadcastReceiver stepCountReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(StepTrackerService.ACTION_STEP_COUNT_UPDATED)) {
                stepCount = intent.getIntExtra(STEP_COUNT_KEY, 0);
                updateUI();
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = new Intent(this, StepTrackerService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);

        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        stepCount = sharedPreferences.getInt(STEP_COUNT_KEY, 0);
        updateUI();

        if (!isPaused) {
            startTime = System.currentTimeMillis() - timePaused;
            timeHandler.postDelayed(timeRunnable, 0);
        }

        IntentFilter filter = new IntentFilter(ACTION_STEP_COUNT_UPDATED);
        registerReceiver(stepCountReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(stepCountReceiver);
    }

    @Override
    protected void onStop() {
        super.onStop();
        timeHandler.removeCallbacks(timeRunnable);
        if (isBound) {
            unbindService(connection);
            isBound = false;
        }
    }
    //    @Override
//    protected void onResume() {
//        super.onResume();
//        Intent intent = new Intent(this, StepTrackerService.class);
//        bindService(intent, connection, Context.BIND_AUTO_CREATE);
//
//        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
//        stepCount = sharedPreferences.getInt(STEP_COUNT_KEY, 0);
//        updateUI();
//
//        if (!isPaused) {
//            startTime = System.currentTimeMillis() - timePaused;
//            timeHandler.postDelayed(timeRunnable, 0);
//        }
//    }
    public void onResetButtonClicked(View view) {
        if (isBound && stepTrackerService != null) {
            stepTrackerService.resetStepCount();
            stepCount = 0; // Reset the step count in MainActivity as well
            startTime = System.currentTimeMillis(); // Reset the start time
            timePaused = 0; // Reset paused time
            updateUI();
        }
    }

    private void updateUI() {
        stepCountTv.setText("Step count: " + stepCount);
        progressBar.setProgress(stepCount);
        if (stepCount >= stepCountTarget) {
            stepCountTargetTv.setText("Step Goal Achieved");
        }else {
            stepCountTargetTv.setText("Step Goal: " + stepCountTarget);
        }
        float distanceInKm = stepCount * stepLengthInMeters / 1000;
        distance.setText(String.format(Locale.getDefault(), "Distance: %.2f Km", distanceInKm));

        float caloriesBurned = (stepCount * stepLengthInMeters * userWeight * MET) / 1000;
        caloriesBurnedTv.setText(String.format(Locale.getDefault(), "Calories burned: %.2f", caloriesBurned));
    }
    public void OnPauseButtonClicked(View view) {
        if (isPaused) {
            isPaused = false;
            pause.setText("Pause");
            startTime = System.currentTimeMillis() - timePaused;
            timeHandler.postDelayed(timeRunnable, 0);
        } else {
            isPaused = true;
            pause.setText("Resume");
            timeHandler.removeCallbacks(timeRunnable);
            timePaused = System.currentTimeMillis() - startTime;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("stepCount", stepCount);
        outState.putLong("startTime", startTime);
        outState.putBoolean("isPaused", isPaused);
        outState.putLong("timePaused", timePaused);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        stepCount = savedInstanceState.getInt("stepCount");
        startTime = savedInstanceState.getLong("startTime");
        isPaused = savedInstanceState.getBoolean("isPaused");
        timePaused = savedInstanceState.getLong("timePaused");
        updateUI();
        if (isPaused) {
            timeHandler.removeCallbacks(timeRunnable);
            pause.setText("Resume");

        } else {
            timeHandler.postDelayed(timeRunnable, 0);
            pause.setText("Pause");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(connection);
            isBound = false;
        }
    }
}


