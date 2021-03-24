package com.dji.simulatorDemo;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Build;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.lang.Math;

import dji.common.error.DJISDKError;
import dji.common.flightcontroller.simulator.InitializationData;
import dji.common.flightcontroller.simulator.SimulatorState;
import dji.common.flightcontroller.virtualstick.FlightControlData;
import dji.common.flightcontroller.virtualstick.FlightCoordinateSystem;
import dji.common.flightcontroller.virtualstick.RollPitchControlMode;
import dji.common.flightcontroller.virtualstick.VerticalControlMode;
import dji.common.flightcontroller.virtualstick.YawControlMode;
import dji.common.model.LocationCoordinate2D;
import dji.common.useraccount.UserAccountState;
import dji.common.util.CommonCallbacks;
import dji.log.DJILog;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;
import dji.common.error.DJIError;
import dji.sdk.sdkmanager.DJISDKInitEvent;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.sdk.useraccount.UserAccountManager;

public class MainActivity extends Activity implements View.OnClickListener {

    private static final String TAG = MainActivity.class.getName();

    private static final String[] REQUIRED_PERMISSION_LIST = new String[]{
            Manifest.permission.VIBRATE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE,
    };
    private List<String> missingPermission = new ArrayList<>();
    private AtomicBoolean isRegistrationInProgress = new AtomicBoolean(false);
    private static final int REQUEST_PERMISSION_CODE = 12345;

    private FlightController mFlightController;
    protected TextView mConnectStatusTextView;
    private ToggleButton mBtnSimulator;
    private Button mBtnTakeOff;
    private Button mBtnLand;
    private ToggleButton mSwtcEnableVirtualStick;

    private TextView mTextView;
    private TextView mTextViewIP;

    private float mPitch;
    private float mRoll;
    private float mYaw;
    private float mThrottle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkAndRequestPermissions();
        setContentView(R.layout.activity_main);

        initUI();

        // Register the broadcast receiver for receiving the device connection's changes.
        IntentFilter filter = new IntentFilter();
        filter.addAction(DJISimulatorApplication.FLAG_CONNECTION_CHANGE);
        registerReceiver(mReceiver, filter);

