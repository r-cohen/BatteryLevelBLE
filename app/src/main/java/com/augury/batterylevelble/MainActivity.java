package com.augury.batterylevelble;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private LinearLayout outputLayout;
    private static final int REQUEST_ENABLE_BT = 100;
    private BLEBatteryService bleBatteryService;

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
        bleBatteryService = new BLEBatteryService(MainActivity.this);
        bleBatteryService.setEventsListener(bleBatteryServiceEvent);
        initBLEService();
    }

    @Override
    protected void onPause() {
        if (bleBatteryService != null) {
            bleBatteryService.bleStopServiceAndAdvertise();
            bleBatteryService.setEventsListener(null);
        }
        super.onPause();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_ENABLE_BT && resultCode != RESULT_OK) {
            output("requesting bluetooth activation");
            initBLEService();
        }
    }

    private IBluetoothAdapterEvent bluetoothAdapterEvent = new IBluetoothAdapterEvent() {
        @Override
        public void onReady() {
            if (bleBatteryService != null) {
                bleBatteryService.bleAdvertiseAndStartService(MainActivity.this);
            }
        }
    };

    private IBLEBatteryServiceEvent bleBatteryServiceEvent = new IBLEBatteryServiceEvent() {
        @Override
        public void onLog(String s) {
            output(s);
        }

        @Override
        public void onAdapterDisabled() {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    };

    private void initBLEService() {
        if (bleBatteryService != null) {
            bleBatteryService.initBluetoothAdapter(bluetoothAdapterEvent);
        }
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

}
