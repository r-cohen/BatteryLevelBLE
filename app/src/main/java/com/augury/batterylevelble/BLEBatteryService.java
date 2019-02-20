package com.augury.batterylevelble;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.os.BatteryManager;

import java.util.UUID;

class BLEBatteryService {
    static final UUID BLE_BATTERY_SERVICE = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb");
    static final UUID BLE_BATTERY_LEVEL_CHARACTERISTIC = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");
    static final UUID DESCRIPTOR_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    static BluetoothGattService buildBLEBatteryService() {
        BluetoothGattService service = new BluetoothGattService(BLE_BATTERY_SERVICE,
                BluetoothGattService.SERVICE_TYPE_PRIMARY);
        BluetoothGattCharacteristic batteryLevel = new BluetoothGattCharacteristic(BLE_BATTERY_LEVEL_CHARACTERISTIC,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ);
        BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(DESCRIPTOR_CONFIG_UUID,
                BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);
        batteryLevel.addDescriptor(descriptor);
        service.addCharacteristic(batteryLevel);
        return service;
    }

    static int getBatteryLevel(Context context) {
        BatteryManager batteryManager = (BatteryManager)context.getSystemService(Context.BATTERY_SERVICE);
        if (batteryManager == null) {
            return -1;
        }
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
    }
}
