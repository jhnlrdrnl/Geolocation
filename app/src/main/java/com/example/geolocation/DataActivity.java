package com.example.geolocation;

import androidx.appcompat.app.AppCompatActivity;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class DataActivity extends AppCompatActivity {

    private ListView listView;
    private Button clearButton, sendButton;
    private ArrayList<String> arrayList;
    private ArrayAdapter arrayAdapter;
    private LocalService localService;
    private String SERVER_PATH;
    private WebSocket webSocket;
    private Boolean isConnected = false;
    private Boolean closedByUser = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data);

        SERVER_PATH = getIntent().getStringExtra("server");

        listView = findViewById(R.id.dataList);
        clearButton = findViewById(R.id.clearButton);
        sendButton = findViewById(R.id.sendButton);

        localService = new LocalService(this);
        arrayList = new ArrayList<>();

        viewData();

        listView.setOnItemClickListener((adapterView, view, i, l) -> {
            String text = listView.getItemAtPosition(i).toString();
        });

        clearButton.setOnClickListener(view -> {
            deleteData();
            arrayList.clear();
            arrayAdapter.notifyDataSetChanged();
        });

        sendButton.setOnClickListener(view -> {
            initiateSocketConnection();
            if (isConnected) {
                sendDataToServer();
                Toast.makeText(this, "All Data Sent to the WebSocket", Toast.LENGTH_SHORT).show();
            }
            else {
                Toast.makeText(this, "Sending Failed. Please try again", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void viewData() {
        Cursor cursor = localService.viewData();

        if (cursor.getCount() != 0) {
            while (cursor.moveToNext()) {
                arrayList.add(cursor.getString(0));
            }

            arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, arrayList);
            listView.setAdapter(arrayAdapter);
        }
    }

    private void deleteData() {
        localService.deleteData();
    }

    private void sendDataToServer() {
        Cursor cursor = localService.viewData();
        if (cursor.getCount() != 0) {
            while (cursor.moveToNext()) {
                webSocket.send(cursor.getString(0));
            }
        }
        closedByUser = true;
        deleteData();
        arrayList.clear();
        arrayAdapter.notifyDataSetChanged();
        webSocket.cancel();
    }

    private void initiateSocketConnection() {
        if (!isConnected) {
            OkHttpClient client = new OkHttpClient()
                    .newBuilder()
                    .pingInterval(5, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true)
                    .build();
            Request request = new Request
                    .Builder()
                    .url(SERVER_PATH)
                    .build();

            webSocket = client.newWebSocket(request, new SocketListener());
            client.connectionPool().evictAll();
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
        public void onOpen(WebSocket webSocket, Response response) {
            super.onOpen(webSocket, response);

            isConnected = true;
            Log.d("Geolocation", "Socket onOpen()");
        }

        @Override
        public void onClosed(WebSocket webSocket, int code, String reason) {
            super.onClosed(webSocket, code, reason);

            isConnected = false;
            Log.d("Geolocation", "Socket onClosed()");
        }
    }
}