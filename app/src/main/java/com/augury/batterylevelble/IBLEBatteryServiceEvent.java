package com.augury.batterylevelble;

public interface IBLEBatteryServiceEvent {
    void onLog(String s);
    void onAdapterDisabled();
}
