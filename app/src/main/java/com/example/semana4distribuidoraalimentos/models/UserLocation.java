package com.example.semana4distribuidoraalimentos.models;

public class UserLocation {
    public double lat;       // Latitud
    public double lon;       // Longitud
    public long timestamp;   // Momento de captura (ms desde 1970)

    public UserLocation() {} // Constructor vacío requerido por Firebase

    public UserLocation(double lat, double lon, long timestamp) {
        this.lat = lat;
        this.lon = lon;
        this.timestamp = timestamp;
    }
}
