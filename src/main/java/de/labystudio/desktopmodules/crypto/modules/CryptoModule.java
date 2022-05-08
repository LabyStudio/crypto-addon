package de.labystudio.desktopmodules.crypto.modules;

import com.google.gson.JsonObject;
import de.labystudio.desktopmodules.core.loader.TextureLoader;
import de.labystudio.desktopmodules.core.module.Module;
import de.labystudio.desktopmodules.core.renderer.IRenderContext;
import de.labystudio.desktopmodules.core.renderer.font.Font;
import de.labystudio.desktopmodules.core.renderer.font.FontStyle;
import de.labystudio.desktopmodules.core.renderer.font.StringAlignment;
import de.labystudio.desktopmodules.core.renderer.font.StringEffect;
import de.labystudio.desktopmodules.core.renderer.swing.SwingRenderContext;
import de.labystudio.desktopmodules.crypto.CryptoAddon;
import de.labystudio.desktopmodules.crypto.api.BlockChainApi;
import de.labystudio.desktopmodules.crypto.api.CurrencyType;
import de.labystudio.desktopmodules.crypto.api.model.ValueAtTime;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.time.ZonedDateTime;

public class CryptoModule extends Module<CryptoAddon> {

    // Rendering
    private static final Font FONT = new Font("Arial", FontStyle.PLAIN, 12);
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##");
    private static final Color COLOR_POSITIVE = new Color(129, 201, 149);
    private static final Color COLOR_NEGATIVE = new Color(242, 139, 130);

    // API
    private final BlockChainApi api = new BlockChainApi();

    // Settings
    private CurrencyType baseCurrency;
    private CurrencyType quoteCurrency;
    private boolean showGraph;
    private int updateInterval;

    // Temp
    private long timeNextRequest = -1L;
    private ValueAtTime[] series = new ValueAtTime[0];
    private Field graphicsField;
    private Graphics2D graphics;
    private double priceAtStartOfDay;
    private long timeMidnightSeconds;

    // Textures
    private BufferedImage textureArrowUp;
    private BufferedImage textureArrowDown;

