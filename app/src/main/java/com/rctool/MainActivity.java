package com.rctool;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Set;

//import android.support.constraint.ConstraintLayout;

public class MainActivity extends AppCompatActivity {
    private final static int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter bluetooth;
    private BTConnection connection;
    private PowerManager.WakeLock wakeLock;
    private OnTouchListener mouseListener = new OnTouchListener() {
        public boolean onTouch(View v, MotionEvent event) {
            final float xPixel = event.getX();
            final float yPixel = event.getY();

            DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager()
                    .getDefaultDisplay()
                    .getMetrics(displayMetrics);
            final int width = displayMetrics.widthPixels;
            final int height = displayMetrics.heightPixels;
            float padding = 100;

            float x = (xPixel - padding) / (width - padding * 2);
            float y = (yPixel - padding) / (height - padding * 2);
            if (connection != null) {
                connection.setXY(x, y);
            }

            TextView textView = findViewById(R.id.text_output);
            textView.setText("onTouch " + x + " " + y + " " + width + " " + height);
            return true;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "com.rctool:wakeLock");
        wakeLock.acquire();
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        bluetooth = BluetoothAdapter.getDefaultAdapter();
        if (bluetooth != null) {
            Log.d("hello", "BluetoothAdapter on");
            // С Bluetooth все в порядке.
        } else {
            Log.d("hello", "BluetoothAdapter off");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        // String status;
        // if (bluetooth.isEnabled()){
        //     status = bluetooth.getName() + " : " + bluetooth.getAddress();
        // } else {
        //     status = "Bluetooth выключен";
        // }

        ConstraintLayout constraintLayout = (ConstraintLayout)findViewById(R.id.main);
        // Register the onClick listener with the implementation above
        constraintLayout.setOnTouchListener(mouseListener);
//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_connection) {
            Set<BluetoothDevice> pairedDevices = bluetooth.getBondedDevices();
            final String[] items = new String[pairedDevices.size()];
            final String[] addresMap = new String[pairedDevices.size()];
            if (pairedDevices.size() > 0){
                int index = 0;
                for (BluetoothDevice device: pairedDevices){
                    items[index] = device.getName() + " - " + device.getAddress();
                    addresMap[index] = device.getAddress();
                    index++;
                }
            }

            selectBluetoothDevice(items, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    if (connection == null) {
                        FloatingActionButton statusButton = findViewById(R.id.statusButton);
                        connection = new BTConnection(bluetooth, statusButton);
                        connection.setListener(new BTConnection.ConnectionStateListener() {
                            @Override
                            public void onConnect() {
                                FloatingActionButton statusButton = findViewById(R.id.statusButton);
                                statusButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.colorSuccess)));
                            }

                            @Override
                            public void onDisconnect() {
                                FloatingActionButton statusButton = findViewById(R.id.statusButton);
                                statusButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.colorError)));
                            }
                        });
                        connection.setDeviceName(addresMap[which]);
                        connection.start();
                    }
                }
            });
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void selectBluetoothDevice(String[] items, final DialogInterface.OnClickListener listener) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle(R.string.select_device);
        dialogBuilder.setItems(items, listener);
        dialogBuilder.create().show();
    }
}
