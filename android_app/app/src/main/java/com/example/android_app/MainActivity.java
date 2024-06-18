package com.example.android_app;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

public class MainActivity extends AppCompatActivity {

    private Button btnListadoChoferes;
    private Button btnInteractuar;
    private BluetoothAdapter btAdapter;

    private BluethootService mService = null;

    private boolean mIsBound;

    private static final String TAG = "MainACtivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG,"oncreate");
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btnListadoChoferes = findViewById(R.id.btnListadoChoferes);
        btnListadoChoferes.setOnClickListener(irAlistadoChoferes);

        btnInteractuar = findViewById(R.id.btn_int);
        btnInteractuar.setOnClickListener(irAInteractuar);

        // Obtener el BluetoothManager y el BluetoothAdapter
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            btAdapter = bluetoothManager.getAdapter();
        }

        //Start del servicio de BT y bindeo
        startService(new Intent(this.getBaseContext(), BluethootService.class));
        //doBindService();

    }

    @Override
    protected void onStart() {
        super.onStart();
        mService = new BluethootService(getApplicationContext(), mHandler);
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder)
        {
            mService = ((BluethootService.LocalBinder)iBinder).getInstance();
            mService.setHandler(mHandler);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName)
        {
            mService = null;
        }
    };



    private void doUnbindService()
    {
        if (mIsBound)
        {
            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        doUnbindService();
    }

    private void doBindService()
    {
        // Establish a connection with the service.  We use an explicit
        // class name because we want a specific service implementation that
        // we know will be running in our own process (and thus won't be
        // supporting component replacement by other applications).
        bindService(new Intent(this,
                BluethootService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    private final View.OnClickListener irAlistadoChoferes = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent i = new Intent(MainActivity.this, ListadoChoferesActivity.class);
            startActivity(i);
        }
    };

    private final View.OnClickListener irAInteractuar = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent i = new Intent(MainActivity.this, InteractuarEmbebidoActivity.class);
            startActivity(i);
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
