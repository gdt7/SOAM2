package com.smartgate.adaptadores;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android_app.R;
import com.smartgate.VerChoferActivity;
import com.smartgate.entidades.Chofer;

import java.util.ArrayList;

public class ListaChoferesAdapter extends RecyclerView.Adapter<ListaChoferesAdapter.ChoferViewHolder>
{

    ArrayList<Chofer> listaChoferes;

    public ListaChoferesAdapter(ArrayList<Chofer> listaChoferes)
    {
        this.listaChoferes = listaChoferes;
    }

    @NonNull
    @Override
    public ChoferViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.lista_item_chofer, null, false);
        return new ChoferViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChoferViewHolder holder, int position)
    {
        holder.viewNombre.setText(listaChoferes.get(position).getNombre());
        holder.viewApellido.setText(listaChoferes.get(position).getApellido());
        holder.viewTurno.setText(listaChoferes.get(position).getTurno());
        holder.viewCodigo.setText(listaChoferes.get(position).getCodigoRFID());
    }

    @Override
    public int getItemCount()
    {
        return listaChoferes.size();
    }

    public class ChoferViewHolder extends RecyclerView.ViewHolder
    {

        TextView viewNombre, viewApellido, viewTurno, viewCodigo;
        ImageView iconInfo;

        public ChoferViewHolder(@NonNull View itemView)
        {
            super(itemView);
            viewNombre = itemView.findViewById(R.id.viewNombre);
            viewApellido = itemView.findViewById(R.id.viewApellido);
            viewTurno = itemView.findViewById(R.id.viewTurno);
            viewCodigo = itemView.findViewById(R.id.viewCodigo);
            iconInfo = itemView.findViewById(R.id.iconInfo);

            iconInfo.setOnClickListener(new View.OnClickListener()
            {

                @Override
                public void onClick(View view)
                {
                    Context context = view.getContext();
                    Intent intent = new Intent(context, VerChoferActivity.class);
                    intent.putExtra("ID", listaChoferes.get(getAdapterPosition()).getId());
                    context.startActivity(intent);
                }
            });
        }
    }
}
