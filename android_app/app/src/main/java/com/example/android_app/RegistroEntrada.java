package com.example.android_app;

import android.provider.BaseColumns;

public class RegistroEntrada implements BaseColumns {

    public static final String TABLE_NAME = "registroEntrada";
    public static final String COLUMN_NAME_DRIVER_ID = "choferId";
    public static final String COLUMN_NAME_TIME = "hora";


    private String driverId;
    private String time;


    public RegistroEntrada(String driverId,String time) {
        this.driverId = driverId;
        this.time = time;
    }
}
