package com.example.android_app;


import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.List;

public class InteractuarEmbebidoActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 1;
    //La constante REQUEST_LOCATION_PERMISSION es utilizada para identificar la solicitud de permisos de ubicación cuando se llama a ActivityCompat.requestPermissions.
    // Esto es necesario para gestionar la respuesta del usuario a la solicitud de permisos en el método onRequestPermissionsResult
    private static final int REQUEST_LOCATION_PERMISSION = 2;
    private BluetoothAdapter adaptorBTW;
    private ArrayAdapter<String> devicesArrayAdapter;
    private Button boton_activar;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_interactuar);

        // Inicializar el adaptador Bluetooth
        adaptorBTW = BluetoothAdapter.getDefaultAdapter();

        // Referenciar el botón de activar Bluetooth
        boton_activar = findViewById(R.id.btn_activar);
        Button discoverDevicesButton = findViewById(R.id.btn_buscar);

        // Inicializar el ArrayAdapter para la lista de dispositivos
        devicesArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        // devicesListView.setAdapter(devicesArrayAdapter);

        // Configurar el texto inicial del botón
        updateBluetoothButtonText();

        // Configurar el listener para el botón de activar/desactivar Bluetooth
        boton_activar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleBluetooth();
            }
        });

        // Configurar el listener para el botón de descubrir dispositivos
        discoverDevicesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                discoverDevices();
            }
        });


        IntentFilter filter = new IntentFilter();

        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED); //Cambia el estado del Bluethoot (Acrtivado /Desactivado)
        filter.addAction(BluetoothDevice.ACTION_FOUND); //Se encuentra un dispositivo bluethoot al realizar una busqueda
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED); //Cuando se comienza una busqueda de bluethoot
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED); //cuando la busqueda de bluethoot finaliza

        //se define (registra) el handler que captura los broadcast anterirmente mencionados.
        registerReceiver(receiver, filter);


        // Registrar el BroadcastReceiver para descubrir dispositivos
        /*IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);

        // Registrar el BroadcastReceiver para cuando finaliza la búsqueda de dispositivos
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(receiver, filter);*/
    }

    private void toggleBluetooth() {
        if (adaptorBTW == null) {
            // El dispositivo no soporta Bluetooth
            Toast.makeText(this, "Bluetooth no es soportado en este dispositivo", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!adaptorBTW.isEnabled()) {
            adaptorBTW.enable();
            Toast.makeText(this, "Bluetooth activado", Toast.LENGTH_SHORT).show();
            boton_activar.setText("Desactivar Bluetooth"); // Cambiar texto del botón a "Desactivar Bluetooth"
        } else {
            adaptorBTW.disable();
            Toast.makeText(this, "Bluetooth desactivado", Toast.LENGTH_SHORT).show();
            boton_activar.setText("Activar Bluetooth"); // Cambiar texto del botón a "Activar Bluetooth"
        }
    }


    private void updateBluetoothButtonText() {
        if (adaptorBTW.isEnabled()) {
            boton_activar.setText("Desactivar Bluetooth");
        } else {
            boton_activar.setText("Activar Bluetooth");
        }
    }

    private void discoverDevices() {
        // Verificar permisos de ubicación
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        } else {
            doDiscovery();
        }
    }

    private void doDiscovery() {
        // Verificar si Bluetooth está activado
        if (adaptorBTW.isEnabled()) {
            // Cancelar la búsqueda previa si está activa
            if (adaptorBTW.isDiscovering()) {
                adaptorBTW.cancelDiscovery();
            }

            // Iniciar la búsqueda de dispositivos
            adaptorBTW.startDiscovery();
            Toast.makeText(this, "Buscando dispositivos...", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Bluetooth no está activado", Toast.LENGTH_SHORT).show();
        }
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Dispositivo encontrado
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceAddress = device.getAddress();
                devicesArrayAdapter.add(deviceName + "\n" + deviceAddress);
                devicesArrayAdapter.notifyDataSetChanged();
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                // Búsqueda finalizada
                Toast.makeText(InteractuarEmbebidoActivity.this, "Búsqueda de dispositivos finalizada", Toast.LENGTH_SHORT).show();
            }

            //Si cambio de estado el Bluethoot(Activado/desactivado)
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action))
            {
                //Obtengo el parametro, aplicando un Bundle, que me indica el estado del Bluethoot
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                //Si esta activado
                if (state == BluetoothAdapter.STATE_ON)
                {

                }
            }
            //Si se inicio la busqueda de dispositivos bluethoot
            else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action))
            {

            }
            //Si finalizo la busqueda de dispositivos bluethoot
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action))
            {

            }
            //si se encontro un dispositivo bluethoot
            else if (BluetoothDevice.ACTION_FOUND.equals(action))
            {

            }


        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Desregistrar el BroadcastReceiver
        unregisterReceiver(receiver);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                doDiscovery();
            } else {
                Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }
}