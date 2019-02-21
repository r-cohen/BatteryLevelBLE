package com.augury.batterylevelble;

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
import android.os.BatteryManager;
import android.os.ParcelUuid;

import java.math.BigInteger;

import static com.augury.batterylevelble.BLEBatteryProfile.BLE_BATTERY_SERVICE;

public class BLEBatteryService {
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGattServer bluetoothGattServer;
    private IBLEBatteryServiceEvent eventsListener;
    private BluetoothManager bluetoothManager;
    private BatteryManager batteryManager;

    public BLEBatteryService(Context context) {
        bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        batteryManager = (BatteryManager)context.getSystemService(Context.BATTERY_SERVICE);
    }

    public void setEventsListener(IBLEBatteryServiceEvent eventsListener) {
        this.eventsListener = eventsListener;
    }

    private void log(String s) {
        if (eventsListener != null) {
            eventsListener.onLog(s);
        }
    }

    public void initBluetoothAdapter(IBluetoothAdapterEvent callback) {
        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            if (eventsListener != null) {
                eventsListener.onAdapterDisabled();
            }
            return;
        }
        log("bluetooth adapter ready");
        callback.onReady();
    }

    public void bleAdvertiseAndStartService(Context context) {
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

        bluetoothGattServer = bluetoothManager.openGattServer(context, gattServerCallback);
        if (bluetoothGattServer == null) {
            log("unable to create gatt server");
            return;
        }
        bluetoothGattServer.addService(BLEBatteryProfile.buildBLEBatteryService());
        log("service started");
    }

    public void bleStopServiceAndAdvertise() {
        if (bluetoothGattServer != null) {
            bluetoothGattServer.close();
            log("service stopped");
        }
        if (bluetoothAdapter != null && bluetoothAdapter.getBluetoothLeAdvertiser() != null) {
            bluetoothAdapter.getBluetoothLeAdvertiser().stopAdvertising(advertiseCallback);
            log("advertising stopped");
        }
    }

    private AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            log("LE Advertise Started.");
        }

        @Override
        public void onStartFailure(int errorCode) {
            log("LE Advertise Failed: " + errorCode);
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
            log(String.format("device %s %s", device.toString(), state));
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            log(String.format("device %s characteristic read request", device.toString()));
            if (characteristic.getUuid().equals(BLEBatteryProfile.BLE_BATTERY_LEVEL_CHARACTERISTIC)) {
                int batteryLevel = getBatteryLevel();
                log("battery level is " + Integer.toString(batteryLevel));

                byte[] value = BigInteger.valueOf(batteryLevel).toByteArray();
                bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, value);
                return;
            }
            bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null);
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
            log(String.format("device %s descriptor read request", device.toString()));
            super.onDescriptorReadRequest(device, requestId, offset, descriptor);
        }
    };


    private int getBatteryLevel() {
        if (batteryManager == null) {
            return -1;
        }
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
    }

}