        // Thread per gestire i msg del socket
        Thread myThread = new Thread(new MyServerThread());
        myThread.start();
    }

    /**
     * Checks if there is any missing permissions, and
     * requests runtime permission if needed.
     */
    private void checkAndRequestPermissions() {
        // Check for permissions
        for (String eachPermission : REQUIRED_PERMISSION_LIST) {
            if (ContextCompat.checkSelfPermission(this, eachPermission) != PackageManager.PERMISSION_GRANTED) {
                missingPermission.add(eachPermission);
            }
        }
        // Request for missing permissions
        if (!missingPermission.isEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,
                    missingPermission.toArray(new String[missingPermission.size()]),
                    REQUEST_PERMISSION_CODE);
        }

    }

    /**
     * Result of runtime permission request
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Check for granted permission and remove from missing list
        if (requestCode == REQUEST_PERMISSION_CODE) {
            for (int i = grantResults.length - 1; i >= 0; i--) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    missingPermission.remove(permissions[i]);
                }
            }
        }
        // If there is enough permission, we will start the registration
        if (missingPermission.isEmpty()) {
            startSDKRegistration();
        } else {
            showToast("Missing permissions!!!");
        }
    }

    private void startSDKRegistration() {
        if (isRegistrationInProgress.compareAndSet(false, true)) {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    showToast("registering, pls wait...");
                    DJISDKManager.getInstance().registerApp(getApplicationContext(), new DJISDKManager.SDKManagerCallback() {
                        @Override
                        public void onRegister(DJIError djiError) {
                            if (djiError == DJISDKError.REGISTRATION_SUCCESS) {
                                DJILog.e("App registration", DJISDKError.REGISTRATION_SUCCESS.getDescription());
                                DJISDKManager.getInstance().startConnectionToProduct();
                                showToast("Register Success");
                            } else {
                                showToast("Register sdk fails, check network is available");
                            }
                            Log.v(TAG, djiError.getDescription());
                        }

                        @Override
                        public void onProductDisconnect() {
                            Log.d(TAG, "onProductDisconnect");
                            showToast("Product Disconnected");

                        }

                        @Override
                        public void onProductConnect(BaseProduct baseProduct) {
                            Log.d(TAG, String.format("onProductConnect newProduct:%s", baseProduct));
                            showToast("Product Connected");

                        }

                        @Override
                        public void onProductChanged(BaseProduct baseProduct) {

                        }

                        @Override
                        public void onComponentChange(BaseProduct.ComponentKey componentKey, BaseComponent oldComponent,
                                                      BaseComponent newComponent) {

                            if (newComponent != null) {
                                newComponent.setComponentListener(new BaseComponent.ComponentListener() {

                                    @Override
                                    public void onConnectivityChange(boolean isConnected) {
                                        Log.d(TAG, "onComponentConnectivityChanged: " + isConnected);
                                    }
                                });
                            }
                            Log.d(TAG,
                                    String.format("onComponentChange key:%s, oldComponent:%s, newComponent:%s",
                                            componentKey,
                                            oldComponent,
                                            newComponent));

                        }

                        @Override
                        public void onInitProcess(DJISDKInitEvent djisdkInitEvent, int i) {

                        }

                        @Override
                        public void onDatabaseDownloadProgress(long l, long l1) {

                        }
                    });
                }
            });
        }
    }

    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            updateTitleBar();
        }
    };

    public void showToast(final String msg) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateTitleBar() {
        if (mConnectStatusTextView == null) return;
        boolean ret = false;
        BaseProduct product = DJISimulatorApplication.getProductInstance();
        if (product != null) {
            if (product.isConnected()) {
                //The product is connected
                mConnectStatusTextView.setText(DJISimulatorApplication.getProductInstance().getModel() + " Connected");
                ret = true;
            } else {
                if (product instanceof Aircraft) {
                    Aircraft aircraft = (Aircraft) product;
                    if (aircraft.getRemoteController() != null && aircraft.getRemoteController().isConnected()) {
                        // The product is not connected, but the remote controller is connected
                        mConnectStatusTextView.setText("only RC Connected");
                        ret = true;
                    }
                }
            }
        }

        if (!ret) {
            // The product or the remote controller are not connected.
            mConnectStatusTextView.setText("Disconnected");
        }
    }

    @Override
    public void onResume() {
        Log.e(TAG, "onResume");
        super.onResume();
        updateTitleBar();
        initFlightController();
        loginAccount();

    }

    @Override
    public void onPause() {
        Log.e(TAG, "onPause");
        super.onPause();
    }

    @Override
    public void onStop() {
        Log.e(TAG, "onStop");
        super.onStop();
    }

    public void onReturn(View view) {
        Log.e(TAG, "onReturn");
        this.finish();
    }

    @Override
    protected void onDestroy() {
        Log.e(TAG, "onDestroy");
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    private void loginAccount() {

        UserAccountManager.getInstance().logIntoDJIUserAccount(this,
                new CommonCallbacks.CompletionCallbackWith<UserAccountState>() {
                    @Override
                    public void onSuccess(final UserAccountState userAccountState) {
                        Log.e(TAG, "Login Success");
                    }

                    @Override
                    public void onFailure(DJIError error) {
                        showToast("Login Error:"
                                + error.getDescription());
                    }
                });
    }

    // iniz. il controllore di volo
    private void initFlightController() {

        Aircraft aircraft = DJISimulatorApplication.getAircraftInstance();
        if (aircraft == null || !aircraft.isConnected()) { // check if the aircraft is not null and is connected
            showToast("Disconnected");
            mFlightController = null;
            return;
        } else {
            mFlightController = aircraft.getFlightController(); // ottengo oggetto per controllare il drone
            mFlightController.setRollPitchControlMode(RollPitchControlMode.VELOCITY);
            mFlightController.setYawControlMode(YawControlMode.ANGULAR_VELOCITY);
            mFlightController.setVerticalControlMode(VerticalControlMode.VELOCITY);
            mFlightController.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);

            // callback per il simulatore che ad ogni aggiornamento degli stati li printa tramite
            // textView
            mFlightController.getSimulator().setStateCallback(new SimulatorState.Callback() {
                @Override
                public void onUpdate(final SimulatorState stateData) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {

                            String yaw = String.format("%.2f", stateData.getYaw());
                            String pitch = String.format("%.2f", stateData.getPitch());
                            String roll = String.format("%.2f", stateData.getRoll());
                            String positionX = String.format("%.2f", stateData.getPositionX());
                            String positionY = String.format("%.2f", stateData.getPositionY());
                            String positionZ = String.format("%.2f", stateData.getPositionZ());

                            mTextView.setText("Yaw : " + yaw + ", Pitch : " + pitch + ", Roll : " + roll + "\n" + ", PosX : " + positionX +
                                    ", PosY : " + positionY +
                                    ", PosZ : " + positionZ);
                        }
                    });
                }
            });
        }
    }

    private void initUI() {

        mBtnTakeOff = (Button) findViewById(R.id.btn_take_off); // take off
        mBtnLand = (Button) findViewById(R.id.btn_land); // land
        mBtnSimulator = (ToggleButton) findViewById(R.id.btn_start_simulator); // per fare tutto in simulazione
        mTextView = (TextView) findViewById(R.id.textview_simulator); // element to show the simulator state infos
        mTextViewIP = (TextView) findViewById(R.id.textview_ip_addr); //ip address of the server android
        mConnectStatusTextView = (TextView) findViewById(R.id.ConnectStatusTextView);
        mSwtcEnableVirtualStick = (ToggleButton) findViewById(R.id.swtc_enable_virtual_stick); //enable/disable Virtual Control Mode

        //Show on screen in a TextView widget the ip address of the Android device
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        String ipAddress = Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());
        mTextViewIP.setText("Your Device IP Address: " + ipAddress);

        mBtnTakeOff.setOnClickListener(this);
        mBtnLand.setOnClickListener(this);

        // Setta parametri iniziali della simulazione (starting point ....)
        mBtnSimulator.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {

                    mTextView.setVisibility(View.VISIBLE);

                    if (mFlightController != null) {

                        mFlightController.getSimulator()
                                .start(InitializationData.createInstance(new LocationCoordinate2D(23, 113), 10, 10),
                                        new CommonCallbacks.CompletionCallback() {
                                            @Override
                                            public void onResult(DJIError djiError) {
                                                if (djiError != null) {
                                                    showToast(djiError.getDescription());
                                                } else {
                                                    showToast("Start Simulator Success");
                                                }
                                            }
                                        });
                    }

                } else {

                    mTextView.setVisibility(View.INVISIBLE);

                    if (mFlightController != null) {
                        mFlightController.getSimulator()
                                .stop(new CommonCallbacks.CompletionCallback() {
                                          @Override
                                          public void onResult(DJIError djiError) {
                                              if (djiError != null) {
                                                  showToast(djiError.getDescription());
                                              } else {
                                                  showToast("Stop Simulator Success");
                                              }
                                          }
                                      }
                                );
                    }
                }
            }
        });

        //Enable/disable of virtual control mode
        mSwtcEnableVirtualStick.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mFlightController != null) {
                    mFlightController.setVirtualStickModeEnabled(isChecked, new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            if (djiError != null) {
                                showToast(djiError.getDescription());
                            } else {
                                if (mSwtcEnableVirtualStick.isChecked()) {
                                    showToast("VCM Enable Success");
                                } else {
                                    showToast("VCM Disable Success");
                                }
                            }
                        }
                    });
                }
            }
        });
    }

    /////// OVERRIDE DEL METODO onTouch, che viene chiamato quando tocco gli stick (per i 2 stick)  //////////////
    // I metodi qui overraidati mettonono i valori degli stick nelle variabili globali mPitch, mRoll, mYaw
    // and mThrottle.
    // GESTIONE BOTTONI
    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.btn_take_off:
                if (mFlightController != null) {
                    mFlightController.startTakeoff(
                            new CommonCallbacks.CompletionCallback() {
                                @Override
                                public void onResult(DJIError djiError) {
                                    if (djiError != null) {
                                        showToast(djiError.getDescription());
                                    } else {
                                        showToast("Take off Success");
                                    }
                                }
                            }
                    );
                }

                break;

            case R.id.btn_land:
                if (mFlightController != null) {

                    mFlightController.startLanding(
                            new CommonCallbacks.CompletionCallback() {
                                @Override
                                public void onResult(DJIError djiError) {
                                    if (djiError != null) {
                                        showToast(djiError.getDescription());
                                    } else {
                                        showToast("Start Landing");
                                    }
                                }
                            }
                    );

                }

                break;

            default:
                break;
        }
    }

    //Socket Server
    class MyServerThread implements Runnable {
        Socket socket;
        ServerSocket server;
        InputStreamReader isr;
        BufferedReader buffer;
        String msg;

        // target position coordinates
        float targetX;
        float targetY;

        double euclideanDistance;

        JSONObject jObj;


        @Override
        public void run() {

            try {
                server = new ServerSocket(8080);

                while (true) {

                    // wait for a new message from the client
                    socket = server.accept();

                    // store the message and save the value in its field
                    isr = new InputStreamReader(socket.getInputStream());
                    buffer = new BufferedReader(isr);
                    msg = buffer.readLine();
                    jObj = new JSONObject(msg);

                    //We store the target position coordinates into two different variables
                    targetX = (float) jObj.getDouble("target_x");
                    targetY = (float) jObj.getDouble("target_y");

                    // The euclidean distance between the body frame of the drone and the target point is computed
                    // In this way is it possible to use it to compute the linear velocity along the Roll axis
                    // Pitch velocity is setted to zero
                    // We use atan2 to compute the angular velocity along the yaw axis inside computeAnguarVelocity to let the drone rotate
                    euclideanDistance = distanceFromTargetPos(targetX, targetY);
                    mPitch = 0;
                    mRoll = computeLinearVelocity(euclideanDistance);
                    mYaw = 15 * computeAnguarVelocity(targetX, targetY);
                    mThrottle = 0;

                    //Each time a new targeg position is received the velocities are sent to the FlightController
                    if (mFlightController != null) {
                        // metodo che manda le variabili globali (Salvate nell'oggetto FlightControlData) al mFlightController
                        mFlightController.sendVirtualStickFlightControlData(
                                new FlightControlData( // oggetto descritto da queste tre variabili
                                        mPitch, mRoll, mYaw, mThrottle
                                ), new CommonCallbacks.CompletionCallback() {
                                    @Override
                                    public void onResult(DJIError djiError) {

                                    }
                                }
                        );
                    }

                }

            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
        }

        //y is the horizontal side axis
        //x is the frontal axis
        public float computeAnguarVelocity(float x, float y) {
            return (float) (((Math.PI / 2) - Math.atan2(targetX, targetY)) / Math.PI);
        }

        // The normal linear velocity is proportional to the distance and it is saturated to 1 if necessary.
        public float computeLinearVelocity(double dist) {
            return (float) satNormalVelocity(dist / 10);
        }

        public double satNormalVelocity(double vel) {
            if (vel > 1) {
                return 1;
            } else if (vel < -1) {
                return -1;
            }
            return vel;
        }

        // Euclidean distance between the body frame and the target position
        public double distanceFromTargetPos(float targX, float targY) {
            return Math.sqrt(Math.pow(targX, 2) + Math.pow(targY, 2));
        }
    }
}
