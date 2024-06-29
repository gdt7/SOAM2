package com.smartgate;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.android_app.R;
import com.smartgate.DB.DbChoferes;
import com.smartgate.entidades.Chofer;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class VerChoferActivity extends AppCompatActivity
{

    EditText txtNombre, txtApellido, txtTurno, txtCodigo;
    Button btnGuardar;

    FloatingActionButton fabEditar, fabEliminar;
    Chofer chofer;
    boolean correcto = false;
    int id = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ver_chofer);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) ->
        {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        txtNombre = findViewById(R.id.txtNombre);
        txtApellido = findViewById(R.id.txtApellido);
        txtTurno = findViewById(R.id.txtTurno);
        txtCodigo = findViewById(R.id.txtCodigo);
        fabEditar = findViewById(R.id.fabEditar);
        fabEliminar = findViewById(R.id.fabEliminar);
        btnGuardar = findViewById(R.id.btnGuardarChofer);

        if (savedInstanceState == null)
        {
            Bundle extras = getIntent().getExtras();
            if (extras == null)
            {
                id = Integer.parseInt(null);
            } else
            {
                id = extras.getInt("ID");
            }
        } else
        {
            id = (int) savedInstanceState.getSerializable("ID");
        }

        DbChoferes dbChoferes = new DbChoferes(VerChoferActivity.this);
        chofer = dbChoferes.verChofer(id);

        if (chofer != null)
        {
            txtNombre.setText(chofer.getNombre());
            txtApellido.setText(chofer.getApellido());
            txtTurno.setText(chofer.getTurno());
            txtCodigo.setText(chofer.getCodigoRFID());
            btnGuardar.setVisibility(View.INVISIBLE);
            //Para que no se muestre el teclado ya que es modo read
            txtNombre.setInputType(InputType.TYPE_NULL);
            txtApellido.setInputType(InputType.TYPE_NULL);
            txtTurno.setInputType(InputType.TYPE_NULL);
            txtCodigo.setInputType(InputType.TYPE_NULL);
        }

        fabEditar.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                txtNombre.setInputType(InputType.TYPE_CLASS_TEXT);
                txtApellido.setInputType(InputType.TYPE_CLASS_TEXT);
                txtTurno.setInputType(InputType.TYPE_CLASS_TEXT);
                txtCodigo.setInputType(InputType.TYPE_CLASS_TEXT);
                btnGuardar.setVisibility(View.VISIBLE);
                fabEditar.setVisibility(View.INVISIBLE);
                fabEditar.setVisibility(View.INVISIBLE);

                // Opcional: mover el cursor al final del texto
                txtNombre.setSelection(txtNombre.getText().length());
                txtApellido.setSelection(txtApellido.getText().length());
                txtTurno.setSelection(txtTurno.getText().length());
                txtCodigo.setSelection(txtCodigo.getText().length());
            }
        });

        fabEliminar.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(VerChoferActivity.this);
                builder.setMessage("¿Desea eliminar este chofer?")
                        .setPositiveButton("SI", new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i)
                            {

                                if (dbChoferes.eliminarChofer(id))
                                {
                                    verRegistro();
                                }
                            }
                        })
                        .setNegativeButton("NO", new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i)
                            {

                            }
                        }).show();
            }
        });

        btnGuardar.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if (!txtNombre.getText().toString().equals("") && !txtApellido.getText().toString().equals(""))
                {
                    correcto = dbChoferes.editarChofer(id, txtNombre.getText().toString(), txtApellido.getText().toString(), txtTurno.getText().toString(), txtCodigo.getText().toString());

                    if (correcto)
                    {
                        Toast.makeText(VerChoferActivity.this, "REGISTRO MODIFICADO", Toast.LENGTH_LONG).show();
                        verRegistro();
                    } else
                    {
                        Toast.makeText(VerChoferActivity.this, "ERROR AL MODIFICAR REGISTRO", Toast.LENGTH_LONG).show();
                    }
                } else
                {
                    Toast.makeText(VerChoferActivity.this, "DEBE LLENAR LOS CAMPOS OBLIGATORIOS", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void verRegistro()
    {
        Intent intent = new Intent(this, ListadoChoferesActivity.class);
        startActivity(intent);
    }
}