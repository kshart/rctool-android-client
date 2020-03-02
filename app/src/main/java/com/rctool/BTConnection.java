package com.rctool;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class BTConnection extends Thread {
    public interface ConnectionStateListener {
        void onConnect();
        void onDisconnect();
    }

    private FloatingActionButton statusButton;
    private BluetoothAdapter bluetooth;
    private String deviceName;
    private BluetoothSocket socket;
    private BluetoothDevice device;
    private InputStream inStream = null;
    private OutputStream outStream = null;
    private ConnectionStateListener listener;

    private boolean updateXY = true;
    private float x = 0.5f;
    private float y = 0.5f;

    private int totalPacks = 0;

    public static final String SERVICE_ID = "00001101-0000-1000-8000-00805f9b34fb";

    public BTConnection(BluetoothAdapter bluetooth, FloatingActionButton statusButton) {
        this.bluetooth = bluetooth;
        this.statusButton = statusButton;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public void setListener(ConnectionStateListener listener) {
        this.listener = listener;
    }

    private void openSocket()
    {
        Log.d("bluetooth", "openSocket");
        try {
            bluetooth.enable();
            bluetooth.cancelDiscovery();
            device = bluetooth.getRemoteDevice(deviceName);
            socket = device.createRfcommSocketToServiceRecord(UUID.fromString(SERVICE_ID));
            socket.connect();

        } catch (IOException e){
            Log.e("bluetooth error", e.getLocalizedMessage());
            socket = null;
            return;
        }

        try {
            inStream = socket.getInputStream();
            outStream = socket.getOutputStream();
        } catch (IOException e){
            Log.e("bluetooth error", e.getLocalizedMessage());
            return;
        }
        onConnect();
    }

    @Override
    public void run() {
        byte[] buffer = new byte[1024];
        ByteArrayOutputStream pack = new ByteArrayOutputStream(1024);

        int byteCount;
        byte byteBefore = 0;

        while (true) {
            if (socket == null) {
                openSocket();
            }
            if (socket != null && !socket.isConnected()) {
                try {
                    Log.d("bluetooth", "connect");
                    socket.connect();
                    Log.d("bluetooth", "connected");
                } catch (IOException connectException) {
                    Log.e("bluetooth", connectException.getLocalizedMessage());
                    try {
                        socket.close();
                        socket = null;
                        onDisconnect();
                        Log.d("bluetooth", "onDisconnect");
                    } catch(IOException closeException){
                        Log.d("bluetooth", "onDisconnect error");
                        Log.e("bluetooth", closeException.getLocalizedMessage());
                    }
                }
            }
            try {
                while (inStream != null && inStream.available() > 0) {
                    Log.d("bluetooth", "readAvailable " + inStream.available());
                    byteCount = inStream.read(buffer);
                    if (byteCount > 0) {
                        Log.d("bluetooth", "read " + byteCount);
                    }
                }
            } catch(IOException e) {
                Log.e("bluetooth", e.getLocalizedMessage());
            }
            if (updateXY) {
                sendSetXY();
                updateXY = false;
            }
//            Log.d("bluetooth", "read " + byteCount);
//            Log.d("bluetooth", new String(buffer, 0, byteCount, StandardCharsets.UTF_8));
//            int startPack = 0;
//            for (int i = 0; i < byteCount; i++) {
//                if (byteBefore == '\r' && buffer[i] == '\n') {
//                    pack.write(buffer, startPack, i + 1);
//                    dispatchPackage(pack);
//                    pack.reset();
//                    startPack = i + 1;
//                }
//                byteBefore = buffer[i];
//            }
//            if (startPack < byteCount) {
//                pack.write(buffer, startPack, byteCount);
////                Log.d("bluetooth pack add", pack.toString() + " " + pack.size() + " " + startPack + " " + byteCount);
//            }
        }
    }

    public void dispatchPackage(ByteArrayOutputStream pack) {
    }

    public void write(byte[] bytes) {
        if (outStream == null) {
            return;
        }
        try {
            Log.e("bluetooth", "write " + bytes.length);
            outStream.write(bytes);
        } catch(IOException e){
            Log.e("bluetooth error", e.getLocalizedMessage());
            try {
                if (socket != null) {
                    socket.close();
                    socket = null;
                }
                onDisconnect();
            } catch(IOException closeException){
                Log.e("bluetooth", closeException.getLocalizedMessage());
            }
        }
    }

    public void setXY(float x, float y) {
        Log.e("bluetooth", "isConnected " + socket.isConnected());
        if (x < 0) {
            x = 0;
        } else if (x > 1) {
            x = 1;
        }
        if (y < 0) {
            y = 0;
        } else if (y > 1) {
            y = 1;
        }
        this.y = y;
        this.x = x;
        this.updateXY = true;
    }

    public void sendSetXY() {
        totalPacks++;
        ByteArrayOutputStream outPack = new ByteArrayOutputStream();
        DataOutputStream outputStream = new DataOutputStream(outPack);
        try {
            outputStream.writeByte(0x01);
            outputStream.writeFloat(x);
            outputStream.writeFloat(y);
            outputStream.writeByte('\r');
            outputStream.writeByte('\n');
        } catch (IOException e) {
            Log.e("bluetooth", e.getLocalizedMessage());
        }
        byte[] bytes = outPack.toByteArray();
        String outs = new String();
        for (byte sym : bytes) {
            outs += "0x" + Integer.toHexString(0xFF & sym) + " ";
        }
        // Log.d("bluetooth pack", outs + " " + bytes.length + " totalPacks=" + totalPacks);
        write(bytes);
    }

    public void onConnect() {
        if (listener != null) {
            listener.onConnect();
        }
    }

    public void onDisconnect() {
        if (listener != null) {
            listener.onDisconnect();
        }
    }
}
