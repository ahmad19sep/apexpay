package com.apexpay.models;

public class FrequentContact {
    public String name;
    public String initials;
    public String accountNumber;
    public String avatarColor;

    public FrequentContact(String name, String initials, String accountNumber, String avatarColor) {
        this.name          = name;
        this.initials      = initials;
        this.accountNumber = accountNumber;
        this.avatarColor   = avatarColor;
    }
}