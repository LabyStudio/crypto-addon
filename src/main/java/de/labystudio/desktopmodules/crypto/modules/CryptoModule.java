package de.labystudio.desktopmodules.crypto.modules;

import com.google.gson.JsonObject;
import de.labystudio.desktopmodules.core.loader.TextureLoader;
import de.labystudio.desktopmodules.core.module.Module;
import de.labystudio.desktopmodules.core.renderer.IRenderContext;
import de.labystudio.desktopmodules.core.renderer.font.Font;
import de.labystudio.desktopmodules.core.renderer.font.FontStyle;
import de.labystudio.desktopmodules.core.renderer.font.StringEffect;
import de.labystudio.desktopmodules.core.renderer.swing.SwingRenderContext;
import de.labystudio.desktopmodules.crypto.CryptoAddon;
import de.labystudio.desktopmodules.crypto.api.BlockChainApi;
import de.labystudio.desktopmodules.crypto.api.CurrencyType;
import de.labystudio.desktopmodules.crypto.api.SeriesScaleType;
import de.labystudio.desktopmodules.crypto.api.model.ValueAtTime;

import java.awt.*;
import java.awt.geom.Path2D;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;

public class CryptoModule extends Module<CryptoAddon> {

    private static final Font FONT = new Font("Arial", FontStyle.PLAIN, 12);

    private final BlockChainApi api = new BlockChainApi();
    private long timeNextRequest = -1L;

    private ValueAtTime[] series = new ValueAtTime[0];

    private Field graphicsField;
    private Graphics2D graphics;

    private double max;
    private double min;
    private double priceAtStartOfDay;

    public CryptoModule() {
        super(200, 30);

        try {
            this.graphicsField = SwingRenderContext.class.getDeclaredField("graphics");
            this.graphicsField.setAccessible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onInitialize(CryptoAddon addon, JsonObject config) {
        super.onInitialize(addon, config);
    }

    @Override
    public void onTick() {
        if (this.timeNextRequest < System.currentTimeMillis()) {
            this.timeNextRequest = System.currentTimeMillis() + SeriesScaleType.FIFTEEN_MINUTES.value() * 1000;

            this.api.requestSeriesAsync(CurrencyType.BTC, CurrencyType.EUR, series -> {
                this.series = series;

                // Get start of day
                ZonedDateTime currentDate = ZonedDateTime.now();
                long midnight = currentDate.toLocalDate()
                        .atStartOfDay()
                        .atZone(currentDate.getZone())
                        .withEarlierOffsetAtOverlap()
                        .toInstant()
                        .getEpochSecond();

                double priceAtStartOfDay = 0;

                // Calculate min and max price
                double max = Double.MIN_VALUE;
                double min = Double.MAX_VALUE;
                for (ValueAtTime entry : this.series) {
                    max = Math.max(entry.getPrice(), max);
                    min = Math.min(entry.getPrice(), min);

                    if (entry.getTimestamp() < midnight) {
                        priceAtStartOfDay = entry.getPrice();
                    }
                }

                this.priceAtStartOfDay = priceAtStartOfDay;
                this.max = max;
                this.min = min;
            });
        }
    }

    @Override
    public void onRender(IRenderContext context, int width, int height, int mouseX, int mouseY) {
        try {
            this.graphics = (Graphics2D) this.graphicsField.get(context);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Draw background
        context.drawRect(0, 0, width - 1, height - 1, new Color(50, 50, 50, 1));

        // Draw price
        if (this.series.length > 0) {
            // Get current price
            ValueAtTime current = this.series[this.series.length - 1];
            String displayString = (int) current.getPrice() + " " + CurrencyType.EUR.name();
            double displayWidth = context.getStringWidth(displayString, FONT);

            // Create spline
            Path2D.Double spline = new Path2D.Double();
            for (int i = 0; i < this.series.length; i++) {
                ValueAtTime entry = this.series[i];

                double x = (this.width - displayWidth) / (double) this.series.length * i;
                double y = this.height - this.height / (this.max - this.min) * (entry.getPrice() - this.min);

                if (i == 0) {
                    spline.moveTo(x, y);
                } else {
                    spline.lineTo(x, y);
                }
            }

            // Draw series
            this.graphics.setColor(Color.RED);
            this.graphics.draw(spline);

            double prevValue = this.priceAtStartOfDay;
            double value = current.getPrice();
            double difference = value - prevValue;
            double percentage = 100 / prevValue * difference;

            // Format
            percentage = ((int) (percentage * 100)) / 100D;
            String displayPercentage = percentage > 0 ? ("+" + percentage) : String.valueOf(percentage);

            // Draw current price
            context.drawString(displayString, width, 0, 10, true, StringEffect.SHADOW, Color.RED, FONT);
            context.drawString((int) difference + "€ | " + displayPercentage + "%", width, 0, 22, true, StringEffect.SHADOW, Color.RED, FONT);
        }
    }

    @Override
    public void loadTextures(TextureLoader textureLoader) {

    }

    @Override
    protected String getIconPath() {
        return "textures/crypto/icon.png";
    }

    @Override
    public String getDisplayName() {
        return "Crypto";
    }
}
