package de.labystudio.desktopmodules.crypto.api;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import de.labystudio.desktopmodules.crypto.api.model.ValueAtTime;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class BlockChainApi {

    private static final String URL_TO_BTC = "https://blockchain.info/tobtc?currency=%s&value=%s";
    private static final String URL_CURRENCY_SERIES = "https://api.blockchain.info/price/index-series?base=%s&quote=%s&scale=%s&start=%s&end=%s";

    private final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private final Gson GSON = new Gson();

    public void requestSeriesAsync(CurrencyType base, CurrencyType quote, int rangeHours, Consumer<ValueAtTime[]> callback) {
        long end = System.currentTimeMillis() / 1000L;
        long start = end - 60L * 60 * rangeHours;
        this.requestAsync(
                String.format(URL_CURRENCY_SERIES, base.name(), quote.name(), SeriesScaleType.FIFTEEN_MINUTES.value(), start, end),
                ValueAtTime[].class,
                response -> {
                    if (response != null) {
                        callback.accept((ValueAtTime[]) response);
                    }
                }
        );
    }

    @SuppressWarnings("SameParameterValue")
    private <T> void requestAsync(String url, Class<?> type, Consumer<T> callback) {
        this.EXECUTOR.execute(() -> {
            try {
                callback.accept(this.request(url, type));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private <T> T request(String url, Class<?> type) throws IOException {
        HttpsURLConnection connection = (HttpsURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        int code = connection.getResponseCode();
        if (code / 100 != 2) {
            return null;
        }

        try (JsonReader reader = new JsonReader(new InputStreamReader(connection.getInputStream()))) {
            return this.GSON.fromJson(reader, type);
        }
    }

}
