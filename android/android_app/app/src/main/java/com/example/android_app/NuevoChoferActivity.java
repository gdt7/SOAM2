package com.example.android_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.android_app.DB.DbChoferes;

public class NuevoChoferActivity extends AppCompatActivity {

    EditText txtNombre;
    EditText txtApellido;
    EditText txtTurno;
    EditText txtCodigo;

    Button btnGuardarChofer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_nuevo_chofer);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        txtNombre = findViewById(R.id.txtNombre);
        txtApellido = findViewById(R.id.txtApellido);
        txtTurno = findViewById(R.id.txtTurno);
        txtCodigo = findViewById(R.id.txtCodigo);
        btnGuardarChofer = findViewById(R.id.btnGuardarChofer);

        btnGuardarChofer.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                DbChoferes dbChoferes = new DbChoferes(NuevoChoferActivity.this);
                long id = dbChoferes.insertarChofer(
                        txtNombre.getText().toString(),
                        txtApellido.getText().toString(),
                        txtTurno.getText().toString(),
                        txtCodigo.getText().toString()
                );

                if(id > 0) {
                    Toast.makeText(NuevoChoferActivity.this, "REGISTRO GUARDADO", Toast.LENGTH_LONG);
                    verRegistro();
                } else {
                    Toast.makeText(NuevoChoferActivity.this, "ERROR AL GUARDAR REGISTRO", Toast.LENGTH_LONG);
                }

            }
        });
    }

    private void verRegistro(){
        Intent intent = new Intent(this, ListadoChoferesActivity.class);
        startActivity(intent);
    }
    private void limpiar(){
        txtNombre.setText("");
        txtApellido.setText("");
        txtTurno.setText("");
        txtCodigo.setText("");
    }
}