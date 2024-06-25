package com.example.android_app;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;


public class MainActivity extends AppCompatActivity implements  SensorEventListener {
    private int param = 8;
    private static boolean isDarkTheme = false;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Aplicar el tema predeterminado
        setTheme(isDarkTheme ? com.google.android.material.R.style.Base_Theme_Material3_Light : com.google.android.material.R.style.Base_Theme_Material3_Dark);
        
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

         // Inicialización del SensorManager y acelerómetro
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (mSensorManager != null) {
            mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }


        Button btnListadoChoferes = findViewById(R.id.btnListadoChoferes);
        btnListadoChoferes.setOnClickListener(irAlistadoChoferes);

        Button btnInteractuar = findViewById(R.id.btnInteractuarEmbebido);
        btnInteractuar.setOnClickListener(irAInteractuar);

        Button btnCrearChofer = findViewById(R.id.btnAgregarChofer);
        btnCrearChofer.setOnClickListener(irACrearChofer);


        //BASE DE DATOS - Se debe hacer cuando ejecutas por primera vez la aplicaciòn y no creaste la DB
        //Button btnCrear = findViewById(R.id.btnCrear);
        //btnCrear.setOnClickListener(crearBD);

    }

    private final View.OnClickListener irAlistadoChoferes = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent i = new Intent(MainActivity.this, ListadoChoferesActivity.class);
            startActivity(i);
        }
    };

    private final View.OnClickListener irACrearChofer = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent i = new Intent(MainActivity.this, NuevoChoferActivity.class);
            startActivity(i);
        }
    };

    private final View.OnClickListener irAInteractuar = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String direccionBluethoot = "EC:94:CB:6A:FB:0E";
            //String direccionBluethoot = "24:DC:C3:A7:4F:96";
            Intent i = new Intent(MainActivity.this, ComunicarConEmbebido.class);
            i.putExtra("Direccion_Bluethoot", direccionBluethoot);
            startActivity(i);
        }
    };

    /*private final View.OnClickListener crearBD = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            DbHelper dbHelper = new DbHelper(MainActivity.this);
            SQLiteDatabase db =dbHelper.getWritableDatabase();
            if(db != null){
                Toast.makeText(MainActivity.this, "BASE DE DATOS CREADA", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(MainActivity.this, "ERROR AL CREAR BASE DE DATOS", Toast.LENGTH_LONG).show();
            }
        }
    };*/
    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: Stopping sensors");
        Parar_Sensores();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Starting sensors");
        Ini_Sensores();
    }

    @Override
    protected void onDestroy()
    {
        Parar_Sensores();
        super.onDestroy();
    }


    // Método para inicializar los sensores
    protected void Ini_Sensores() {
        if (mSensorManager != null && mAccelerometer != null) {
            mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            Log.d(TAG, "Ini_Sensores: Sensor listener registered");
        } else {
            Log.e(TAG, "Ini_Sensores: SensorManager or Accelerometer not initialized");
        }
    }

    // Método para parar la escucha de los sensores
    private void Parar_Sensores() {
        if (mSensorManager != null && mAccelerometer != null) {
            mSensorManager.unregisterListener((SensorEventListener) this, mAccelerometer);
            Log.d(TAG, "Parar_Sensores: Sensor listener unregistered");
        } else {
            Log.e(TAG, "Parar_Sensores: SensorManager or Accelerometer not initialized");
        }
    }


    //Este método se llama cada vez que hay un cambio en los datos de un sensor que está registrado con un SensorEventListener
    @Override
    public void onSensorChanged(SensorEvent event) {
        synchronized (this) {
            Log.d(TAG, "onSensorChanged: " + event.sensor.getName());

            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];

                double acceleration = Math.sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH;
                //Log.d(TAG, "Acceleration: " + acceleration);

                if (acceleration > param) {
                    Log.d(TAG, "onSensorChanged: Significant movement detected");
                    //playsound();
                    // Cambiar el tema
                    toggleTheme();
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Método necesario para implementar SensorEventListener
    }

    private void toggleTheme() {
       //Log.d(TAG, "CAMBIAR TEMA!!"); // Registra un mensaje de depuración indicando que se va a cambiar el tema
        isDarkTheme = !isDarkTheme;
        recreate();
    }
}
