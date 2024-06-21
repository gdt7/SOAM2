package com.example.android_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        Button btnListadoChoferes = findViewById(R.id.btnListadoChoferes);
        btnListadoChoferes.setOnClickListener(irAlistadoChoferes);

        Button btnInteractuar = findViewById(R.id.btnInteractuarEmbebido);
        btnInteractuar.setOnClickListener(irAInteractuar);

        Button btnCrearChofer = findViewById(R.id.btnAgregarChofer);
        btnCrearChofer.setOnClickListener(irACrearChofer);

        //Button btnConfigurarBluetooth = findViewById(R.id.btnBluetooth);
        //btnConfigurarBluetooth.setOnClickListener(irAConfigurarBluetooth);


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

    private final View.OnClickListener irAConfigurarBluetooth = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent i = new Intent(MainActivity.this, ConfigurarBluetooth.class);
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
            String direccionBluethoot = "6C:94:66:CB:44:67";
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
}
