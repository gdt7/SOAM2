package com.example.android_app;

import android.provider.BaseColumns;

public final class FeedReaderContract {
    // To prevent someone from accidentally instantiating the contract class,
    // make the constructor private.
    private FeedReaderContract() {
    }

    /* Inner class that defines the table contents */
    public static class FeedEntry implements BaseColumns {
        public static final String TABLE_NAME = "chofer";
        public static final String COLUMN_NAME_FULL_NAME = "nombreApellido";
        public static final String COLUMN_NAME_TAG_ID = "tagId";
        public static final String COLUMN_NAME_ENTRY_HOUR = "horarioEntrada";

    }

    public static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + FeedEntry.TABLE_NAME + " (" +
                    FeedEntry._ID + " INTEGER PRIMARY KEY," +
                    FeedEntry.COLUMN_NAME_FULL_NAME + " TEXT," +
                    FeedEntry.COLUMN_NAME_TAG_ID + " TEXT," +
                    FeedEntry.COLUMN_NAME_ENTRY_HOUR + " TEXT)";

    public static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + FeedEntry.TABLE_NAME;



}