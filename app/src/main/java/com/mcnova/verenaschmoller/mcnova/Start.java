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
import android.support.design.widget.Snackbar;
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

    Button btOn, btList, btOff, btConnect;
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
    Button bleScanButton, bleStopButton;
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
        btConnect = findViewById(R.id.buttonConnect);
        bleScanButton = findViewById(R.id.bleScan);
        bleStopButton = findViewById(R.id.bleScanStop);

        BA = BluetoothAdapter.getDefaultAdapter();
        lv = findViewById(R.id.listView);

        //BLE Variablen Initialsierung
        btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();
        btScanner = btAdapter.getBluetoothLeScanner();

        //Laden des Textviews von BLE
        bleTextView = (TextView) findViewById(R.id.TextViewBLE);
        //Setzen wie sich das TextView verhält
        bleTextView.setMovementMethod(new ScrollingMovementMethod());

        //Enable/Disable Buttons falls Bluetooth auf dem Gerät ein- bzw. ausgeschaltet ist
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
                Log.e(TAG, result.getDevice().getName());
            }

            // auto scroll for text view
            final int scrollAmount = bleTextView.getLayout().getLineTop(bleTextView.getLineCount()) - bleTextView.getHeight();
            // if there is no need to scroll, scrollAmount will be <=0
            if (scrollAmount > 0)
                bleTextView.scrollTo(0, scrollAmount);
        }
    };

    //Methode, welche beim Klick auf den Button SCANBLE aufgerufen wird
    public void startScanning(View view) {
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
    public void stopScanning(View view) {
        System.out.println("stopping scanning");
        //bleTextView.setText("Scan gestoppt!\n");
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                btScanner.stopScan(leScanCallback);
            }
        });
    }

    //Methode, welche beim Klick auf den Button 'Verbindung herstellen' aufgerufen wird
    //Voraussetzung: Bluetooth Device muss gepaired sein!
    public void connectToRobot(View view) {

        pairedDevices = BA.getBondedDevices();
        for (BluetoothDevice bt : pairedDevices) {
            Log.e(TAG, bt.getName());                       //Liste an gepaarten Devices durchgehen
            if (bt.getName().equals(bluetoothDevice)) {     //Checken, ob gesuchtes Gerät enthalten ist
                _bluetoothDev = bt;                         //Falls ja, gesuchtes Gerät in Variable abspeichern
            }
        }

        TextView pressed = findViewById(R.id.connectionBool);
        if (_bluetoothDev.getName() != "") {                                            // Prüfung, ob Device-Variable belegt ist
            Log.e(TAG, _bluetoothDev.getAddress());
            int bond = _bluetoothDev.getBondState();
            pressed.setText(String.valueOf(bond) + " to " + _bluetoothDev.getName());   // Verbindungsstatus anzeigen

            //Verbindung wird hergestellt
            mBluetoothGatt = _bluetoothDev.connectGatt(this, false, mGattCallback);
            Log.e(TAG, "###################################################Connected to" + mBluetoothGatt.getDevice().getName());

            /*
            Classic Bluetooth 4.0:
            --------------------------------
            ConnectThread t = new ConnectThread(_bluetoothDev, devNumber);
            pressed.setText("connected");
            t.start();
            */
        } else {
            pressed.setText("could not connect");
        }
    }

    //Methode, welche beim Klick auf eine der Übungen aufgerufen wird
    public boolean writeCharacteristic(View view) {

        boolean status = false;
        TextView connBool = findViewById(R.id.connectionBool);
        AlertDialog alertDialog = new AlertDialog.Builder(Start.this).create();
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        //check if mBluetoothGatt is available
        if (mBluetoothGatt == null) {
            Log.e(TAG, "lost connection");
            connBool.setText("writeStatus: " + status);
            alertDialog.setTitle("Error");
            alertDialog.setMessage("Keine Verbindung");
            alertDialog.show();
            return false;
        }
        else {
            Log.e(TAG, "connection good" + mBluetoothGatt.toString());
        }

        //check if Service is available
        BluetoothGattService Service = mBluetoothGatt.getService(service);
        if (Service == null) {
            Log.e(TAG, "service not found!" + Service);
            connBool.setText("writeStatus: " + status);
            alertDialog.setTitle("Error");
            alertDialog.setMessage("Service nicht gefunden");
            alertDialog.show();
            return false;
        }
        else {
            Log.e(TAG, "service found!" + Service);
        }

        //check if Characteristic is available
        BluetoothGattCharacteristic charac = Service.getCharacteristic(character);
        if (charac == null) {
            Log.e(TAG, "char not found!");
            connBool.setText("writeStatus: " + status);
            alertDialog.setTitle("Error");
            alertDialog.setMessage("Characteristic nicht gefunden");
            alertDialog.show();
            return false;
        }
        else {
            Log.e(TAG, "char found!" + charac);
        }

        //byte array initialisieren und mit ausgewählten Werten belegen
        byte[] value = new byte[16];                                        // Bluetooth Chip kann 16 byte empfangen
        TextView repeats = findViewById(R.id.repeats);
        int nRepeats = Integer.parseInt(repeats.getText().toString());
        value[0] = (byte) nRepeats;                     // Zwischen 1 und 10
        int exerciseId = Integer.parseInt(view.getTag().toString());        // Übungs-ID wird vom Button-Tag abgerufen
        value[1] = (byte) exerciseId;                   // Zwischen 1 und 6

        //value[15] = 0x01;
        /*for (int i = 2; i < 16; i++)
        {
            value[i] = 0x00;
        }*/

        charac.setValue(value);
        status = mBluetoothGatt.writeCharacteristic(charac);
        Log.e(TAG, "Number of Bytes:" + value.length);
        Log.e(TAG, "Byte 0:" + value[0]);
        Log.e(TAG, "Byte 1:" + value[1]);
        Log.e(TAG, "WriteStatus:" + status);

        if(status) {
            connBool.setText("writeStatus: " + status);

            alertDialog.setTitle("Erfolg");
            alertDialog.setMessage("Übung wird ausgeführt.\nBitte warten Sie bis der Roboter die aktuelle Übung beendet hat!");
            alertDialog.show();
        }
        else {
            connBool.setText("writeStatus: " + status);
            alertDialog.setTitle("Error");
            alertDialog.setMessage("Characteristic konnte nicht beschrieben werden");
            alertDialog.show();
        }
        return status;
    }

    //Methode, welche beim Klick auf den Button '+' aufgerufen wird
    //Wiederholungsvariable inkrementieren
    public void increaseRepeats(View view) {
        int temp = 0;
        TextView repeats = findViewById(R.id.repeats);
        temp = Integer.parseInt(repeats.getText().toString());
        temp += 1;
        repeats.setText(String.valueOf(temp));
        if (temp == 10) {                                           // '+' und '-' Buttons
            findViewById(R.id.increase).setEnabled(false);          // entsprechend aktivieren/deaktivieren,
        }                                                           // um für die Wiederholungsvariable nur
        else if (temp > 1) {                                        // Werte zwischen 1 und 10 zuzulassen
            findViewById(R.id.increase).setEnabled(true);
            findViewById(R.id.decrease).setEnabled(true);
        }

    }

    //Methode, welche beim Klick auf den Button '-' aufgerufen wird
    //Wiederholungsvariable dekrementieren
    public void decreaseRepeats(View view) {
        int temp = 0;
        TextView repeats = findViewById(R.id.repeats);
        temp = Integer.parseInt(repeats.getText().toString());
        temp -= 1;
        repeats.setText(String.valueOf(temp));
        if (temp == 1) {                                            // '+' und '-' Buttons
            findViewById(R.id.decrease).setEnabled(false);          // entsprechend aktivieren/deaktivieren,
        }                                                           // um für die Wiederholungsvariable nur
        else if (temp < 10){                                        // Werte zwischen 1 und 10 zuzulassen
            findViewById(R.id.increase).setEnabled(true);
            findViewById(R.id.decrease).setEnabled(true);
        }
    }

    //Methode, welche beim Klick auf den Button 'Turn On' aufgerufen wird
    //Bluetooth auf den Gerät einschalten
    public void on(View v) {
        if (!BA.isEnabled()) {                                      // Abfrage ob Bluetooth bereits eingeschaltet ist
            Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnOn, 0);
            Toast.makeText(getApplicationContext(), "Turned on", Toast.LENGTH_LONG).show();
            btList.setEnabled(true);                                // Buttons entsprechend aktivieren/deaktivieren
            btOff.setEnabled(true);                                 // Bluetooth eingeschaltet:
            btConnect.setEnabled(true);                             // alle Buttons verfügbar, außer 'Turn On'
            bleScanButton.setEnabled(true);
            bleStopButton.setEnabled(true);
            btOn.setEnabled(false);
        } else {
            Toast.makeText(getApplicationContext(), "Already on", Toast.LENGTH_LONG).show();
        }
    }

    //Methode, welche beim Klick auf den Button 'Turn Off' aufgerufen wird
    //Bluetooth auf den Gerät ausschalten
    public void off(View v) {
        BA.disable();                                               // Bluetooth ausschalten
        Toast.makeText(getApplicationContext(), "Turned off", Toast.LENGTH_LONG).show();

        btOn.setEnabled(true);                                      // Buttons entsprechend aktivieren/deaktivieren
        btList.setEnabled(false);                                   // Bluetooth ausgeschaltet:
        btOff.setEnabled(false);                                    // kein Button verfügbar, außer 'Turn On'
        btConnect.setEnabled(false);
        bleScanButton.setEnabled(false);
        bleStopButton.setEnabled(false);
    }

    // Aktiviert Sichtbarkeit des Gerätes
    public void visible(View v) {
        Intent getVisible = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        startActivityForResult(getVisible, 0);
    }

    //Methode, welche beim Klick auf den Button 'List devices' aufgerufen wird
    //Listet gepaarte Geräte auf, welche den Namen des gesuchten Gerätes haben
    public void list(View v) {
        pairedDevices = BA.getBondedDevices();

        ArrayList list = new ArrayList();
        for (BluetoothDevice bt : pairedDevices) {
            Log.e(TAG, bt.getName());
            if (bt.getName().equals(bluetoothDevice)) {
                list.add(bt.getName());
                _bluetoothDev = bt;
                Log.e(TAG, "Found required Bluetooth device with name: " + _bluetoothDev.getName());
            }
        }

        Toast.makeText(getApplicationContext(), "Showing Paired Devices", Toast.LENGTH_SHORT).show();
        final ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, list);
        lv.setAdapter(adapter);
    }


    // Starten des BLE Scans
    private void startBLEScan() {
        List<ScanFilter> filters = new ArrayList<>();
        ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_POWER).build();
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

    public void ShowConnectedDevices(View view) {
        Set<BluetoothDevice> gattServerConnectedDevices = BA.getBondedDevices();
        for (BluetoothDevice device1 : gattServerConnectedDevices) {
            Log.e(TAG, "Found connected device: " + device1.getName());
            Log.e(TAG, "Found UUIDS device: " + device1.getUuids()[0].toString() + " Length " + device1.getUuids().length);

        }
    }

    // Classic Bluetooth 4.0 connection.
    // Not used
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
}