    public CryptoModule() {
        super(150, 30);

        // Get swing context
        try {
            this.graphicsField = SwingRenderContext.class.getDeclaredField("graphics");
            this.graphicsField.setAccessible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onLoadConfig(JsonObject config) {
        super.onLoadConfig(config);

        // Default currency type
        JsonObject currencyObject;
        if (config.has("currency")) {
            currencyObject = config.getAsJsonObject("currency");
        } else {
            currencyObject = new JsonObject();
            currencyObject.addProperty("base", CurrencyType.BTC.name());
            currencyObject.addProperty("quota", CurrencyType.USD.name());
            config.add("currency", currencyObject);
        }

        // Graph visibility
        if (config.has("show_graph")) {
            this.showGraph = config.get("show_graph").getAsBoolean();
        } else {
            config.addProperty("show_graph", this.showGraph = true);
        }

        // Update interval
        if (config.has("update_interval")) {
            this.updateInterval = config.get("update_interval").getAsInt();
        } else {
            config.addProperty("update_interval", this.updateInterval = 60 * 15); // Default: 15 minutes
        }

        // Load currency type
        try {
            this.baseCurrency = CurrencyType.valueOf(currencyObject.get("base").getAsString());
            this.quoteCurrency = CurrencyType.valueOf(currencyObject.get("quota").getAsString());

            this.timeNextRequest = -1L;
        } catch (Exception e) {
            e.printStackTrace();

            this.baseCurrency = CurrencyType.BTC;
            this.quoteCurrency = CurrencyType.USD;
        }
    }

    @Override
    public void onTick() {
        if (this.baseCurrency != null && this.timeNextRequest < System.currentTimeMillis()) {
            this.timeNextRequest = System.currentTimeMillis() + this.updateInterval * 1000L;

            this.api.requestSeriesAsync(this.baseCurrency, this.quoteCurrency, 24, series -> {
                this.series = series;

                // Get start of day
                ZonedDateTime currentDate = ZonedDateTime.now();
                this.timeMidnightSeconds = currentDate.toLocalDate()
                        .atStartOfDay()
                        .atZone(currentDate.getZone())
                        .withEarlierOffsetAtOverlap()
                        .toInstant()
                        .getEpochSecond();

                // Find price at start of day
                double priceAtStartOfDay = 0;
                for (ValueAtTime entry : this.series) {
                    if (entry.getTimestamp() < this.timeMidnightSeconds) {
                        priceAtStartOfDay = entry.getPrice();
                    }
                }
                this.priceAtStartOfDay = priceAtStartOfDay;
            });
        }
    }

    @Override
    public void onRender(IRenderContext context, int width, int height, int mouseX, int mouseY) {
        if (!(context instanceof SwingRenderContext)) {
            return; // Not supported
        }

        try {
            this.graphics = (Graphics2D) this.graphicsField.get(context);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Draw background
        context.drawRect(0, 0, width - 1, height - 1, new Color(50, 50, 50, 1));

        // Draw price
        if (this.series.length > 0) {
            // Get current price data
            ValueAtTime current = this.series[this.series.length - 1];
            double prevValue = this.priceAtStartOfDay;
            double value = current.getPrice();
            double difference = value - prevValue;
            double percentage = 100 / prevValue * difference;

            // Positive check
            boolean positive = difference >= 0;
            Color color = positive ? COLOR_POSITIVE : COLOR_NEGATIVE;

            // Create price lines
            String line1 = String.format("%s %s", (int) value, this.quoteCurrency.name());
            String line2 = String.format("%s%%", this.toSigned(percentage));

            double space = 5;
            double line1Width = context.getStringWidth(line1, FONT);
            double line2Width = context.getStringWidth(line2, FONT);
            double totalTextWidth = Math.max(line1Width, line2Width) + space;
            double splineWidth = width - totalTextWidth;

            if (this.showGraph) {
                // Draw current price
                context.drawString(line1, splineWidth + space, 10, StringAlignment.LEFT, StringEffect.SHADOW, Color.WHITE, FONT);
                context.drawString(line2, splineWidth + space, (10 + 2) * 2, StringAlignment.LEFT, StringEffect.SHADOW, color, FONT);

                // Draw arrow
                context.drawImage(positive ? this.textureArrowUp : this.textureArrowDown, splineWidth + space * 2 + line2Width, 14, 10, 10);
            } else {
                // Draw current price
                context.drawString(line1, splineWidth + space, 20, StringAlignment.LEFT, StringEffect.SHADOW, color, FONT);
            }

            if (this.showGraph) {
                // Calculate min and max price
                double max = Double.MIN_VALUE;
                double min = Double.MAX_VALUE;
                for (ValueAtTime entry : this.series) {
                    max = Math.max(entry.getPrice(), max);
                    min = Math.min(entry.getPrice(), min);
                }

                // Create spline
                Path2D.Double splineYesterday = new Path2D.Double();
                Path2D.Double splineToday = new Path2D.Double();
                for (int i = 0; i < this.series.length; i++) {
                    ValueAtTime entry = this.series[i];

                    double x = splineWidth / (double) this.series.length * i;
                    double y = height - height / (max - min) * (entry.getPrice() - min);

                    if (entry.getTimestamp() > this.timeMidnightSeconds) {
                        splineToday.lineTo(x, y);
                    } else {
                        if (i == 0) {
                            splineYesterday.moveTo(x, y);
                        } else {
                            splineYesterday.lineTo(x, y);
                        }
                        splineToday.moveTo(x, y);
                    }
                }

                // Draw series of yesterday
                this.graphics.setColor(Color.WHITE);
                this.graphics.draw(splineYesterday);

                // Draw series of today
                this.graphics.setColor(color);
                this.graphics.draw(splineToday);
            }
        }
    }

    @Override
    public void loadTextures(TextureLoader textureLoader) {
        this.textureArrowUp = textureLoader.load("textures/crypto/arrow/arrow_up.png");
        this.textureArrowDown = textureLoader.load("textures/crypto/arrow/arrow_down.png");
    }

    @Override
    protected String getIconPath() {
        return "textures/crypto/icon.png";
    }

    @Override
    public String getDisplayName() {
        return "Crypto";
    }

    private String toSigned(double value) {
        String formatted = DECIMAL_FORMAT.format(value);
        return value >= 0 ? String.format("+%s", formatted) : formatted;
    }
}
