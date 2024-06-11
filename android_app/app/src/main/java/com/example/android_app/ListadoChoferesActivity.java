package com.example.android_app;


import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.view.Gravity;
import android.widget.ImageButton;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.List;

public class ListadoChoferesActivity extends AppCompatActivity {

    private TableLayout tableChoferes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_listado_choferes);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tableChoferes = (TableLayout) findViewById(R.id.tableChoferes);

        FeedReaderDbHelper dbHelper = new FeedReaderDbHelper(this);

        SQLiteDatabase db = dbHelper.getWritableDatabase();

// Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_FULL_NAME, "Jose Perez");
        values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_TAG_ID, "100 234 99 122");
        values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_ENTRY_HOUR, "10");

// Insert the new row, returning the primary key value of the new row
       // long newRowId = db.insert(FeedReaderContract.FeedEntry.TABLE_NAME, null, values);

         db = dbHelper.getReadableDatabase();

// Define a projection that specifies which columns from the database
// you will actually use after this query.
        String[] projection = {
                BaseColumns._ID,
                FeedReaderContract.FeedEntry.COLUMN_NAME_FULL_NAME,
                FeedReaderContract.FeedEntry.COLUMN_NAME_TAG_ID,
                FeedReaderContract.FeedEntry.COLUMN_NAME_ENTRY_HOUR
        };

// Filter results WHERE "title" = 'My Title'
        String selection = FeedReaderContract.FeedEntry.COLUMN_NAME_TAG_ID + " = ?";
        String[] selectionArgs = { "100 234 99 122" };

// How you want the results sorted in the resulting Cursor
        String sortOrder =
                FeedReaderContract.FeedEntry.COLUMN_NAME_TAG_ID + " DESC";

        Cursor cursor = db.query(
                FeedReaderContract.FeedEntry.TABLE_NAME,   // The table to query
                projection,             // The array of columns to return (pass null to get all)
                selection,              // The columns for the WHERE clause
                selectionArgs,          // The values for the WHERE clause
                null,                   // don't group the rows
                null,                   // don't filter by row groups
                sortOrder               // The sort order
        );

        List<Long> itemIds = new ArrayList<>();
        while(cursor.moveToNext()) {
            long itemId = cursor.getLong(
                    cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntry._ID));
            String driverName = cursor.getString(
                    cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntry.COLUMN_NAME_FULL_NAME));
            String tagId = cursor.getString(
                    cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntry.COLUMN_NAME_TAG_ID));
            TableRow tr = new TableRow(this);
            tr.setGravity(Gravity.CENTER);
            tr.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT));
            TextView txtName = new TextView(this);
            txtName.setText(driverName);
            //txtName.setGravity(View.TEXT_ALIGNMENT_CENTER);
            //txtName.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT));
            TextView txtTagId = new TextView(this);
            txtTagId.setText(tagId);
            //txtTagId.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT));
            tr.addView(txtName);
            tr.addView(txtTagId);
            ImageButton b = new ImageButton(this);
            b.setImageResource(R.drawable.baseline_visibility_24);
            ImageButton b1 = new ImageButton(this);
            b1.setImageResource(R.drawable.sharp_calendar_clock_24);
            ImageButton b2 = new ImageButton(this);
            b2.setImageResource(R.drawable.baseline_delete_24);
          //  b.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT));
           // b1.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT));
            //b2.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT));
            tr.addView(b);
            tr.addView(b1);
            tr.addView(b2);
            /* Add row to TableLayout. */
            tableChoferes.addView(tr, new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.MATCH_PARENT));
        }
        cursor.close();

    //tr.setBackgroundResource(R.drawable.sf_gradient_03);

    }
}