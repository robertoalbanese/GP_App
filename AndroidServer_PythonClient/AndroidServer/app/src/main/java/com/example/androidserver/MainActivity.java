package com.example.androidserver;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Context context = getApplicationContext();
        // NetClient nc = new NetClient(host, port, mac);
        // String r = nc.receiveDataFromServer();
        Thread myThread = new Thread(new MyServerThread());
        myThread.start();
    }

    class MyServerThread implements Runnable{
        Socket socket;
        ServerSocket server;
        InputStreamReader isr;
        BufferedReader buffer;
        Handler h = new Handler();
        String msg;
        TextView msgView = (TextView) findViewById(R.id.msg);

        JSONObject jObj;



        @Override
        public void run() {
            try
            {
                server = new ServerSocket(8080);
                while(true){
                    socket = server.accept();
                    isr = new InputStreamReader(socket.getInputStream());
                    buffer = new BufferedReader(isr);
                    msg = buffer.readLine();
                    jObj = new JSONObject( msg);
                    int pitch = jObj.getInt("pitch");
                    msg = Integer.toString(pitch);


                    h.post(new Runnable() {
                        @Override
                        public void run() {
                            msgView.setText(msg);
                        }
                    });
                }
            }catch(IOException | JSONException e){
                e.printStackTrace();
            }
        }
    }
}