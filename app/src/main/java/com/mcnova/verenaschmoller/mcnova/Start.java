package com.mcnova.verenaschmoller.mcnova;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Parcel;
import android.os.ParcelUuid;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;


import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.view.View;

import android.app.AlertDialog;
import android.content.DialogInterface;

import com.mcnova.verenaschmoller.mcnova.MyBluetoothService;


public class Start extends AppCompatActivity {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    String bluetoothDevice = "MCNOVA_FA50";
    String bleBluetoothDevice = "MCNOVA_FA50";

    Button btOn, btList, btOff;
    private int devNumber;
    private BluetoothAdapter BA;
    private Set<BluetoothDevice> pairedDevices;
    private BluetoothDevice _bluetoothDev;
    ListView lv;
    private static final String TAG = "Start";
    private Map<String, BluetoothDevice> mScanResults;
    private ScanCallback mScanCallback;
    //private UUID uuid = UUID.randomUUID();
    private UUID uuid = UUID.fromString("0000111e-0000-1000-8000-00805f9b34fb");
    private UUID bleuuid = UUID.fromString("0000fe61-0000-1000-8000-00805f9b34fb");
    private UUID service = UUID.fromString("1eb96b07-a816-49ed-99a4-de0700b195aa");
    private UUID character = UUID.fromString("970a788a-cec2-4132-b269-872dd131baea");
    //BluetoothManager bluetoothManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);

