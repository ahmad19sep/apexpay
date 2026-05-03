package com.apexpay.models;

public class Wallet {
    public String holderName;
    public String accountNumber;
    public double balance;
    public String cardExpiry;
    public boolean isFrozen;

    public Wallet(String holderName, String accountNumber, double balance, String cardExpiry) {
        this.holderName    = holderName;
        this.accountNumber = accountNumber;
        this.balance       = balance;
        this.cardExpiry    = cardExpiry;
        this.isFrozen      = false;
    }
}