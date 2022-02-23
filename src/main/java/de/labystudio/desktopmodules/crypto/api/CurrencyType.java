package de.labystudio.desktopmodules.crypto.api;

public enum CurrencyType {
    BTC("Bitcoin"),
    EUR("Euro"),
    USD("US Dollar"),
    ETH("Ethereum");

    private final String displayName;

    CurrencyType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return this.displayName;
    }
}
