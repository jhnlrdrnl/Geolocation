package com.example.geolocation;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class LocationService extends Service {

    private final SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss a, dd/MM/yyyy", Locale.getDefault());
    private WebSocket webSocket;
    private String SERVER_PATH;
    private Boolean isConnected = false;
    private Boolean closedByUser = false;
    private LocalService localService;
    private OkHttpClient client;

    private final LocationCallback locationCallback = new LocationCallback() {

        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);

            if (locationResult != null && locationResult.getLastLocation() != null) {
                double dLatitude = locationResult.getLastLocation().getLatitude();
                double dLongitude = locationResult.getLastLocation().getLongitude();

                String wholeData = dLatitude + ", " + dLongitude + ", " + getAddressName(dLatitude, dLongitude) + ", " + df.format(new Date());

                if (isConnected) {
                    webSocket.send(wholeData);
                    Log.d("Geolocation Server", "Attempting to Send Location to Server: " + wholeData);
                }
                else {
                    Log.d("Geolocation Local", "Attempting to Send Location to Local: " + wholeData);
                    localService = new LocalService(getApplicationContext());
                    localService.insertData(wholeData);
                }
            }
        }
    };

    private void startLocationService() {
        String channelId = "location_notification_channel";
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Intent resultIntent = new Intent();

        PendingIntent pendingIntent = PendingIntent.getActivity(
                getApplicationContext(),
                0,
                resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), channelId)
                .setContentTitle("Geolocation")
                .setContentText("Foreground Service is Running")
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .setAutoCancel(false)
                .setPriority(NotificationCompat.PRIORITY_LOW);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notificationManager != null && notificationManager.getNotificationChannel(channelId) == null) {
                NotificationChannel notificationChannel = new NotificationChannel(
                        channelId,
                        "Location Service",
                        NotificationManager.IMPORTANCE_LOW
                );
                notificationChannel.setDescription("This channel is used by another location service");
                notificationManager.createNotificationChannel(notificationChannel);
            }
        }

        LocationRequest locationRequest = LocationRequest.create()
                .setInterval(5000)
                .setFastestInterval(2500)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) getApplicationContext(), new String[]{ Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION }, 11);
        }

        LocationServices.getFusedLocationProviderClient(this).requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        startForeground(Constants.LOCATION_SERVICE_ID, builder.build());
    }

    private void stopLocationService() {
        LocationServices.getFusedLocationProviderClient(this)
                .removeLocationUpdates(locationCallback);
        stopForeground(true);
        stopSelf();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            SERVER_PATH = intent.getStringExtra("server");
            if (action != null) {
                if (action.equals(Constants.ACTION_START_LOCATION_SERVICE)) {
                    initiateSocketConnection();
                    startLocationService();
                }
                else if (action.equals(Constants.ACTION_STOP_LOCATION_SERVICE)) {
                    closedByUser = true;
                    disconnectSocketConnection();
                    stopLocationService();
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void initiateSocketConnection() {
        if (!isConnected) {
            client = new OkHttpClient()
                    .newBuilder()
                    .pingInterval(5, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true)
                    .build();

            Request request = new Request
                    .Builder()
                    .url(SERVER_PATH)
                    .build();

            webSocket = client.newWebSocket(request, new SocketListener());
        }
    }

    private void disconnectSocketConnection() {
        if (isConnected) {
            webSocket.close(Constants.NORMAL_CLOSURE_STATUS, "Socket Connection Closing");
        }
    }

    private void reconnectSocketConnection(WebSocket webSocket) {
        webSocket.close(Constants.NORMAL_CLOSURE_STATUS, null);
        try {
            Thread.sleep(3_000);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        initiateSocketConnection();
    }

    public class SocketListener extends WebSocketListener {

        @Override
        public void onOpen( WebSocket webSocket,  Response response) {
            super.onOpen(webSocket, response);

            isConnected = true;
            Log.d("Geolocation", "Socket onOpen()");
        }

        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            super.onClosing(webSocket, code, reason);

            isConnected = false;
            Log.d("Geolocation", "Socket onClosing()");
        }

        @Override
        public void onClosed( WebSocket webSocket, int code,  String reason) {
            super.onClosed(webSocket, code, reason);

            isConnected = false;
            Log.d("Geolocation", "Socket onClosed()");
        }

        @Override
        public void onFailure( WebSocket webSocket,  Throwable t, Response response) {
            super.onFailure(webSocket, t, response);

            isConnected = false;
            closedByUser = false;

            if (!closedByUser) {
                reconnectSocketConnection(webSocket);
                Log.e("Geolocation", "Socket onFailure(). Attempting to Reconnect");
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onDestroy() {
        stopLocationService();
    }

    private String getAddressName(double latitude, double longitude) {
        String myAddress = "";
        Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());

        try {
            List<Address> addressList = geocoder.getFromLocation(latitude, longitude, 1);
            myAddress = addressList.get(0).getAddressLine(0);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return myAddress;
    }
}
