import de.labystudio.desktopmodules.crypto.CryptoAddon;

public class StartCrypto {

    public static void main(String[] args) throws Exception {
        // Start the core with the addon
        Start.main(new String[]{CryptoAddon.class.getName()});
    }

}
