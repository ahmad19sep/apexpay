package com.apexpay.models;

import java.io.Serializable;

public class Asset implements Serializable {
    public static final String TYPE_CRYPTO = "Crypto";
    public static final String TYPE_STOCK  = "Stock";
    public static final String TYPE_ETF    = "ETF";

    public String symbol;
    public String name;
    public String type;
    public double price;
    public double changePercent;
    public double marketCap;
    public double peRatio;
    public double volume24h;
    public String iconEmoji;

    public Asset(String symbol, String name, String type,
                 double price, double changePercent,
                 double marketCap, double peRatio,
                 double volume24h, String iconEmoji) {
        this.symbol        = symbol;
        this.name          = name;
        this.type          = type;
        this.price         = price;
        this.changePercent = changePercent;
        this.marketCap     = marketCap;
        this.peRatio       = peRatio;
        this.volume24h     = volume24h;
        this.iconEmoji     = iconEmoji;
    }

    public double get52wHigh()  { return price * 1.18; }
    public double get52wLow()   { return price * 0.62; }
}