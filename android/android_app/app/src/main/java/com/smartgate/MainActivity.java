package com.smartgate;

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

import com.example.android_app.R;

public class MainActivity extends AppCompatActivity implements SensorEventListener
{


    private static final int ACCELERATION_UMBRAL = 8;

    private static boolean isDarkTheme = false;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;

    private static final String MAC_BLUETOOTH = "EC:94:CB:6A:FB:0E";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        // Aplicar el tema predeterminado
        setTheme(isDarkTheme ? com.google.android.material.R.style.Base_Theme_Material3_Light : com.google.android.material.R.style.Base_Theme_Material3_Dark);

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) ->
        {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Inicialización del SensorManager y acelerómetro
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (mSensorManager != null)
        {
            mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }


        Button btnListadoChoferes = findViewById(R.id.btnListadoChoferes);
        btnListadoChoferes.setOnClickListener(irAlistadoChoferes);

        Button btnInteractuar = findViewById(R.id.btnInteractuarEmbebido);
        btnInteractuar.setOnClickListener(irAInteractuar);

        Button btnCrearChofer = findViewById(R.id.btnAgregarChofer);
        btnCrearChofer.setOnClickListener(irACrearChofer);

    }

    private final View.OnClickListener irAlistadoChoferes = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            Intent i = new Intent(MainActivity.this, ListadoChoferesActivity.class);
            startActivity(i);
        }
    };

    private final View.OnClickListener irACrearChofer = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            Intent i = new Intent(MainActivity.this, NuevoChoferActivity.class);
            startActivity(i);
        }
    };

    private final View.OnClickListener irAInteractuar = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            Intent i = new Intent(MainActivity.this, ComunicarConEmbebido.class);
            i.putExtra("Direccion_Bluetooth", MAC_BLUETOOTH);
            startActivity(i);
        }
    };

    @Override
    protected void onPause()
    {
        super.onPause();
        Parar_Sensores();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        Ini_Sensores();
    }

    @Override
    protected void onDestroy()
    {
        Parar_Sensores();
        super.onDestroy();
    }


    // Método para inicializar los sensores
    protected void Ini_Sensores()
    {
        if (mSensorManager != null && mAccelerometer != null)
        {
            mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        } else
        {
            Log.e(TAG, "Ini_Sensores: SensorManager or Accelerometer not initialized");
        }
    }

    // Método para parar la escucha de los sensores
    private void Parar_Sensores()
    {
        if (mSensorManager != null && mAccelerometer != null)
        {
            mSensorManager.unregisterListener((SensorEventListener) this, mAccelerometer);
        } else
        {
            Log.e(TAG, "Parar_Sensores: SensorManager or Accelerometer not initialized");
        }
    }


    //Este método se llama cada vez que hay un cambio en los datos de un sensor que está registrado con un SensorEventListener
    @Override
    public void onSensorChanged(SensorEvent event)
    {
        synchronized (this)
        {

            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            {
                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];

                double acceleration = Math.sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH;


                if (acceleration > ACCELERATION_UMBRAL)
                {
                    toggleTheme();
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {
        // Método necesario para implementar SensorEventListener
    }

    private void toggleTheme()
    {
        isDarkTheme = !isDarkTheme;
        recreate();
    }
}
