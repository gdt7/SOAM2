package com.example.android_app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import java.util.ArrayList;
import java.util.List;

public class ChoferDbHelper extends SQLiteOpenHelper {
    private SQLiteDatabase db;
    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "CGTVirtual.db";

    public ChoferDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        db = this.getWritableDatabase();
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(Chofer.SQL_CREATE_ENTRIES);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(Chofer.SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }


    public void insertChofer(String fullName, String tagId, String entryHour) {
        ContentValues values = new ContentValues();
        values.put(Chofer.COLUMN_NAME_FULL_NAME, fullName);
        values.put(Chofer.COLUMN_NAME_TAG_ID, tagId);
        values.put(Chofer.COLUMN_NAME_ENTRY_HOUR, entryHour);

// Insert the new row, returning the primary key value of the new row
        long newRowId = db.insert(Chofer.TABLE_NAME, null, values);
    }

    public List<Chofer> findChoferById(String tagId) {
        String[] projection = {
                BaseColumns._ID,
                Chofer.COLUMN_NAME_FULL_NAME,
                Chofer.COLUMN_NAME_TAG_ID,
                Chofer.COLUMN_NAME_ENTRY_HOUR
        };

        String selection = Chofer.COLUMN_NAME_TAG_ID + " = ?";
        String[] selectionArgs = {tagId};

// How you want the results sorted in the resulting Cursor
        String sortOrder =
                Chofer.COLUMN_NAME_TAG_ID + " DESC";

        Cursor cursor = db.query(
                Chofer.TABLE_NAME,   // The table to query
                projection,             // The array of columns to return (pass null to get all)
                selection,              // The columns for the WHERE clause
                selectionArgs,          // The values for the WHERE clause
                null,                   // don't group the rows
                null,                   // don't filter by row groups
                sortOrder               // The sort order
        );

        List<Chofer> choferes = new ArrayList<>();
        while (cursor.moveToNext()) {

            long itemId = cursor.getLong(
                    cursor.getColumnIndexOrThrow(Chofer._ID));
            String driverName = cursor.getString(
                    cursor.getColumnIndexOrThrow(Chofer.COLUMN_NAME_FULL_NAME));
            String tag = cursor.getString(
                    cursor.getColumnIndexOrThrow(Chofer.COLUMN_NAME_TAG_ID));
            String entryHour = cursor.getString(
                    cursor.getColumnIndexOrThrow(Chofer.COLUMN_NAME_ENTRY_HOUR));
            choferes.add(new Chofer(driverName, tag, entryHour));
        }
        return choferes;
    }


}
