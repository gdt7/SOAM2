package com.example.android_app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

public class RegistroEntradaDao extends SQLiteOpenHelper {

    private SQLiteDatabase db;

    public static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + RegistroEntrada.TABLE_NAME + " (" +
                    RegistroEntrada._ID + " INTEGER PRIMARY KEY," +
                    RegistroEntrada.COLUMN_NAME_DRIVER_ID + " TEXT," +
                    RegistroEntrada.COLUMN_NAME_TIME + " TEXT)";

    public static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + RegistroEntrada.TABLE_NAME;


    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "CGTVirtual.db";

    public RegistroEntradaDao(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        db = this.getWritableDatabase();
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(RegistroEntradaDao.SQL_CREATE_ENTRIES);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(RegistroEntradaDao.SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }


    public void insertRegistro() {
        ContentValues values = new ContentValues();
        values.put(RegistroEntrada.COLUMN_NAME_DRIVER_ID, "Jose Perez");
        values.put(RegistroEntrada.COLUMN_NAME_TIME, "100 234 99 122");

        // Insert the new row, returning the primary key value of the new row
        long newRowId = db.insert(RegistroEntrada.TABLE_NAME, null, values);
    }

    public RegistroEntrada findRegistrorById(String id) {
        String[] projection = {
                BaseColumns._ID,
                RegistroEntrada.COLUMN_NAME_DRIVER_ID
        };
        // Filter results WHERE "title" = 'My Title'
        String selection = RegistroEntrada.COLUMN_NAME_DRIVER_ID + " = ?";
        String[] selectionArgs = {id};

// How you want the results sorted in the resulting Cursor
        String sortOrder =
                RegistroEntrada.COLUMN_NAME_DRIVER_ID + " DESC";

        Cursor cursor = db.query(
                RegistroEntrada.TABLE_NAME,   // The table to query
                projection,             // The array of columns to return (pass null to get all)
                selection,              // The columns for the WHERE clause
                selectionArgs,          // The values for the WHERE clause
                null,                   // don't group the rows
                null,                   // don't filter by row groups
                sortOrder               // The sort order
        );

        RegistroEntrada re = null;
        while (cursor.moveToNext()) {
//            long id= cursor.getLong(
//                    cursor.getColumnIndexOrThrow(RegistroEntrada._ID));
            String driverName = cursor.getString(
                    cursor.getColumnIndexOrThrow(RegistroEntrada.COLUMN_NAME_DRIVER_ID));
            String time = cursor.getString(
                    cursor.getColumnIndexOrThrow(RegistroEntrada.COLUMN_NAME_TIME));
            re = new RegistroEntrada(driverName,time);
        }
        cursor.close();
        return re;
    }
}
