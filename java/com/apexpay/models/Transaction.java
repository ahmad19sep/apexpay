package com.apexpay.models;

public class Transaction {
    public final String icon;
    public final String title;
    public final String date;
    public final String amount;
    public final boolean isCredit;

    public Transaction(String icon, String title, String date, String amount, boolean isCredit) {
        this.icon = icon;
        this.title = title;
        this.date = date;
        this.amount = amount;
        this.isCredit = isCredit;
    }
}
