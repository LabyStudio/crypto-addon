package de.labystudio.desktopmodules.crypto.api;

public enum SeriesScaleType {
    ONE_HOUR(3600),
    FIFTEEN_MINUTES(900);

    private final long value;

    SeriesScaleType(int value) {
        this.value = value;
    }

    public long value() {
        return value;
    }
}
