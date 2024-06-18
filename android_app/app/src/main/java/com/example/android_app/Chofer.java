package com.example.android_app;

import android.provider.BaseColumns;

public class Chofer implements BaseColumns{

    public static final String TABLE_NAME = "chofer";
    public static final String COLUMN_NAME_FULL_NAME = "nombreApellido";
    public static final String COLUMN_NAME_TAG_ID = "tagId";
    public static final String COLUMN_NAME_ENTRY_HOUR = "horarioEntrada";

    private String fullName;
    private String  tagId;
    private String entryHour;

    public static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    _ID + " INTEGER PRIMARY KEY," +
                    COLUMN_NAME_FULL_NAME + " TEXT," +
                    COLUMN_NAME_TAG_ID + " TEXT," +
                    COLUMN_NAME_ENTRY_HOUR + " TEXT)";

    public static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + TABLE_NAME;

    public Chofer(String fullName, String tagId, String entryHour) {
        this.fullName = fullName;
        this.tagId = tagId;
        this.entryHour = entryHour;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getTagId() {
        return tagId;
    }

    public void setTagId(String tagId) {
        this.tagId = tagId;
    }

    public String getEntryHour() {
        return entryHour;
    }

    public void setEntryHour(String entryHour) {
        this.entryHour = entryHour;
    }
}