package de.labystudio.desktopmodules.crypto.api.model;

public class ValueAtTime {

    private long timestamp;
    private double price;
    private double volume24h;

    public long getTimestamp() {
        return timestamp;
    }

    public double getPrice() {
        return price;
    }

    public double getVolume24h() {
        return volume24h;
    }
}
