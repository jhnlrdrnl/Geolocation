package com.example.geolocation;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class MainActivity extends AppCompatActivity {

    private FloatingActionButton viewDataButton;
    private EditText serverText;
    private SwitchMaterial locationSwitch;
    private static final int REQUEST_CODE_LOCATION_PERMISSION = 1;
    private static final String DEFAULT_SERVER_PATH = "https://192.168.1.3:3000"; /* replace with your own DEFAULT_SERVER_PATH */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        locationSwitch = findViewById(R.id.locationSwitch);
        serverText = findViewById(R.id.editText);
        viewDataButton = findViewById(R.id.viewDataButton);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 999);
        }

        if (isLocationServiceRunning())
            locationSwitch.setChecked(true);

        locationSwitch.setOnClickListener(view -> {
            if (!isLocationServiceRunning()) {
                if (locationSwitch.isChecked()) {
                    if (serverText.getText().toString().equals("")) {
                        serverText.setText(DEFAULT_SERVER_PATH);
                    }
                    if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{ Manifest.permission.ACCESS_FINE_LOCATION }, REQUEST_CODE_LOCATION_PERMISSION);
                    else startLocationService();
                }
                else {
                    stopLocationService();
                }
            }
            else if (isLocationServiceRunning()) {
                if (!locationSwitch.isChecked()) {
                    stopLocationService();
                }
            }
        });

        viewDataButton.setOnClickListener(view -> {
            if (serverText.getText().toString().equals("")) {
                serverText.setText(DEFAULT_SERVER_PATH);
            }

            Intent dataIntent = new Intent(this, DataActivity.class);
            dataIntent.putExtra("server", serverText.getText().toString());
            startActivity(dataIntent);
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_LOCATION_PERMISSION && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
                startLocationService();
            }
            else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean isLocationServiceRunning() {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

        if (activityManager != null) {
            for (ActivityManager.RunningServiceInfo serviceInfo: activityManager.getRunningServices(Integer.MAX_VALUE)) {
                if (LocationService.class.getName().equals(serviceInfo.service.getClassName())) {
                    if (serviceInfo.foreground) {
                        return true;
                    }
                }
            }
            return false;
        }
        return false;
    }

    private void startLocationService() {
        if (!isLocationServiceRunning()) {
            Intent intent = new Intent(getApplicationContext(), LocationService.class);
            intent.setAction(Constants.ACTION_START_LOCATION_SERVICE);
            intent.putExtra("server", serverText.getText().toString());
            startService(intent);
            Toast.makeText(this, "Location Service Started", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopLocationService() {
        if (isLocationServiceRunning()) {
            Intent intent = new Intent(getApplicationContext(), LocationService.class);
            intent.setAction(Constants.ACTION_STOP_LOCATION_SERVICE);
            intent.putExtra("server", serverText.getText().toString());
            startService(intent);
            Toast.makeText(this, "Location Service Stopped", Toast.LENGTH_SHORT).show();
        }
    }
}