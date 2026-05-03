package com.apexpay.models;

public class Subscription {
    public String name;
    public String icon;
    public double monthlyAmount;
    public String nextBillingDate;
    public boolean isActive;

    public Subscription(String name, String icon, double monthlyAmount, String nextBillingDate) {
        this.name            = name;
        this.icon            = icon;
        this.monthlyAmount   = monthlyAmount;
        this.nextBillingDate = nextBillingDate;
        this.isActive        = true;
    }
}