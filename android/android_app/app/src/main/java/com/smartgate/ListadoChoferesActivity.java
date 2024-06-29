package com.smartgate;


import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android_app.R;
import com.smartgate.DB.DbChoferes;
import com.smartgate.adaptadores.ListaChoferesAdapter;
import com.smartgate.entidades.Chofer;

import java.util.ArrayList;

public class ListadoChoferesActivity extends AppCompatActivity
{

    RecyclerView listaChoferes;
    ArrayList<Chofer> listaArrayChoferes;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_listado_choferes);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) ->
        {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        listaChoferes = findViewById(R.id.listaChoferes);
        listaChoferes.setLayoutManager(new LinearLayoutManager(this));

        DbChoferes dbChoferes = new DbChoferes(ListadoChoferesActivity.this);
        listaArrayChoferes = new ArrayList<>();

        ListaChoferesAdapter adapter = new ListaChoferesAdapter(dbChoferes.mostrarChoferes());
        listaChoferes.setAdapter(adapter);
    }
}