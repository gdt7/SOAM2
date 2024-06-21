package com.example.android_app.entidades;

public class Chofer {
    private int id;
    private String nombre;
    private String apellido;
    private String turno;
    private String codigoRFID;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getTurno() {
        return turno;
    }

    public void setTurno(String turno) {
        this.turno = turno;
    }

    public String getCodigoRFID() {
        return codigoRFID;
    }

    public void setCodigoRFID(String codigoRFID) {
        this.codigoRFID = codigoRFID;
    }
}
