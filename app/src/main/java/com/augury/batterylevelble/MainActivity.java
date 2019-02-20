package com.augury.batterylevelble;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static com.augury.batterylevelble.BLEBatteryService.BLE_BATTERY_SERVICE;

public class MainActivity extends AppCompatActivity {
    private LinearLayout outputLayout;
    private BluetoothAdapter bluetoothAdapter;
    private static final int REQUEST_ENABLE_BT = 100;
    private BluetoothGattServer bluetoothGattServer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        outputLayout = findViewById(R.id.layoutOutput);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }


    @Override
    protected void onResume() {
        super.onResume();
        initBluetoothAdapter(new IAdapterReady() {
            @Override
            public void onReady() {
                bleAdvertiseAndStartService();
            }
        });
    }

    @Override
    protected void onPause() {
        bleStopServiceAndAdvertise();
        super.onPause();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_ENABLE_BT && resultCode != RESULT_OK) {
            output("requesting bluetooth activation");
            initBluetoothAdapter(new IAdapterReady() {
                @Override
                public void onReady() {
                    bleAdvertiseAndStartService();
                }
            });
        }
    }

    private void bleAdvertiseAndStartService() {
        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .build();
        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .setIncludeTxPowerLevel(false)
                .addServiceUuid(new ParcelUuid(BLE_BATTERY_SERVICE))
                .build();
        bluetoothAdapter.getBluetoothLeAdvertiser().startAdvertising(settings, data, advertiseCallback);

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothGattServer = bluetoothManager.openGattServer(MainActivity.this, gattServerCallback);
        if (bluetoothGattServer == null) {
            output("unable to create gatt server");
            return;
        }
        bluetoothGattServer.addService(BLEBatteryService.buildBLEBatteryService());
        output("service started");
    }

    private void bleStopServiceAndAdvertise() {
        if (bluetoothGattServer != null) {
            bluetoothGattServer.close();
            output("service stopped");
        }
        if (bluetoothAdapter != null && bluetoothAdapter.getBluetoothLeAdvertiser() != null) {
            bluetoothAdapter.getBluetoothLeAdvertiser().stopAdvertising(advertiseCallback);
            output("advertising stopped");
        }
    }

    private interface IAdapterReady {
        void onReady();
    }

    private void initBluetoothAdapter(IAdapterReady callback) {
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            return;
        }
        output("bluetooth adapter ready");
        callback.onReady();
    }

    private void output(final String s) {
        if (outputLayout != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    TextView tv = new TextView(MainActivity.this);
                    tv.setText(s);
                    tv.setLayoutParams(new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT));
                    outputLayout.addView(tv);
                }
            });
        }
    }

    private AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            output("LE Advertise Started.");
        }

        @Override
        public void onStartFailure(int errorCode) {
            output("LE Advertise Failed: " + errorCode);
        }
    };

    private BluetoothGattServerCallback gattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            String state;
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    state = "connected";
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    state = "disconnected";
                    break;
                case BluetoothProfile.STATE_CONNECTING:
                    state = "connecting";
                    break;
                case BluetoothProfile.STATE_DISCONNECTING:
                    state = "disconnecting";
                    break;
                default:
                    state = "?";
                    break;
            }
            output(String.format("device %s %s", device.toString(), state));
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            output(String.format("device %s characteristic read request", device.toString()));
            if (characteristic.getUuid().equals(BLEBatteryService.BLE_BATTERY_LEVEL_CHARACTERISTIC)) {
                int batteryLevel = BLEBatteryService.getBatteryLevel(MainActivity.this);
                output("battery level is " + Integer.toString(batteryLevel));

                byte[] value = BigInteger.valueOf(batteryLevel).toByteArray();
                bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, value);
                return;
            }
            bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null);
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
            output(String.format("device %s descriptor read request", device.toString()));
            super.onDescriptorReadRequest(device, requestId, offset, descriptor);
        }
    };
}