    //For Bluetooth Low Energy
    private BluetoothLeScanner btScanner;
    private BluetoothManager btManager;
    private BluetoothAdapter btAdapter;
    private TextView bleTextView;
    Button bleScanButton, bleStop;
    private final static int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        // Use this check to determine whether BLE is supported on the device. Then
        // you can selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "No BLE", Toast.LENGTH_SHORT).show();
            finish();
        }

        btOn = findViewById(R.id.buttonOn);
        btList = findViewById(R.id.buttonList);
        btOff = findViewById(R.id.buttonOff);

        BA = BluetoothAdapter.getDefaultAdapter();
        lv = findViewById(R.id.listView);

        //BLE Varbiablen Initialsierung
        btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();
        btScanner = btAdapter.getBluetoothLeScanner();
        bleScanButton = (Button) findViewById(R.id.bleScan);
        bleStop = (Button) findViewById(R.id.bleScanStop);

        //Setzen der Buttons für die den Start und Stop des Scanvorgangs von Bluetooth Low Energie
        bleScanButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startScanning();
            }
        });
        bleStop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                stopScanning();
            }
        });

        //Laden des Textviews von BLE
        bleTextView = (TextView) findViewById(R.id.TextViewBLE);
        //Setzen wie sie das TextView verhält
        bleTextView.setMovementMethod(new ScrollingMovementMethod());

        // Example of a call to a native method
        //TextView tv = findViewById(R.id.sample_text);
        //tv.setText(stringFromJNI());

        if (BA.isEnabled()) {
            btOn.setEnabled(false);
        } else {
            btList.setEnabled(false);
            btOff.setEnabled(false);
        }

        // Checken, ob Location Access aktiviert ist, wird dringend für BLE benötigt
        if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("This app needs location access");
            builder.setMessage("Please grant location access so this app can detect peripherals.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                }
            });
            builder.show();
        }
    }

    //BLE Scan mit Append in die Liste der Erfassten Devices
    //TODO von TextView auf ListView umändern. Oder If um Devicename zu fangen, damit verbunden werden kann
    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            //bleTextView.append("Dev: " + result.getDevice().getName() + " RSSI: " + result.getRssi() + " UUID: " + result.getScanRecord().getServiceUuids() + "ADDR:" + result.getDevice().getAddress() + "\n");
            //bleTextView.append("Dev: " + result.getDevice().getName() +  "\n");
            if (result.getDevice().getName() == null) {
                Log.e(TAG, "NULL DEVICES!!\n\n\n\n");
            } else {
                bleTextView.append("Dev: " + result.getDevice().getName() + " UUID: " + result.getScanRecord().getServiceUuids() + "ADDR:" + result.getDevice().getAddress() + "\n");
                Log.e(TAG, result.getDevice().getName() + "###########################################################################################!!\n\n\n\n");
            }

            // auto scroll for text view
            final int scrollAmount = bleTextView.getLayout().getLineTop(bleTextView.getLineCount()) - bleTextView.getHeight();
            // if there is no need to scroll, scrollAmount will be <=0
            if (scrollAmount > 0)
                bleTextView.scrollTo(0, scrollAmount);
        }
    };

    //Methode, welche beim Klick auf den Button SCANBLE aufgerufen wird
    public void startScanning() {
        System.out.println("start scanning");
        bleTextView.setText("Geräte Scannen\n");
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                btScanner.startScan(leScanCallback);
            }
        });
    }
    //Methode, welche beim Klick auf den Button BLESTOP aufgerufen wird

    public void stopScanning() {
        System.out.println("stopping scanning");
        //bleTextView.setText("Scan gestoppt!\n");
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                btScanner.stopScan(leScanCallback);
            }
        });
    }


    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    //public native String stringFromJNI();

    public void connectToRobot(View view) {

        pairedDevices = BA.getBondedDevices();
        for (BluetoothDevice bt : pairedDevices) {
            Log.e(TAG, bt.getName() + " = " + bluetoothDevice);
            Log.e(TAG, Boolean.toString(bt.getName().equals(bluetoothDevice)));
            if (bt.getName().equals(bluetoothDevice)) {
                _bluetoothDev = bt;
            }
        }

        BluetoothSocket socket = null;
        TextView pressed = findViewById(R.id.connectionBool);
        if (_bluetoothDev.getName() != "") {
            Log.e(TAG, _bluetoothDev.getAddress());
            //_bluetoothDe#v.createBond();
            int bond = _bluetoothDev.getBondState();
            pressed.setText(String.valueOf(bond) + " to " + _bluetoothDev.getName());
            mBluetoothGatt = _bluetoothDev.connectGatt(this, false, mGattCallback);
            Log.e(TAG, "###################################################Connected to" + mBluetoothGatt.getDevice().getName());
            //ConnectThread t = new ConnectThread(_bluetoothDev, devNumber);
            //pressed.setText("connected");
            //t.start();
        } else {
            pressed.setText("could not connect");
        }
    }

    public void ShowConnectedDevices(View view) {
        Log.e(TAG, "Funktion!!!!!!!");
        //List<BluetoothDevice> gattServerConnectedDevices = btManager.getConnectedDevices(BluetoothProfile.GATT_SERVER);
        Set<BluetoothDevice> gattServerConnectedDevices = BA.getBondedDevices();
        for (BluetoothDevice device1 : gattServerConnectedDevices) {
            Log.e(TAG, "Found connected device: " + device1.getName());
            Log.e(TAG, "Found UUIDS device: " + device1.getUuids()[0].toString() + " Length " + device1.getUuids().length);

        }
    }

    public void increaseRepeats(View view) {
        int temp = 0;
        TextView repeats = findViewById(R.id.repeats);
        temp = Integer.parseInt(repeats.getText().toString());
        temp += 1;
        repeats.setText(String.valueOf(temp));
        if (temp == 10) {
            Button bt = findViewById(R.id.increase);
            bt.setEnabled(false);
        }
        else if (temp > 1) {
            findViewById(R.id.increase).setEnabled(true);
            findViewById(R.id.decrease).setEnabled(true);
        }

    }

    public void decreaseRepeats(View view) {
        int temp = 0;
        TextView repeats = findViewById(R.id.repeats);
        temp = Integer.parseInt(repeats.getText().toString());
        temp -= 1;
        repeats.setText(String.valueOf(temp));
        if (temp == 1) {
            Button bt = findViewById(R.id.decrease);
            bt.setEnabled(false);
        }
        else if (temp < 10){
            findViewById(R.id.increase).setEnabled(true);
            findViewById(R.id.decrease).setEnabled(true);
        }
    }

    public void on(View v) {
        if (!BA.isEnabled()) {
            Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnOn, 0);
            Toast.makeText(getApplicationContext(), "Turned on", Toast.LENGTH_LONG).show();
            btList.setEnabled(true);
            btOff.setEnabled(true);
            btOn.setEnabled(false);
        } else {
            Toast.makeText(getApplicationContext(), "Already on", Toast.LENGTH_LONG).show();
        }
    }

    public void off(View v) {
        BA.disable();
        Toast.makeText(getApplicationContext(), "Turned off", Toast.LENGTH_LONG).show();
        btOn.setEnabled(true);
        btList.setEnabled(false);
        btOff.setEnabled(false);
    }


    public void visible(View v) {
        Intent getVisible = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        startActivityForResult(getVisible, 0);
    }

    //Liste für normales Bluetooth
    public void list(View v) {
        pairedDevices = BA.getBondedDevices();

        ArrayList list = new ArrayList();
        boolean counter = true;
        devNumber = 0;

        for (BluetoothDevice bt : pairedDevices) {
            //list.add(bt.getName());
            Log.e(TAG, bt.getName() + " = " + bluetoothDevice);
            Log.e(TAG, Boolean.toString(bt.getName().equals(bluetoothDevice)));
            if (bt.getName().equals(bluetoothDevice)) {
                list.add(bt.getName());
                _bluetoothDev = bt;
                Log.e(TAG, _bluetoothDev.getName());
                counter = false;
            }
            if (counter) devNumber++;
        }

        Toast.makeText(getApplicationContext(), "Showing Paired Devices", Toast.LENGTH_SHORT).show();

        final ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, list);

        lv.setAdapter(adapter);
    }


    // Starten des BLE Scans
    private void startBLEScan() {
        if (!hasLocationPermissions())  //Check ob man die Location Permissions hat
            requestLocationPermission(); // Wenn nein, dann hohl sie dir

        List<ScanFilter> filters = new ArrayList<>();
        ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_POWER).build();
        // mScanResults = new HashMap<>();
        // mScanCallback = new BLEScanCallback(mScanResults);


    }

    private boolean hasLocationPermissions() {

        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        // requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_FINE_LOCATION);

    }

    private class BLEScanCallback extends ScanCallback {

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            addScanResult(result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult result : results) {
                addScanResult(result);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e(TAG, "BLE Error mit Code" + errorCode);
        }

        private void addScanResult(ScanResult result) {
            BluetoothDevice dv = result.getDevice();
            String deviceAddress = dv.getAddress();
            mScanResults.put(deviceAddress, dv);
        }
    }

    private static class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private UUID uuid = UUID.randomUUID();
        private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        public ConnectThread(BluetoothDevice device, int devNumber) {

            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            mmDevice = device;
            device.fetchUuidsWithSdp();
            ParcelUuid[] uuids = device.getUuids();
            Log.e(TAG, mmDevice.getName() + ": " + uuids[devNumber].toString());
            Log.e(TAG, "Type: " + String.valueOf(mmDevice.getType()));
            UUID my_uuid = UUID.fromString(uuids[devNumber].toString());


            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                tmp = device.createRfcommSocketToServiceRecord(my_uuid);
                Log.e(TAG, "socket created!");
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            mBluetoothAdapter.cancelDiscovery();


            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                Log.e(TAG, "trying to connect");
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                Log.e(TAG, connectException.getMessage());
                try {
                    mmSocket.close();
                    Log.e(TAG, "close socket");
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                if (mmSocket.isConnected()) Log.e(TAG, "is connected");
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            // manageMyConnectedSocket(mmSocket);
           /* Log.e(TAG, "send msg");
            MyBluetoothService.ConnectedThread t = new MyBluetoothService.ConnectedThread(mmSocket);
            t.start();
            String message = "Hello World";
            byte[] msg = message.getBytes();
            t.write(msg); */
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }

    // TODO Befehle an Device / Box senden
    private final BluetoothGattCallback mGattCallback =
            new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                                    int newState) {
                    String intentAction;
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        intentAction = ACTION_GATT_CONNECTED;
                        mConnectionState = STATE_CONNECTED;
                        broadcastUpdate(intentAction);
                        Log.i(TAG, "Connected to GATT server.");
                        Log.i(TAG, "Attempting to start service discovery:" +
                                mBluetoothGatt.discoverServices());

                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        intentAction = ACTION_GATT_DISCONNECTED;
                        mConnectionState = STATE_DISCONNECTED;
                        Log.i(TAG, "Disconnected from GATT server.");
                        broadcastUpdate(intentAction);
                    }
                }

                @Override
                // New services discovered
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                    } else {
                        Log.w(TAG, "onServicesDiscovered received: " + status);
                    }
                }
            };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    public boolean writeCharacteristic(View view) {

        //check mBluetoothGatt is available
        if (mBluetoothGatt == null) {
            Log.e(TAG, "lost connection");
            return false;
        }
        {
            Log.e(TAG, "connection good" + mBluetoothGatt.toString());
        }
        BluetoothGattService Service = mBluetoothGatt.getService(service);
        if (Service == null) {
            Log.e(TAG, "service not found!" + Service);
            return false;
        }
        else
        {
            Log.e(TAG, "service found!" + Service);
        }
        BluetoothGattCharacteristic charac = Service.getCharacteristic(character);
        if (charac == null) {
            Log.e(TAG, "char not found!");
            return false;
        }
        else
        {
            Log.e(TAG, "char found!" + charac);
        }

        byte[] value = new byte[16];
        TextView repeats = findViewById(R.id.repeats);
        int nRepeats = Integer.parseInt(repeats.getText().toString());
        //Integer.parseInt(repeats.getText().toString());
        value[0] = (byte) nRepeats;
        int exerciseId = Integer.parseInt(view.getTag().toString());
        value[1] = (byte) exerciseId;
        //value[15] = 0x01;
        /*for (int i = 2; i < 16; i++)
        {
            value[i] = 0x00;
        }*/
        //value[1] = 0x02;
        //value[0] = (byte) (116);
        //value[1] = (byte) (105);
        //charac.setValue(value);
        //String test = "it";
        //value = test.getBytes();
        charac.setValue(value);
        boolean status = mBluetoothGatt.writeCharacteristic(charac);
        Log.e(TAG, "Number of Bytes:" + value.length);
        Log.e(TAG, "Byte 0:" + value[0]);
        Log.e(TAG, "Byte 1:" + value[1]);
        Log.e(TAG, "WriteStatus:" + status);
        return status;
    }
}





