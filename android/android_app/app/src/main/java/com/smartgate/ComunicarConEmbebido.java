package com.smartgate;

import static androidx.activity.result.ActivityResultCallerKt.registerForActivityResult;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.android_app.R;
import com.smartgate.DB.DbChoferes;
import com.smartgate.entidades.Chofer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/*********************************************************************************************************
 * Activity que realiza la comunicacion con esp32
 **********************************************************************************************************/

//******************************************** Hilo principal del Activity**************************************
public class ComunicarConEmbebido extends AppCompatActivity
{

    Button btnBarrera;
    TextView txtNombre, txtApellido, txtTurno, txtRFID, txtEstadoLlegada;

    private ActivityResultLauncher<Intent> bluetoothActivityResultLauncher;

    Handler bluetoothIn;
    final int handlerState = 0; //used to identify handler message

    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private final StringBuilder recDataString = new StringBuilder();

    private ConnectedThread mConnectedThread;

    private DbChoferes dbChoferes;

    // SPP UUID service  - Funciona en la mayoria de los dispositivos
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // String for MAC address del Hc05
    private static String address = null;

    private static final String MOVER_BARRERA_CODE = "P";
    private static final String AUTORIZADO_TARDE_CODE = "T";
    private static final String AUTORIZADO_A_TIEMPO_CODE = "A";
    private static final String NO_AUTORIZADO_CODE = "N";


    String[] permissions = new String[]{
            android.Manifest.permission.BLUETOOTH,
            android.Manifest.permission.BLUETOOTH_ADMIN,
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_SCAN
    };

