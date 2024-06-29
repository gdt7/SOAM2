package com.smartgate.DB;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.Nullable;

import com.smartgate.entidades.Chofer;

import java.util.ArrayList;

public class DbChoferes extends DbHelper
{

    Context context;

    public DbChoferes(@Nullable Context context)
    {
        super(context);
        this.context = context;
    }

    public long insertarChofer(String nombre, String apellido, String turno, String codigoRFID)
    {

        long id = 0;

        try
        {
            DbHelper dbHelper = new DbHelper(context);
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            ContentValues values = new ContentValues();
            values.put("nombre", nombre);
            values.put("apellido", apellido);
            values.put("turno", turno);
            values.put("codigorfid", codigoRFID);

            id = db.insert(TABLE_CHOFERES, null, values);
        } catch (Exception ex)
        {
            ex.toString();
        }

        return id;
    }

    public ArrayList<Chofer> mostrarChoferes()
    {

        DbHelper dbHelper = new DbHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ArrayList<Chofer> listaChoferes = new ArrayList<>();
        Chofer chofer;
        Cursor cursorChoferes;

        cursorChoferes = db.rawQuery("SELECT * FROM " + TABLE_CHOFERES + " ORDER BY nombre ASC", null);

        if (cursorChoferes.moveToFirst())
        {
            do
            {
                chofer = new Chofer();
                chofer.setId(cursorChoferes.getInt(0));
                chofer.setNombre(cursorChoferes.getString(1));
                chofer.setApellido(cursorChoferes.getString(2));
                chofer.setTurno(cursorChoferes.getString(3));
                chofer.setCodigoRFID(cursorChoferes.getString(4));
                listaChoferes.add(chofer);
            } while (cursorChoferes.moveToNext());
        }

        cursorChoferes.close();

        return listaChoferes;
    }

    public Chofer verChofer(int id)
    {

        DbHelper dbHelper = new DbHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        Chofer chofer = null;
        Cursor cursorChoferes;

        cursorChoferes = db.rawQuery("SELECT * FROM " + TABLE_CHOFERES + " WHERE id = " + id + " LIMIT 1", null);

        if (cursorChoferes.moveToFirst())
        {
            chofer = new Chofer();
            chofer.setId(cursorChoferes.getInt(0));
            chofer.setNombre(cursorChoferes.getString(1));
            chofer.setApellido(cursorChoferes.getString(2));
            chofer.setTurno(cursorChoferes.getString(4));
            chofer.setCodigoRFID(cursorChoferes.getString(3));
        }

        cursorChoferes.close();

        return chofer;
    }

    public Chofer getChoferByRFID(String rfid)
    {

        DbHelper dbHelper = new DbHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        Chofer chofer = null;
        Cursor cursorChoferes;

        cursorChoferes = db.rawQuery("SELECT * FROM " + TABLE_CHOFERES + " WHERE codigorfid = \"" + rfid + "\" LIMIT 1", null);

        if (cursorChoferes.moveToFirst())
        {
            chofer = new Chofer();
            chofer.setId(cursorChoferes.getInt(0));
            chofer.setNombre(cursorChoferes.getString(1));
            chofer.setApellido(cursorChoferes.getString(2));
            chofer.setTurno(cursorChoferes.getString(4));
            chofer.setCodigoRFID(cursorChoferes.getString(3));
        }

        cursorChoferes.close();

        return chofer;
    }

    public boolean editarChofer(int id, String nombre, String apellido, String turno, String codigoRFID)
    {

        boolean correcto = false;

        DbHelper dbHelper = new DbHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        try
        {
            db.execSQL("UPDATE " + TABLE_CHOFERES + " SET nombre = '" + nombre + "', apellido = '" + apellido + "', turno = '" + turno + "', codigorfid = '" + codigoRFID + "' WHERE id='" + id + "' ");
            correcto = true;
        } catch (Exception ex)
        {
            ex.toString();
            correcto = false;
        } finally
        {
            db.close();
        }
        return correcto;
    }

    public boolean eliminarChofer(int id)
    {

        boolean correcto = false;

        DbHelper dbHelper = new DbHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        try
        {
            db.execSQL("DELETE FROM " + TABLE_CHOFERES + " WHERE id = '" + id + "'");
            correcto = true;
        } catch (Exception ex)
        {
            ex.toString();
            correcto = false;
        } finally
        {
            db.close();
        }

        return correcto;
    }
}
