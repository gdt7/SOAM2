package com.example.android_app;


import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.BaseColumns;
import android.util.Log;
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

    BluethootService btService;
    boolean btBounded = false;
    private static final String TAG = "ListadoChoferes";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG,"oncreate");
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_listado_choferes);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Intent mIntent = new Intent(this, BluethootService.class);
        bindService(mIntent, mConnection, BIND_AUTO_CREATE);

       // btService.setHandler(mHandler);

        tableChoferes = (TableLayout) findViewById(R.id.tableChoferes);

        ChoferDbHelper dbHelp = new ChoferDbHelper(this);
        // dbHelp.insertChofer("JOSE SOSA","122 322 112 442","15");


        List<Chofer> choferes = new ArrayList<>();
        choferes.addAll(dbHelp.findChoferById("122 322 112 442"));

        for (Chofer c : choferes) {
            String driverName = c.getFullName();
            String tagId = c.getTagId();
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
    }

    ServiceConnection mConnection = new ServiceConnection() {

        public void onServiceDisconnected(ComponentName name) {
            btBounded = false;
            btService = null;
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            // Toast.makeText(context, "Service is connected", 1000).show();
            Log.d(TAG,"entro service connected");
            BluethootService.LocalBinder mLocalBinder = (BluethootService.LocalBinder) service;
            btService = mLocalBinder.getInstance();
            btBounded = true;
            btService.setHandler(mHandler);
        }
    };


    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            //FragmentActivity activity = getActivity();
            switch (msg.what) {
                case BluethootService.Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluethootService.STATE_CONNECTED:

                            break;
                        case BluethootService.STATE_CONNECTING:

                            break;
                        case BluethootService.STATE_LISTEN:
                        case BluethootService.STATE_NONE:

                            break;
                    }
                    break;
                case BluethootService.Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    //mConversationArrayAdapter.add("Me:  " + writeMessage);
                    break;
                case BluethootService.Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    //mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + readMessage);
                    //LEO MENSAJE E INSTANCIO EL POPUP PARA QUE EL USUARIO DECIDA QUE QUIERE HACER
                    DialogBuilder.buildAuthorizationDialog(getBaseContext());
                    break;
            }
        }
    };
}