    public static final int MULTIPLE_PERMISSIONS = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comunicacion);

        //Se definen los componentes del layout
        btnBarrera = (Button) findViewById(R.id.btnBarrera);
        txtNombre = (TextView) findViewById(R.id.txtNombreChoferInfo);
        txtApellido = (TextView) findViewById(R.id.txtApellidoInfo);
        txtTurno = (TextView) findViewById(R.id.txtTurnoInfo);
        txtRFID = (TextView) findViewById(R.id.txtRFIDInfo);
        txtEstadoLlegada = (TextView) findViewById(R.id.txtHorarioLlegada);

        //obtengo el adaptador del bluethoot
        btAdapter = BluetoothAdapter.getDefaultAdapter();

        //defino el Handler de comunicacion entre el hilo Principal  el secundario.
        //El hilo secundario va a mostrar informacion al layout atraves utilizando indeirectamente a este handler
        bluetoothIn = Handler_Msg_Hilo_Principal();

        //defino los handlers para los botones Apagar y encender
        btnBarrera.setOnClickListener(btnMoverBarrera);

        dbChoferes = new DbChoferes(ComunicarConEmbebido.this);

        if (checkPermissions())
        {
            // Inicializar y registrar el ActivityResultLauncher
            bluetoothActivityResultLauncher = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    new ActivityResultCallback<ActivityResult>()
                    {
                        @Override
                        public void onActivityResult(ActivityResult result)
                        {
                            if (result.getResultCode() == RESULT_OK)
                            {
                                // Bluetooth ha sido habilitado
                                showAlert("ÉXITO", "Bluetooth ha sido habilitado");
                            } else
                            {
                                // El usuario no habilitó Bluetooth o la solicitud fue cancelada
                                showAlert("ERROR", "Bluetooth NO ha sido habilitado");
                            }
                        }
                    }
            );
        } else
        {
            finish();
        }

    }

    private boolean checkPermissions()
    {
        int result;
        List<String> listPermissionsNeeded = new ArrayList<>();

        //Se chequea si la version de Android es menor a la 6


        for (String p : permissions)
        {
            result = ContextCompat.checkSelfPermission(this, p);
            if (result != PackageManager.PERMISSION_GRANTED)
            {
                listPermissionsNeeded.add(p);
            }
        }
        if (!listPermissionsNeeded.isEmpty())
        {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[0]), MULTIPLE_PERMISSIONS);
            return false;
        }
        return true;
    }

    private void showAlert(String title, String text)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Configura el título y mensaje del AlertDialog
        builder.setTitle(title);
        builder.setMessage(text);

        // Configura el botón positivo
        builder.setPositiveButton("Cerrar", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                // Acción a realizar cuando el usuario hace clic en el botón OK
                dialog.dismiss();
            }
        });

        // Crea y muestra el AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        txtEstadoLlegada.setVisibility(View.INVISIBLE);
    }

    @SuppressLint("MissingPermission")
    @Override
    //Cada vez que se detecta el evento OnResume se establece la comunicacion con el HC05, creando un
    //socketBluethoot
    public void onResume()
    {
        super.onResume();
        if (checkPermissions())
        {
            //Obtengo el parametro, aplicando un Bundle, que me indica la Mac Adress del HC05
            Intent intent = getIntent();
            Bundle extras = intent.getExtras();

            address = extras.getString("Direccion_Bluetooth");

            BluetoothDevice device = btAdapter.getRemoteDevice(address);

            //se realiza la conexion del Bluethoot crea y se conectandose a atraves de un socket
            try
            {
                btSocket = createBluetoothSocket(device);
            } catch (IOException e)
            {
                showToast("La creacción del Socket fallo");
            }
            // Establish the Bluetooth socket connection.
            try
            {
                btSocket.connect();
            } catch (IOException e)
            {
                try
                {
                    btSocket.close();
                } catch (IOException e2)
                {
                    //insert code to deal with this
                }
            }

            //Una establecida la conexion con el Hc05 se crea el hilo secundario, el cual va a recibir
            // los datos de Arduino atraves del bluethoot
            mConnectedThread = new ConnectedThread(btSocket);
            mConnectedThread.start();

            //I send a character when resuming.beginning transmission to check device is connected
            //If it is not an exception will be thrown in the write method and finish() will be called
            mConnectedThread.write("Conexion establecida");
        }

    }


    @Override
    //Cuando se ejecuta el evento onPause se cierra el socket Bluethoot, para no recibiendo datos
    public void onPause()
    {
        super.onPause();
        try
        {
            //Don't leave Bluetooth sockets open when leaving activity
            if (btSocket != null)
            {
                btSocket.close();
            }
        } catch (IOException e2)
        {
            //insert code to deal with this
        }
    }

    //Metodo que crea el socket bluethoot
    @SuppressLint("MissingPermission")
    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException
    {

        return device.createRfcommSocketToServiceRecord(BTMODULEUUID);
    }

    //Handler que sirve que permite mostrar datos en el Layout al hilo secundario
    private Handler Handler_Msg_Hilo_Principal()
    {
        return new Handler(Looper.getMainLooper())
        {
            public void handleMessage(@NonNull Message msg)
            {
                //si se recibio un msj del hilo secundario
                if (msg.what == handlerState)
                {
                    //voy concatenando el msj
                    String readMessage = (String) msg.obj;
                    recDataString.append(readMessage);
                    int endOfLineIndex = recDataString.indexOf("\n");

                    //cuando recibo toda una linea la muestro en el layout
                    if (endOfLineIndex > 0)
                    {
                        String dataInPrint = recDataString.substring(0, endOfLineIndex).trim();
                        txtRFID.setText(dataInPrint);

                        recDataString.delete(0, recDataString.length());

                        Chofer chofer = dbChoferes.getChoferByRFID(dataInPrint);
                        if (chofer == null)
                        {
                            txtNombre.setText("NO REGISTRADO");
                            txtApellido.setText("NO REGISTRADO");
                            txtTurno.setText("NO REGISTRADO");
                            txtEstadoLlegada.setText("NO AUTORIZADO");
                            txtEstadoLlegada.setVisibility(View.VISIBLE);
                            txtEstadoLlegada.setBackgroundColor(Color.RED);
                            mConnectedThread.write(NO_AUTORIZADO_CODE);
                        } else
                        {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                            {
                                LocalTime expected = LocalTime.parse(chofer.getTurno());
                                LocalTime now = LocalTime.now();

                                if (now.isAfter(expected))
                                {
                                    txtNombre.setText(chofer.getNombre());
                                    txtApellido.setText(chofer.getApellido());
                                    txtTurno.setText(chofer.getTurno());
                                    txtEstadoLlegada.setVisibility(View.VISIBLE);
                                    txtEstadoLlegada.setText("AUTORIZADO - TARDE");
                                    txtEstadoLlegada.setBackgroundColor(R.color.colorBlue);
                                    mConnectedThread.write(AUTORIZADO_TARDE_CODE);
                                } else
                                {
                                    txtNombre.setText(chofer.getNombre());
                                    txtApellido.setText(chofer.getApellido());
                                    txtTurno.setText(chofer.getTurno());
                                    txtEstadoLlegada.setVisibility(View.VISIBLE);
                                    txtEstadoLlegada.setText("AUTORIZADO - A TIEMPO");
                                    txtEstadoLlegada.setBackgroundColor(R.color.colorSuccess);
                                    mConnectedThread.write(AUTORIZADO_A_TIEMPO_CODE);
                                }
                            }
                        }
                    }
                }
            }
        };

    }


    private final View.OnClickListener btnMoverBarrera = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            mConnectedThread.write(MOVER_BARRERA_CODE);
        }
    };



    private void showToast(String message)
    {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    //******************************************** Hilo secundario del Activity**************************************
    //*************************************** recibe los datos enviados por el HC05**********************************

    private class ConnectedThread extends Thread
    {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        //Constructor de la clase del hilo secundario
        public ConnectedThread(BluetoothSocket socket)
        {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try
            {
                //Create I/O streams for connection
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e)
            {
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        //metodo run del hilo, que va a entrar en una espera activa para recibir los msjs del HC05
        public void run()
        {
            byte[] buffer = new byte[256];
            int bytes;

            //el hilo secundario se queda esperando mensajes del HC05
            while (true)
            {
                try
                {
                    //se leen los datos del Bluethoot
                    bytes = mmInStream.read(buffer);
                    String readMessage = new String(buffer, 0, bytes);

                    //se muestran en el layout de la activity, utilizando el handler del hilo
                    // principal antes mencionado
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e)
                {
                    break;
                }
            }
        }


        //write method
        public void write(String input)
        {
            byte[] msgBuffer = input.getBytes();           //converts entered String into bytes
            try
            {
                mmOutStream.write(msgBuffer);                //write bytes over BT connection via outstream
            } catch (IOException e)
            {
                //if you cannot write, close the application
                showToast("La conexion fallo");
                finish();

            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode)
        {
            case MULTIPLE_PERMISSIONS:
            {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    // permissions granted.
                    //enableComponent(); // Now you call here what ever you want :)
                } else
                {
                    String perStr = "";
                    for (String per : permissions)
                    {
                        perStr += "\n" + per;
                    }
                    // permissions list of don't granted permission
                    Toast.makeText(this, "ATENCION: La aplicacion no funcionara " +
                            "correctamente debido a la falta de Permisos", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }

}
