package com.apexpay.models;

public class Holding {
    public int    id;
    public String symbol;
    public String name;
    public String assetType;
    public double quantity;
    public double avgBuyPrice;
    public double currentPrice;

    public Holding() {}

    public Holding(String symbol, String name, String assetType,
                   double quantity, double avgBuyPrice, double currentPrice) {
        this.symbol       = symbol;
        this.name         = name;
        this.assetType    = assetType;
        this.quantity     = quantity;
        this.avgBuyPrice  = avgBuyPrice;
        this.currentPrice = currentPrice;
    }

    public double getTotalValue()        { return quantity * currentPrice; }
    public double getProfitLoss()        { return (currentPrice - avgBuyPrice) * quantity; }
    public double getProfitLossPct() {
        if (avgBuyPrice == 0) return 0;
        return ((currentPrice - avgBuyPrice) / avgBuyPrice) * 100.0;
    }
}