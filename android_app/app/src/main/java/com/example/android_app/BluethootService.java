package com.example.android_app;

import static com.example.android_app.BluethootService.Constants.MAC_ESP32;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;

import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class BluethootService extends Service {
    private static final int REQUEST_ENABLE_BT = 1;
    //La constante REQUEST_LOCATION_PERMISSION es utilizada para identificar la solicitud de permisos de ubicación cuando se llama a ActivityCompat.requestPermissions.
    // Esto es necesario para gestionar la respuesta del usuario a la solicitud de permisos en el método onRequestPermissionsResult
    private static final int REQUEST_LOCATION_PERMISSION = 2;
    private BluetoothAdapter btAdapter;

    private ArrayAdapter<String> devicesArrayAdapter;

    private BluetoothSocket btSocket = null;

    private ConnectThread mConnectThread;
    private static ConnectedThread mConnectedThread;

    // SPP UUID service  - Funciona en la mayoria de los dispositivos
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // String for MAC address del Hc05
    private static String address = null;

    Handler bluetoothIn;
    final int handlerState = 0;
    private final StringBuilder recDataString = new StringBuilder();

    public static BluetoothDevice device = null;

    public static final String BT_DEVICE = "btdevice";
    public static final String SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB";
    public static final int STATE_NONE = 0; // we're doing nothing
    public static final int STATE_LISTEN = 1; // now listening for incoming
    // connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing
    // connection
    public static final int STATE_CONNECTED = 3; // now connected to a remote
    // device
    private static Handler mHandler = null;
    public static int mState = STATE_NONE;
    private String emmbeddedMac = "";
    private final IBinder mIBinder = new LocalBinder();

    public interface Constants {

        // Message types sent from the BluetoothChatService Handler
        int MESSAGE_STATE_CHANGE = 1;
        int MESSAGE_READ = 2;
        int MESSAGE_WRITE = 3;

        // Key names received from the BluetoothChatService Handler
        String DEVICE_NAME = "device_name";
        String TOAST = "toast";

        String MAC_ESP32 = "";
    }

    /**
     * Constructor. Prepares a new BluetoothChat session.
     *
     * @param context The UI Activity Context
     * @param handler A Handler to send messages back to the UI Activity
     */
    public BluethootService(Context context,Handler handler){
        mHandler = handler;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Inicializar el adaptador Bluetooth
        btAdapter = BluetoothAdapter.getDefaultAdapter();

        // Inicializar el ArrayAdapter para la lista de dispositivos
       // devicesArrayAdapter = new ArrayAdapter<>(ctx, android.R.layout.simple_list_item_1);

        IntentFilter filter = new IntentFilter();

        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED); //Cambia el estado del Bluethoot (Acrtivado /Desactivado)
        filter.addAction(BluetoothDevice.ACTION_FOUND); //Se encuentra un dispositivo bluethoot al realizar una busqueda
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED); //Cuando se comienza una busqueda de bluethoot
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED); //cuando la busqueda de bluethoot finaliza

        //se define (registra) el handler que captura los broadcast anterirmente mencionados.
        registerReceiver(receiver, filter);


        //defino el Handler de comunicacion entre el hilo Principal  el secundario.
        //El hilo secundario va a mostrar informacion al layout atraves utilizando indeirectamente a este handler
        bluetoothIn = Handler_Msg_Hilo_Principal();

        super.onCreate();
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mIBinder;
    }


    @SuppressLint("MissingPermission")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter != null) {
            device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
             String deviceName = device.getName();
            String macAddress = device.getAddress();
            if (macAddress != null && macAddress.length() > 0) {
                connectToDevice(MAC_ESP32);
            } else {
                stopSelf();
                return START_STICKY_COMPATIBILITY;
            }
        }
        String stopservice = intent.getStringExtra("stopservice");
        if (stopservice != null && stopservice.length() > 0) {
            stop();
        }
        return START_STICKY;
    }

    private void connectionFailed() {
        BluethootService.this.stop();
        //Message msg = mHandler.obtainMessage(AbstractActivity.MESSAGE_TOAST);
        Message msg = mHandler.obtainMessage(1);
        Bundle bundle = new Bundle();
        bundle.putString("1","1");
        bundle.putString(Constants.TOAST, "Unable to connect device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        BluethootService.this.connectToDevice(device.getAddress());
    }

    private void connectionLost() {
        BluethootService.this.stop();
        //Message msg = mHandler.obtainMessage(AbstractActivity.MESSAGE_TOAST);
        Message msg = mHandler.obtainMessage(1);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }


    private synchronized void connectToDevice(String macAddress) {

        BluetoothDevice device = btAdapter.getRemoteDevice(macAddress);
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }


    private synchronized void connected(BluetoothSocket mmSocket, BluetoothDevice mmDevice) {
        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        mConnectedThread = new ConnectedThread(mmSocket);
        mConnectedThread.start();

        // Message msg =
        // mHandler.obtainMessage(AbstractActivity.MESSAGE_DEVICE_NAME);
        // Bundle bundle = new Bundle();
        // bundle.putString(AbstractActivity.DEVICE_NAME, "p25");
        // msg.setData(bundle);
        // mHandler.sendMessage(msg);
        setState(STATE_CONNECTED);

    }
    private void setState(int state) {
        BluethootService.mState = state;
        if (mHandler != null) {
           // mHandler.obtainMessage(AbstractActivity.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
        }
    }

    @SuppressLint("MissingPermission")
    public synchronized void stop() {
        setState(STATE_NONE);
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        if (btAdapter != null) {
            btAdapter.cancelDiscovery();
        }
        stopSelf();
    }

    @SuppressLint("MissingPermission")
    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {

        return device.createRfcommSocketToServiceRecord(BTMODULEUUID);
    }

    //Handler que sirve que permite mostrar datos en el Layout al hilo secundario
    private Handler Handler_Msg_Hilo_Principal() {
        return new Handler(Looper.getMainLooper()) {
            public void handleMessage(@NonNull android.os.Message msg) {
                //si se recibio un msj del hilo secundario
                if (msg.what == handlerState) {
                    //voy concatenando el msj
                    String readMessage = (String) msg.obj;
                    recDataString.append(readMessage);
                    int endOfLineIndex = recDataString.indexOf("\r\n");

                    //cuando recibo toda una linea la muestro en el layout
                    if (endOfLineIndex > 0) {
                        String dataInPrint = recDataString.substring(0, endOfLineIndex);
                        //txtPotenciometro.setText(dataInPrint);

                        recDataString.delete(0, recDataString.length());
                    }
                }
            }
        };

    }


    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Dispositivo encontrado
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                String deviceName = device.getName();
                String deviceAddress = device.getAddress();
                devicesArrayAdapter.add(deviceName + "\n" + deviceAddress);
                devicesArrayAdapter.notifyDataSetChanged();
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                // Búsqueda finalizada
                //Toast.makeText(InteractuarEmbebidoActivity.this, "Búsqueda de dispositivos finalizada", Toast.LENGTH_SHORT).show();
            }

            //Si cambio de estado el Bluethoot(Activado/desactivado)
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                //Obtengo el parametro, aplicando un Bundle, que me indica el estado del Bluethoot
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                //Si esta activado
                if (state == BluetoothAdapter.STATE_ON) {

                }
            }
            //Si se inicio la busqueda de dispositivos bluethoot
            else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {

            }
            //Si finalizo la busqueda de dispositivos bluethoot
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {

            }
        }
    };

    //******************************************** Hilo secundario del Activity**************************************
    //*************************************** recibe los datos enviados por el HC05**********************************



    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        @SuppressLint("MissingPermission")
        public ConnectThread(BluetoothDevice device) {
            this.mmDevice = device;
            BluetoothSocket tmp = null;
            try {
                tmp = device.createRfcommSocketToServiceRecord(UUID.fromString(SPP_UUID));
            } catch (IOException e) {
                e.printStackTrace();
            }
            mmSocket = tmp;
        }

        @SuppressLint("MissingPermission")
        @Override
        public void run() {
            setName("ConnectThread");
            btAdapter.cancelDiscovery();
            try {
                mmSocket.connect();
            } catch (IOException e) {
                try {
                    mmSocket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                connectionFailed();
                return;

            }
            synchronized (BluethootService.this) {
                mConnectThread = null;
            }
            connected(mmSocket, mmDevice);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                //Log.e("PrinterService", "close() of connect socket failed", e);
            }
        }
    }


    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                //Log.e("Printer Service", "temp sockets not created", e);
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        @Override
        public void run()
        {
            byte[] buffer = new byte[256];
            int bytes;

            //el hilo secundario se queda esperando mensajes del HC05
            while (true)
            {
                try
                {
                    //se leen los datos del Bluethoot(ACA DEBERIA MANEJAR LA LOGICA DE GENERAR EL POPUP
                    bytes = mmInStream.read(buffer);
                    String readMessage = new String(buffer, 0, bytes);

                    //se muestran en el layout de la activity, utilizando el handler del hilo
                    // principal antes mencionado
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();



                } catch (IOException e) {
                    break;
                }
            }
        }


        private byte[] btBuff;


        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);

                // Share the sent message back to the UI Activity
                //mHandler.obtainMessage(MainActivity.MESSAGE_WRITE, buffer.length, -1, buffer).sendToTarget();
                mHandler.obtainMessage(1, buffer.length, -1, buffer).sendToTarget();
            } catch (IOException e) {
               // Log.e("PrinterService", "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();

            } catch (IOException e) {
                //Log.e("PrinterService", "close() of connect socket failed", e);
            }
        }

    }


    @Override
    public void onDestroy()
    {
        if(mHandler != null)
        {
            mHandler = null;
        }
    }

    public class LocalBinder extends Binder
    {
        public BluethootService getInstance()
        {
            return BluethootService.this;
        }
    }

    public void setHandler(Handler handler)
    {
        mHandler = handler;
    }


    //Funcion para enviar mensajes a traves del ConnectedThread
    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }

}
